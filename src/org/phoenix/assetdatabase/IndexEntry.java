package org.phoenix.assetdatabase;

import java.io.IOException;
import java.io.RandomAccessFile;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author Vince
 */
public class IndexEntry {

    private final TypeGroupInstance tgi;
    private long fileOffset;
    private long fileSize;
    private boolean changed;

    public IndexEntry(TypeGroupInstance tgi) {
        this.tgi = tgi;
        fileOffset = -1L;
        fileSize = -1L;
        changed = false;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public long getFileSize() {
        return fileSize;
    }

    public TypeGroupInstance getTgi() {
        return tgi;
    }

    public void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isChanged() {
        return changed;
    }

    public IndexEntry setChanged() {
        this.changed = true;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TypeGroupInstance) {
            return tgi.equals(obj);
        }
        if (!(obj instanceof IndexEntry)) {
            return false;
        }
        return tgi.equals(((IndexEntry)obj).tgi);
    }

    @Override
    public int hashCode() {
        return tgi.hashCode();
    }

    @Override
    public IndexEntry clone() {
        IndexEntry ie = new IndexEntry(tgi.clone());
        ie.fileOffset = fileOffset;
        ie.fileSize = fileSize;
        return ie;
    }

    public void save(RandomAccessFile out) throws IOException {
        requireNonNull(out, "DataOutput cannot be null.");
        out.writeInt(tgi.type);
        out.writeInt(tgi.group);
        out.writeLong(tgi.instance);
        out.writeLong(fileOffset);
        out.writeLong(fileSize);
        changed = false;
    }

    public static IndexEntry load(RandomAccessFile in) throws IOException {
        requireNonNull(in, "DataInput cannot be null.");
        int type = in.readInt();
        int group = in.readInt();
        long instance = in.readLong();
        TypeGroupInstance tgi = new TypeGroupInstance(type, group, instance);
        IndexEntry result = new IndexEntry(tgi);
        result.fileOffset = in.readLong();
        result.fileSize = in.readLong();
        result.changed = false;
        return result;
    }

    public static final int SIZEOF = 4 + 4 + 8 + 8 + 8;

}
