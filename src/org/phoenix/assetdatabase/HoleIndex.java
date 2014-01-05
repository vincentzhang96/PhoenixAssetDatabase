package org.phoenix.assetdatabase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static java.util.Objects.requireNonNull;

/**
 * Index of holes. Holes are created when files are deleted, resized, or moved
 * and are filled with junk data. This index tracks their location and size.
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class HoleIndex {

    private final Set<HoleIndexEntry> entries;

    /**
     * Creates a new empty hole index.
     */
    public HoleIndex() {
        entries = new LinkedHashSet<>();
    }

    /**
     * Returns a <b>copy</b> of the hole index entries as an immutable list.
     *
     * @return
     */
    public List<HoleIndexEntry> getEntriesAsListImmutable() {
        return Collections.unmodifiableList(getEntriesAsList());
    }

    /**
     * Returns a <b>copy</b> of the hole index entries as a list.
     *
     * @return
     */
    public List<HoleIndexEntry> getEntriesAsList() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns an immutable <b>copy</b> of the hole index entry set.
     *
     * @return
     */
    public Set<HoleIndexEntry> getEntriesImmutable() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(entries));
    }

    /**
     * Returns the hole index entry set.
     *
     * @return
     */
    public Set<HoleIndexEntry> getEntries() {
        return entries;
    }

    /**
     * Loads the hole index from a RandomAccessFile with the file pointer <b>already
     * set</b> to the proper location.
     *
     * @param raf The RandomAccessFile to read from.
     * @param numEntries Number of entries to read (retrieved from the PAD
     * header).
     * @throws IOException The Hole Index could not be read.
     * @throws NullPointerException The RandomAccessFile is null.
     * @throws IllegalArgumentException The number of entries specified is less
     * than zero.
     */
    public void load(RandomAccessFile raf, int numEntries) throws IOException {
        requireNonNull(raf, "RandomAccessFile cannot be null.");
        if (numEntries < 0) {
            throw new IllegalArgumentException("Number of entries cannot be less than zero.");
        }
        entries.clear();
        for (int count = 0; count < numEntries; count++) {
            HoleIndexEntry e = new HoleIndexEntry();
            e.load(raf);
            entries.add(e);
        }
    }

    /**
     * Saves the hole index to a RandomAccessFile with the file pointer <b>already
     * set</b> to the proper location.
     *
     * @param raf The RandomAccessFile to write to.
     * @throws IOException The Hole Index could not be written.
     * @throws NullPointerException The RandomAccessFile is null.
     */
    public void save(RandomAccessFile raf) throws IOException {
        requireNonNull(raf, "DataOut cannot be null.");
        for (HoleIndexEntry ie : entries) {
            ie.save(raf);
        }
    }

    /**
     * Compacts the hole index by merging adjacent holes. Holes are not allowed
     * to overlap - they will not be detected by this function.
     */
    public void compact() {
        //  If our index has one or none then don't bother.
        if (entries.size() < 2) {
            return;
        }
        List<HoleIndexEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (e, f) -> Long.compareUnsigned(e.getHoleOffset(), f.getHoleOffset()));
        Collections.reverse(sorted);
        Stack<HoleIndexEntry> stack = new Stack<>();
        //  Push the entries onto the stack (reverse order, so earliest entries are first/top).
        for (HoleIndexEntry hie : sorted) {
            stack.push(hie);
        }
        List<HoleIndexEntry> done = new ArrayList<>();
        while (!stack.isEmpty()) {
            //  Pop off the next entry
            HoleIndexEntry top = stack.pop();
            //  No more remaining, we're done
            if (stack.isEmpty()) {
                done.add(top);
                break;
            }
            //  Check the next entry, if it can be merged with the top entry then pop the entry, merge, and push onto the stack.
            HoleIndexEntry next = stack.peek();
            if (top.getHoleOffset() + top.getHoleSize() >= next.getHoleOffset()) {
                stack.pop();
                HoleIndexEntry merged = new HoleIndexEntry(top.getHoleOffset(), top.getHoleSize() + next.getHoleSize());
                stack.push(merged);
            } else {
                done.add(top);
            }
        }
        clear();
        entries.addAll(done);
    }

    /**
     * Returns the size of this hole index, in bytes.
     * @return 
     */
    public int getSizeBytes() {
        return HoleIndexEntry.SIZEOF * entries.size();
    }

    /**
     * Removes all entries from this hole index.
     */
    public void clear() {
        entries.clear();
    }

}
