package org.phoenix.assetdatabase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class HoleIndexEntry {

    /**
     * Location of hole in the file
     */
    private long holeOffset;
    /**
     * Size of hole (in bytes).
     */
    private long holeSize;
    
    public HoleIndexEntry() {
        this(0, 0);
    }
    
    /**
     * Creates a blank hole at offset 0, size 0.
     * @param o
     * @param s
     */
    public HoleIndexEntry(long o, long s) {
        holeOffset = o;
        holeSize = s;
    }

    /**
     * Gets the offset of this hole.
     * @return 
     */
    public long getHoleOffset() {
        return holeOffset;
    }

    /**
     * Gets the size of this hole.
     * @return 
     */
    public long getHoleSize() {
        return holeSize;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (int) (this.holeOffset ^ (this.holeOffset >>> 32));
        hash = 83 * hash + (int) (this.holeSize ^ (this.holeSize >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HoleIndexEntry other = (HoleIndexEntry) obj;
        if (this.holeOffset != other.holeOffset) {
            return false;
        }
        return true;
    }
    
    
    /**
     * Reads this hole index entry from the input.
     * @param in DataInput to read from.
     * @throws IOException The entry could not be read.
     * @throws NullPointerException The DataInput is null.
     */
    public void load(DataInput in) throws IOException {
        requireNonNull(in, "DataInput cannot be null.");
        holeOffset = in.readLong();
        holeSize = in.readLong();
    }
    
    /**
     * Writes this hole index entry to the output.
     * @param out DataOutput to write to.
     * @throws IOException The entry could not be saved.
     * @throws NullPointerException The DataOutput is null.
     */
    public void save(DataOutput out) throws IOException {
        requireNonNull(out, "DataOutput cannot be null.");
        out.writeLong(holeOffset);
        out.writeLong(holeSize);
    }

    @Override
    public String toString() {
        return String.format("offset %s size %s", holeOffset, holeSize);
    }
    
    
    
    /**
     * <code>SIZEOF = 16</code><br/>
     * The size of a hole index entry, in bytes. 
     */
    public static final int SIZEOF = 8 + 8;
}
