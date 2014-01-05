package org.phoenix.assetdatabase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Represents a PAD Index, containing a set of {@link IndexEntry}.
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class Index {

    /**
     * Contains the entries of this Index
     */
    private final Set<IndexEntry> entries;

    /**
     * Creates an empty index.
     */
    public Index() {
        entries = new LinkedHashSet<>();
    }

    /**
     * Returns a <b>copy</b> of the index entries as an immutable list.
     *
     * @return
     */
    public List<IndexEntry> getEntriesAsListImmutable() {
        return Collections.unmodifiableList(getEntriesAsList());
    }

    /**
     * Returns a <b>copy</b> of the index entries as a list.
     *
     * @return
     */
    public List<IndexEntry> getEntriesAsList() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns an immutable <b>copy</b> of the index entry set.
     *
     * @return
     */
    public Set<IndexEntry> getEntriesImmutable() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(entries));
    }

    /**
     * Returns the index entry set.
     *
     * @return
     */
    public Set<IndexEntry> getEntries() {
        return entries;
    }

    /**
     * Gets the entry with the specified TGI, or null if the index does not
     * contain an entry with the specified TGI.
     *
     * @param tgi The TGI to retrieve.
     * @return An IndexEntry matching the requested TGI or null if none were
     * found.
     */
    public IndexEntry getEntry(TypeGroupInstance tgi) {
        requireNonNull(tgi, "TGI cannot be null.");
        for (IndexEntry ie : entries) {
            if (ie.getTgi().equals(tgi)) {
                return ie;
            }
        }
        return null;
    }

    /**
     * Loads the index from a RandomAccessFile with the file pointer <b>already
     * set</b> to the proper location.
     *
     * @param raf The RandomAccessFile to read from.
     * @param numEntries Number of entries to read (retrieved from the PAD
     * header).
     * @throws IOException The Index could not be read.
     * @throws NullPointerException The RandomAccessFile is null.
     * @throws IllegalArgumentException The number of entries specified is less
     * than zero.
     */
    public void load(RandomAccessFile raf, int numEntries) throws IOException {
        requireNonNull(raf, "DataInput cannot be null.");
        if (numEntries < 0) {
            throw new IllegalArgumentException("Number of entries must be greater than zero.");
        }
        entries.clear();
        for (int count = 0; count < numEntries; count++) {
            IndexEntry e = IndexEntry.load(raf);
            entries.add(e);
        }
    }

    /**
     * Saves the index to a RandomAccessFile with the file pointer <b>already
     * set</b> to the proper location.
     *
     * @param raf The RandomAccessFile to write to.
     * @throws IOException The Index could not be written.
     * @throws NullPointerException The RandomAccessFile is null.
     */
    public void save(RandomAccessFile raf) throws IOException {
        requireNonNull(raf, "DataOut cannot be null.");
        for (IndexEntry ie : entries) {
            ie.save(raf);
        }
    }

    /**
     * Returns the size of this index, in bytes.
     * @return 
     */
    public int getSizeBytes() {
        return IndexEntry.SIZEOF * entries.size();
    }

    @Override
    public Index clone() {
        Index i = new Index();
        for (IndexEntry ie : entries) {
            i.entries.add(ie.clone());
        }
        return i;
    }

    /**
     * Removes all entries from the index.
     */
    public void clear() {
        entries.clear();
    }
}
