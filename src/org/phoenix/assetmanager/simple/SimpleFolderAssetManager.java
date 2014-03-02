package org.phoenix.assetmanager.simple;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.phoenix.assetdatabase.AssetDatabase;
import org.phoenix.assetdatabase.AssetDatabaseImpl;
import org.phoenix.assetdatabase.Index;
import org.phoenix.assetdatabase.IndexEntry;
import org.phoenix.assetdatabase.Subfile;
import org.phoenix.assetdatabase.TypeGroupInstance;
import org.phoenix.assetmanager.PAssetManager;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author Vince
 */
public class SimpleFolderAssetManager implements PAssetManager {

    public final Path rootFolder;

    private Set<String> acceptableFileExts;
    private Map<TypeGroupInstance, SimpleManagerEntry> entries;
    private Function<Path, AssetDatabase> databaseSupplier;

    public SimpleFolderAssetManager(Path rootFolder, Function<Path, AssetDatabase> supplier) throws FileNotFoundException {
        requireNonNull(rootFolder, "RootFolder cannot be null.");
        requireNonNull(supplier, "Supplier cannot be null.");
        rootFolder = rootFolder.toAbsolutePath();
        if (Files.notExists(rootFolder)) {
            throw new FileNotFoundException("Folder at " + rootFolder.toString() + " does not exist.");
        }
        if (!Files.isDirectory(rootFolder)) {
            throw new IllegalArgumentException(rootFolder.toString() + " is not a directory.");
        }
        this.rootFolder = rootFolder;
        databaseSupplier = supplier;
        acceptableFileExts = new HashSet<>();
        entries = new HashMap<>();
    }

    public Set<String> getAcceptableFileExts() {
        return acceptableFileExts;
    }

    @Override
    public void indexAssets() {
        indexAssets((f) -> {
            //  Do nothing
        });
    }

    @Override
    public void indexAssets(DoubleConsumer progressUpdateHandler) {
        requireNonNull(progressUpdateHandler, "ProgressUpdateHandler cannot be null. Pass an empty lambda instead.");
        progressUpdateHandler.accept(STATUS_SCANNING);
        //  Walk the tree to get a list of candidate files
        List<Path> candidates = new ArrayList<>();
        try {
            listCandidates(rootFolder, candidates);
        } catch (UncheckedIOException ex) {
            progressUpdateHandler.accept(STATUS_FAILED);
            return;
        }
        progressUpdateHandler.accept(0);
        try {
            final int count = candidates.size();
            for (int i = 0; i < count; i++) {
                Path p = candidates.get(i);
                AssetDatabase ad = new AssetDatabaseImpl(p);
                ad.load();
                Index index = ad.getIndex();
                Set<IndexEntry> e = index.getEntriesImmutable();
                e.stream().forEach((ie) -> entries.put(ie.getTgi(), new SimpleManagerEntry(ie.getTgi(), p)));
                double progress = (double) i / (double) count;
                progressUpdateHandler.accept(progress);
            }
        } catch (IOException ex) {
            progressUpdateHandler.accept(STATUS_FAILED);
            return;
        }
    }

    private void listCandidates(Path dir, List<Path> candidates) throws UncheckedIOException {
        Stream<Path> dirs = null;
        try {
            //  Depth first
            dirs = Files.list(dir);
            dirs.filter(Files::isDirectory).
                    sorted((p1, p2) -> p1.toString().compareToIgnoreCase(p2.toString())).
                    forEachOrdered((d) -> listCandidates(d, candidates));
            dirs.close();
            //  Then files in the directory
            dirs = Files.list(dir);
            dirs.filter((p) -> !Files.isDirectory(p)).
                    filter((Path p) -> {
                        Path f = p.getFileName();
                        String[] ss = f.toString().split("\\.");
                        if (ss.length <= 1) {
                            return false;
                        }
                        return acceptableFileExts.contains(ss[ss.length - 1]);
                    }).
                    sorted((p1, p2) -> p1.toString().compareToIgnoreCase(p2.toString())).
                    forEachOrdered(candidates::add);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } finally {
            if (dirs != null) {
                dirs.close();
            }
        }

    }

    @Override
    public Subfile getSubfile(TypeGroupInstance tgi) throws FileNotFoundException, IOException {
        requireNonNull(tgi, "TGI cannot be null.");

        SimpleManagerEntry sme = entries.get(tgi);
        if (sme == null) {
            throw new FileNotFoundException("TGI " + tgi.toString() + " not found.");
        }
        AssetDatabase db = databaseSupplier.apply(sme.padLocation);
        db.load();
        return db.loadSubfile(tgi);
    }

    @Override
    public Map<TypeGroupInstance, Subfile> getSubfiles(Collection<TypeGroupInstance> tgis) throws FileNotFoundException, IOException {
        requireNonNull(tgis, "TGI cannot be null.");
        Set<TypeGroupInstance> set;
        if (tgis instanceof Set) {
            set = (Set) tgis;
        } else {
            set = new HashSet(tgis);
        }
        //  Collect TGIs that have the same Path
        Map<Path,List<TypeGroupInstance>> bins = new HashMap<>();
        for(TypeGroupInstance tgi : tgis) {
            SimpleManagerEntry sme = entries.get(tgi);
            if(sme == null) {
                throw new FileNotFoundException("TGI " + tgi.toString() + " not found.");
            }
            List<TypeGroupInstance> l = bins.get(sme.padLocation);
            if(l == null) {
                l = new ArrayList<>();
                bins.put(sme.padLocation, l);
            }
            l.add(tgi);
        }
        Map<TypeGroupInstance, Subfile> ret = new HashMap<>();
        //  Go through each and load
        for(Entry<Path, List<TypeGroupInstance>> e : bins.entrySet()) {
            Path p = e.getKey();
            List<TypeGroupInstance> l = e.getValue();
            AssetDatabase db = databaseSupplier.apply(p);
            db.load();
            ret.putAll(db.loadSubfiles(l));
        }
        return ret;
    }

    @Override
    public boolean contains(TypeGroupInstance tgi) {
        requireNonNull(tgi, "TGI cannot be null.");
        return entries.containsKey(tgi);
    }

    @Override
    public boolean containsAll(Collection<TypeGroupInstance> tgis) {
        requireNonNull(tgis, "TGI collection cannot be null.");
        Set<TypeGroupInstance> set;
        if (tgis instanceof Set) {
            set = (Set) tgis;
        } else {
            set = new HashSet(tgis);
        }
        //  anyMatch returns true if any element in the stream matches the predicate.
        //  In this case, the predicate returns true if the TGI is NOT contained in the index.
        return !set.stream().
                anyMatch((tgi) -> !entries.containsKey(tgi));
    }

    @Override
    public Collection<TypeGroupInstance> containsAny(Collection<TypeGroupInstance> tgis) {
        requireNonNull(tgis, "TGI collection cannot be null.");
        Set<TypeGroupInstance> set;
        if (tgis instanceof Set) {
            set = (Set) tgis;
        } else {
            set = new HashSet(tgis);
        }
        return set.stream().
                filter(entries::containsKey).
                collect(Collectors.toSet());
    }

    @Override
    public void clearIndex() {
        entries.clear();
    }

    @Override
    public void clearCache() {
        //  Do nothing
    }

}
