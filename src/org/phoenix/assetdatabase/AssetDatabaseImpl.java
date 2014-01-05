package org.phoenix.assetdatabase;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the Phoenix Asset Database, supporting reading and writing version 3 PADs.
 * 
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class AssetDatabaseImpl implements AssetDatabase {

    public static final int HEADER_SIZE = 42;
    public static final int VERSION_3 = 3;

    private final Path location;
    /**
     * Index that exists on disk (does not change except during load() or
     * save()).
     */
    private final Index index;
    /**
     * Working index/index in memory (changes as modifications are made).
     */
    private Index modIndex;
    //  New or changed data
    private Map<TypeGroupInstance, Subfile> modifiedSubfiles;
    private final HoleIndex holeIndex;
    private final MetadataList metadata;

    private int version;

    public AssetDatabaseImpl(Path loc) {
        location = requireNonNull(loc, "Location cannot be null.");
        index = new Index();
        modIndex = new Index();
        holeIndex = new HoleIndex();
        metadata = new MetadataList();
        modifiedSubfiles = new HashMap<>();
        version = 3;
    }

    @Override
    public void load() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(location.toFile(), "r")) {
            //  Magic Number
            raf.seek(0);
            int magic = raf.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IOException(String.format("Invalid magic number for file: Expected 0x%08X, got 0x%08X", MAGIC_NUMBER, magic));
            }
            version = raf.readInt();
            debug("Version is %s.", version);
            switch (version) {
                case VERSION_3:
                    loadV3(raf);
                    break;
                default:
                    throw new UnsupportedOperationException("Cannot process PAD version " + version);
            }
        } catch (EOFException eof) {
            debug("Empty/nonexistant file.");
            version = getLatestVersion();
        }
    }

    private void loadV3(RandomAccessFile raf) throws IOException {
        long indexOffset = raf.readLong();
        int numIndexEntries = raf.readInt();
        long holeIndexOffset = raf.readLong();
        int numHoleEntries = raf.readInt();
        long metadataOffset = raf.readLong();
        int numMetadataEntries = raf.readUnsignedShort();
        debug("Index located at 0x%08X with %s entries.", indexOffset, numIndexEntries);
        debug("HoleIndex located at 0x%08X with %s entries.", holeIndexOffset, numHoleEntries);
        debug("MetadataTable located at 0x%08X with %s entries.", metadataOffset, numMetadataEntries);

        if (numIndexEntries > 0 && indexOffset >= HEADER_SIZE) {
            raf.seek(indexOffset);
            index.load(raf, numIndexEntries);
            syncModIndex();
        }
        if (numHoleEntries > 0 && holeIndexOffset >= HEADER_SIZE) {
            raf.seek(holeIndexOffset);
            holeIndex.load(raf, numHoleEntries);
        }
        if (numMetadataEntries > 0 && metadataOffset >= HEADER_SIZE) {
            raf.seek(metadataOffset);
            metadata.load(raf, numMetadataEntries);
        }
    }

    private void syncModIndex() {
        if (modIndex != null) {
            modIndex.clear();
        }
        modIndex = index.clone();
        modifiedSubfiles.clear();
    }

    private void syncIndex() {
        index.clear();
        index.getEntries().addAll(modIndex.getEntries());
        modifiedSubfiles.clear();
    }

    @Override
    public void save() throws IOException {
        switch (version) {
            case 3:
                saveV3(version);
                break;
            default:
                //  Update to latest
                saveV3(version);
                break;
        }
    }

    private void saveV3(int previousVersion) throws IOException {
        if (previousVersion != 3) {
            throw new UnsupportedOperationException("Updating from version " + previousVersion + " to version 3 is not supported.");
        }
        //  For now, do a naive save that simply loads up the data from disk, applies the changes, and saves it back to disk.
        try (RandomAccessFile raf = new RandomAccessFile(location.toFile(), "rw")) {
            HashMap<TypeGroupInstance, Subfile> subfiles = new HashMap<>();
            if (Files.exists(location)) {
                try {
                    AssetDatabaseImpl adi = new AssetDatabaseImpl(location);
                    adi.load();
                    List<TypeGroupInstance> tgis = new ArrayList();
                    adi.getIndex().getEntries().stream().
                            //  Only load files that still remain
                            filter(modIndex.getEntries()::contains).
                            forEach((e) -> tgis.add(e.getTgi()));
                    subfiles.putAll(adi.loadSubfiles(tgis));
                    //  The Asset Database is automatically released
                } catch (IOException e) {
                    debug("No previous file or invalid file:\n%s", e.getLocalizedMessage());
                }
            }
            //  Patch in the changed subfiles
            subfiles.putAll(modifiedSubfiles);

            //  Write the file
            raf.seek(0);
            raf.setLength(0);
            //  Using hardcoded offsets for now
            int indexOffset = HEADER_SIZE;
            int holeIndexOffset = indexOffset + modIndex.getSizeBytes();
            int metadataOffset = holeIndexOffset + holeIndex.getSizeBytes();

            //  Header
            raf.writeInt(MAGIC_NUMBER);                         //  Magic               4   0x0000
            raf.writeInt(VERSION_3);                            //  Version             4   0x0004
            raf.writeLong(indexOffset);                         //  Index offset        8   0x0008
            raf.writeInt(modIndex.getEntries().size());         //  Index entries       4   0x0010
            raf.writeLong(holeIndexOffset);                     //  Hole index offset   8   0x0014
            raf.writeInt(holeIndex.getEntries().size());        //  Hole index entries  4   0x001C
            raf.writeLong(metadataOffset);                      //  Metadata offset     8   0x0020
            raf.writeShort(metadata.getTagsImmutable().size()); //  Metadata entries    2   0x0028
            //  Index and hole index will be saved on second pass
            debug("End of header at 0x%08X", raf.getFilePointer()); //                      0x002A
            raf.seek(raf.getFilePointer() + modIndex.getSizeBytes() + holeIndex.getSizeBytes() + metadata.getSizeBytes());
            debug("Skipped to position 0x%08X", raf.getFilePointer());
            //  Write file data and track offsets
            debug("Writing file data starting at pos 0x%08X", raf.getFilePointer());
            for (Entry<TypeGroupInstance, Subfile> e : subfiles.entrySet()) {
                doWriteFileV3(e, raf);
            }

            //  Save the index
            raf.seek(indexOffset);
            debug("Seeked to pos 0x%08X for index writing.", raf.getFilePointer());
            modIndex.save(raf);
            debug("Finished writing index, ended at pos 0x%08X", raf.getFilePointer());
            //  Save hole index
            raf.seek(holeIndexOffset);
            debug("Seeked to pos 0x%08X for hole index writing.", raf.getFilePointer());
            holeIndex.save(raf);
            debug("Finished writing hole index, ended at pos 0x%08X", raf.getFilePointer());
            //  Save metadata
            debug("Seeked to pos 0x%08X for metadata writing.", raf.getFilePointer());
            metadata.save(raf);
            debug("Finished writing metadata, ended at pos 0x%08X", raf.getFilePointer());
            raf.close();

            //  Synchronize the index so that the main index now refers to our new index (modIndex).
            syncIndex();
        }
    }

    private void doWriteFileV3(Map.Entry<TypeGroupInstance, Subfile> e, RandomAccessFile raf) throws IOException {
        IndexEntry ie = modIndex.getEntry(e.getKey());
        SaveInformation si = e.getValue().save(raf);
        ie.setFileOffset(si.diskOffset);
        ie.setFileSize(si.diskSize);
        debug("Wrote %s at 0x%08X", e.getKey().toString(), si.diskOffset);
    }

    @Override
    public boolean contains(TypeGroupInstance tgi) {
        return getIndex().getEntries().contains(new IndexEntry(tgi));
    }

    @Override
    public Subfile loadSubfile(TypeGroupInstance tgi) throws FileNotFoundException, IOException {
        IndexEntry ie = index.getEntry(tgi);
        if (ie == null) {
            throw new FileNotFoundException("TGI " + tgi.toString() + " not in database.");
        }
        try (RandomAccessFile raf = new RandomAccessFile(location.toFile(), "r")) {
            return loadSubfileImpl(raf, ie);
        }
    }

    @Override
    public Map<TypeGroupInstance, Subfile> loadSubfiles(Collection<TypeGroupInstance> tgis) throws FileNotFoundException, IOException {
        if (tgis.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<TypeGroupInstance, Subfile> result = new HashMap<>(tgis.size());
        try (RandomAccessFile raf = new RandomAccessFile(location.toFile(), "r")) {
            for (TypeGroupInstance tgi : tgis) {
                IndexEntry ie = index.getEntry(tgi);
                if (ie == null) {
                    throw new FileNotFoundException("TGI " + tgi.toString() + " not in database.");
                }
                result.put(tgi, loadSubfileImpl(raf, ie));
            }
        }
        return result;
    }

    private Subfile loadSubfileImpl(RandomAccessFile raf, IndexEntry ie) throws IOException {
        raf.seek(ie.getFileOffset());
        Subfile sf = new Subfile();
        sf.load(raf);
        return sf;
    }

    @Override
    public void putSubfile(IndexEntry ie, Subfile sf) {
        requireNonNull(ie, "IndexEntry cannot be null.");

        ie.setFileSize(sf.getCompressedSize());
        modIndex.getEntries().add(ie.setChanged());
        modifiedSubfiles.put(ie.getTgi(), sf);
    }

    @Override
    public void putSubfiles(Map<IndexEntry, Subfile> files) {
        requireNonNull(files, "Subfile map cannot be null.");
        if (files.isEmpty()) {
            return;
        }
        files.entrySet().stream().forEach((e) -> putSubfile(e.getKey(), e.getValue()));
    }

    @Override
    public void removeSubfile(TypeGroupInstance tgi) {
        modIndex.getEntries().removeIf((ie) -> ie.getTgi().equals(tgi));
    }

    private static void debug(String s, Object... args) {
        if (args == null || args.length == 0) {
            System.out.println(s);
        } else {
            System.out.println(String.format(s, args));
        }
    }

    /**
     * Gets the index of this database. The index returned is a view of the
     * database <i>on disk.</i> Changes made to it are not reflected through the
     * index until the database is saved.
     *
     * @return
     */
    @Override
    public Index getIndex() {
        return index;
    }

    @Override
    public MetadataList getMetadata() {
        return metadata;
    }

    @Override
    public void clear() {
        modIndex.clear();
        modifiedSubfiles.clear();
    }

    public static int getLatestVersion() {
        return VERSION_3;
    }

}
