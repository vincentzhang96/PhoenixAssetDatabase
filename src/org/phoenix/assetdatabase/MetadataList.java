package org.phoenix.assetdatabase;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

/**
 * Represents a block of metadata entries.
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class MetadataList {

    private final Map<String, String> tags;

    public MetadataList() {
        tags = new HashMap<>();
    }

    /**
     * Creates a MetadataList with the specified tags.
     *
     * @param tags
     */
    public MetadataList(Map<String, String> tags) {
        this();
        requireNonNull(tags, "Map cannot be null.");
        putAll(tags);
    }

    /**
     * Gets an immutable view of the metadata tags. To put values, use
     * {@link MetadataList#put(String, String)}.
     *
     * @return
     */
    public final Map<String, String> getTagsImmutable() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Adds a key-value pair to the underlying tag Map. This function performs
     * necessary housekeeping and input-sanitation.
     * <p>
     * Please note that metadata list entries use UTF-8, so the limit is 255
     * <b>bytes</b>, <i>not characters</i>. Multibyte characters will obviously
     * contribute more towards the limit.
     *
     * @param key The key for this pair. Cannot be null, empty, or effectively
     * empty (<code>key.trim().isEmpty()</code> must not be true). Will be
     * truncated to 255 bytes if longer.
     * @param value The value for this pair. Can be any value (<b>null</b> will
     * be encoded as an empty String). Will be truncated to 255 bytes if longer.
     * @return The previous value associated with key, or null if there was no
     * mapping for key. (A null return can also indicate that the map previously
     * associated null with key, if the implementation supports null values.)
     */
    public final String put(String key, String value) {
        requireNonNull(key, "Key cannot be null.");
        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty or effectively empty.");
        }
        if (value == null) {
            value = "";
        }
        //  Truncate Strings to 255 chars if need be
        //  TODO replace with proper UTF-8 byte counting. This WILL allow Strings more than 255 bytes long through!
        if (key.length() > 255) {
            key = key.substring(0, 255);
        }
        if (value.length() > 255) {
            value = value.substring(0, 255);
        }
        return tags.put(key, value);
    }

    /**
     * @see Map#putAll(java.util.Map)
     * @param map
     */
    public final void putAll(Map<String, String> map) {
        requireNonNull(map, "Map cannot be null.");
        map.entrySet().parallelStream().forEach((entry) -> {
            put(entry.getKey(), entry.getValue());
        });
    }

    public int getSizeBytes() {
        int count = 0;
        if (tags.isEmpty()) {
            return 0;
        }
        for (Entry<String, String> e : tags.entrySet()) {
            try {
                //  Dummy output
                DataOutput dOut = new DataOutputStream(new ByteArrayOutputStream());
                count += 1;     //  Key length
                count += 1;     //  Value length
                count += writeModUTF8String(dOut, e.getKey());
                count += writeModUTF8String(dOut, e.getValue());
            } catch (IOException ex) {
                //  Should never happen when writing to a BAOS.
                throw new RuntimeException("Impossible exception.", ex);
            }
        }
        return count;
    }

    /**
     * Loads the MetadataList from the DataInput, reading
     * <code>numberOfEntries</code> entries. The DataInput <b>must</b> be set to
     * the start of the metadata block.
     *
     * @param in
     * @param numberOfEntries
     * @throws IOException
     */
    public void load(DataInput in, int numberOfEntries) throws IOException {
        Objects.requireNonNull(in, "ObjectInput cannot be null.");
        tags.clear();
        if (numberOfEntries <= 0) {
            return;
        }
        /*
         FORMAT
         BYTES   CONTENT
         REPEAT numberOfEntries times {
         1       Length of key string    
         1       Length of value string
         n       Key string bytes
         n       Value string bytes
         } END REPEAT
         */
        for (int count = 0; count < numberOfEntries; count++) {
            int lenKey = in.readUnsignedByte();
            int lenValue = in.readUnsignedByte();
            String key = readModUTF8String(in, lenKey);
            String value = readModUTF8String(in, lenValue);
            tags.put(key, value);
        }
    }

    /**
     * Saves the MetadataList to the DataOutput. The DataOutput <b>must</b> be
     * set to the start of the metadata block.
     *
     * @param out
     * @return The number of bytes written.
     * @throws IOException
     */
    public int save(DataOutput out) throws IOException {
        Objects.requireNonNull(out, "ObjectOutput cannot be null.");
        /*
         FORMAT
         BYTES   CONTENT
         REPEAT numberOfEntries times {
         1       Length of key string    
         1       Length of value string
         n       Key string bytes
         n       Value string bytes
         } END REPEAT
         */
        int written = 0;
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            out.write(key.length());
            out.write(value.length());
            written += 2;
            written += writeModUTF8String(out, key);
            written += writeModUTF8String(out, value);
        }
        return written;
    }

    @Override
    public String toString() {
        return tags.toString();
    }

    private static String readModUTF8String(DataInput bis, int len) throws IOException {
        StringBuilder sb = new StringBuilder();
        int pos = -1;
        while (true) {
            if (pos >= len - 1) {
                return sb.toString();
            }
            final int a = bis.readUnsignedByte();
            pos++;
            if (a == -1) {
                return sb.toString();
            }
            //  If the first byte of a group matches the bit pattern 0xxxxxxx (where x means "may be 0 or 1"), then the group consists of just that byte. The byte is zero-extended to form a character.
            if ((a & 0b1000_0000) == 0b0000_0000) {
                sb.append((char) a);
                continue;
            }
            //  If the first byte of a group matches the bit pattern 110xxxxx, then the group consists of that byte a and a second byte b.
            if ((a & 0b1110_0000) == 0b1100_0000) {
                //  If there is no byte b (because byte a was the last of the bytes to be read),
                final int b = bis.readUnsignedByte();
                pos++;
                //  or if byte b does not match the bit pattern 10xxxxxx, then a UTFDataFormatException is thrown.
                if ((b & 0b1100_0000) != 0b1000_0000) {
                    throw new UTFDataFormatException(String.format("Invalid second byte at pos %s: got binary pattern %08X. So far: \"%s\"", pos, b, sb.toString()));
                }
                //  Otherwise, the group is converted to the character:
                sb.append((char) (((a & 0x1F) << 6) | (b & 0x3F)));
                continue;
            }
            //  If the first byte of a group matches the bit pattern 1110xxxx, then the group consists of that byte a and two more bytes b and c.
            if ((a & 0b1111_0000) == 0b1110_0000) {
                //  If there is no byte c or b (because byte a was one of the last two of the bytes to be read),
                final int b = bis.readUnsignedByte();
                pos++;
                final int c = bis.readUnsignedByte();
                pos++;
                //  or either byte b or byte c does not match the bit pattern 10xxxxxx, then a UTFDataFormatException is thrown.
                if ((b & 0b1100_0000) != 0b1000_0000) {
                    throw new UTFDataFormatException("Invalid second byte at pos " + (pos - 1));
                }
                if ((c & 0b1100_0000) != 0b1000_0000) {
                    throw new UTFDataFormatException("Invalid third byte at pos " + pos);
                }
                //  Otherwise, the group is converted to the character:
                sb.append((char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F)));
                continue;
            }
            //  If the first byte of a group matches the pattern 1111xxxx or the pattern 10xxxxxx, then a UTFDataFormatException is thrown.
            if ((a & 0b1111_0000) == 0b1111_0000 || (a & 0b1100_0000) == 0b1000_0000) {
                throw new UTFDataFormatException("Invalid byte at pos " + pos);
            }
            //  Should have handled everything
            assert false;
        }
    }

    private static int writeModUTF8String(DataOutput out, String s) throws IOException {
        Queue<Character> chars = new LinkedList<>();
        char[] charArray = s.toCharArray();
        for (int c = 0; c < charArray.length; c++) {
            chars.add(charArray[c]);
        }
        int written = 0;
        while (chars.peek() != null) {
            char c = chars.poll();
            //  If a character c is in the range \u0001 through \u007f, it is represented by one byte:
            if (c >= '\u0001' && c <= '\u007F') {
                out.write(c);
                written++;
                continue;
            }
            //  If a character c is \u0000 or is in the range \u0080 through \u07ff, then it is represented by two bytes, to be written in the order shown:
            if (c == '\u0000' || (c >= '\u0080' && c <= '\u07FF')) {
                out.write((byte) (0xc0 | (0x1f & (c >> 6))));
                out.write((byte) (0x80 | (0x3f & c)));
                written += 2;
                continue;
            }
            //  If a character c is in the range \u0800 through uffff, then it is represented by three bytes, to be written in the order shown:
            if (c >= '\u0800' && c <= '\uFFFF') {
                out.write((byte) (0xe0 | (0x0f & (c >> 12))));
                out.write((byte) (0x80 | (0x3f & (c >> 6))));
                out.write((byte) (0x80 | (0x3f & c)));
                written += 3;
                continue;
            }
            //  Should have handled everything
            assert false;
        }
        return written;
    }

    public void clear() {
        tags.clear();
    }
}
