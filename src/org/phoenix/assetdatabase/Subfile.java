package org.phoenix.assetdatabase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import static java.security.MessageDigest.getInstance;
import static java.util.Objects.requireNonNull;

/**
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class Subfile {

    public static final MessageDigest md5;

    static {
        try {
            md5 = getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * The type of compression used.
     * <ul>
     * <li> 0 - No compression.</li>
     * </ul>
     */
    private int compressionType;
    /**
     * The size of this subfile, in bytes, on disk (compressed).
     */
    private int compressedSize;
    /**
     * The size of this subfile, in bytes, in memory (decompressed).
     */
    private int decompressedSize;
    /**
     * The MD5 hash of the decompressed file data.
     */
    private byte[] md5Hash;
    /**
     * The metadata list associated with this subfile.
     */
    private MetadataList metadata;
    /**
     * Data to/from disk
     */
    private Optional<byte[]> dataOnDisk;
    /**
     * Data in memory
     */
    private Optional<byte[]> dataInMemory;

    /**
     * Creates a new Subfile with an empty metadata list, no compression, and no data on disk or in memory.
     */
    public Subfile() {
        this(0);
    }
    
    /**
     * Creates a new subfile with an empty metadata list, no data on disk or in memory, and the specified compression.
     * @param compressionID 
     */
    public Subfile(int compressionID) {
        metadata = new MetadataList();
        dataOnDisk = Optional.empty();
        dataInMemory = Optional.empty();
        compressionType = compressionID;
    }

    /**
     * Returns the compression type ID that was used to compress this file.
     * @return 
     */
    public int getCompressionType() {
        return compressionType;
    }

    /**
     * Returns the size of the file on disk (compressed).
     * @return 
     */
    public long getCompressedSize() {
        return compressedSize;
    }

    /**
     * Returns the size of the file when decompressed.
     * @return 
     */
    public long getDecompressedSize() {
        return decompressedSize;
    }

    /**
     * Returns the metadataList for this subfile.
     * @return 
     */
    public MetadataList getMetadata() {
        return metadata;
    }

    public byte[] getData() throws IOException {
        if (!dataOnDisk.isPresent()) {
              throw new DataNotPresentException("File data must be loaded first!");
        }
        //  Decompress and process
        if (!dataInMemory.isPresent()) {
            decompressAndSet();
            //  MD5 check
            if (!isEmptyArray(md5Hash)) {
                byte[] digest = calculateMD5(dataInMemory.get());
                if (!Arrays.equals(md5Hash, digest)) {
                    throw new DataCorruptedException("MD5 hashes do not match. File possibly corrupt?");
                }
            }
        }
        return dataInMemory.get();
    }

    /**
     * Sets this subfile's raw (decompressed) data, optionally calculates the MD5 hash, and compresses the data.
     * @param newData
     * @param calculateMD5 
     */
    public void setData(byte[] newData, boolean calculateMD5) {
        this.dataInMemory = Optional.of(newData);
        //  Calculate MD5
        if (calculateMD5) {
            md5Hash = calculateMD5(dataInMemory.get());
        } else {
            md5Hash = new byte[16];
            Arrays.fill(md5Hash, (byte)0);
        }
        //  Compress
        compressAndSet();
    }

    private void decompressAndSet() {
        if (!dataOnDisk.isPresent()) {
            throw new IllegalStateException("RawData must first been read before decompressing!");
        }
        if (compressionType == 0) {
            dataInMemory = Optional.of(new byte[dataOnDisk.get().length]);
            System.arraycopy(dataOnDisk.get(), 0, dataInMemory.get(), 0, dataOnDisk.get().length);
            return;
        }
        //  Do decompressing
        //  TODO Impl
    }

    private void compressAndSet() {
        if (!dataInMemory.isPresent()) {
            throw new IllegalStateException("Data must first been set before compressing!");
        }
        if (compressionType == 0) {
            dataOnDisk = Optional.of(new byte[dataInMemory.get().length]);
            System.arraycopy(dataInMemory.get(), 0, dataOnDisk.get(), 0, dataInMemory.get().length);
            compressedSize = dataOnDisk.get().length;
            decompressedSize = dataInMemory.get().length;
            return;
        }
        //  TODO Impl
    }
    
    /**
     * Loads this subfile from a database using the given RandomAccessFile. The file pointer should be set beforehand.
     * @param in
     * @throws IOException 
     */
    public void load(RandomAccessFile in) throws IOException {
        requireNonNull(in, "RandomAccessFile cannot be null.");
        md5Hash = new byte[16];
        dataInMemory = Optional.empty();
        compressionType = in.readUnsignedShort();
        compressedSize = in.readInt();
        decompressedSize = in.readInt();
        in.readFully(md5Hash);
        int numMetadata = in.readUnsignedShort();
        dataOnDisk = Optional.of(new byte[compressedSize]);
        in.readFully(dataOnDisk.get());
        metadata = new MetadataList();
        metadata.load(in, numMetadata);
    }

    /**
     * Saves this subfile to the RandomAccessFile. The file pointer should be set beforehand.
     * @param out
     * @return
     * @throws IOException 
     */
    public SaveInformation save(RandomAccessFile out) throws IOException {
        requireNonNull(out, "RandomAccessFile cannot be null.");
        if(md5Hash == null) {
            md5Hash = new byte[16];
            Arrays.fill(md5Hash, (byte)0);
        }
        long ptr = out.getFilePointer();
        out.writeShort(compressionType);
        out.writeInt(compressedSize);
        out.writeInt(decompressedSize);
        out.write(md5Hash);
        out.writeShort(metadata.getTagsImmutable().size());
        out.write(dataOnDisk.get());
        metadata.save(out);
        return new SaveInformation(ptr, compressedSize);
    }
    
    /**
     * Calculates the MD5 hash of a byte array.
     * @param data
     * @return 
     */
    public static byte[] calculateMD5(byte[] data) {
        md5.reset();
        return md5.digest(data);
    }

    /**
     * Checks if a given array is empty (all zeros).
     * @param data
     * @return 
     */
    public static boolean isEmptyArray(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}

