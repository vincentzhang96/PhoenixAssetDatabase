package org.phoenix.assetdatabase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author Vince
 */
public class HoleIndexEntry {

    private long holeOffset;
    private long holeSize;
    
    public HoleIndexEntry() {
        
    }

    public long getHoleOffset() {
        return holeOffset;
    }

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
    
    
    
    public void load(DataInput in) throws IOException {
        requireNonNull(in, "DataInput cannot be null.");
        holeOffset = in.readLong();
        holeSize = in.readLong();
    }
    
    public void save(DataOutput out) throws IOException {
        requireNonNull(out, "DataOutput cannot be null.");
        out.writeLong(holeOffset);
        out.writeLong(holeSize);
    }
    
    public static final int SIZEOF = 8 + 8;
}
