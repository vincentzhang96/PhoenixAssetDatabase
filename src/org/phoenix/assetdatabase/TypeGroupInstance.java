package org.phoenix.assetdatabase;

/**
 * Represents a Type-Group-Instance triplet used to identify files in a PAD.
 * This class is immutable.
 * <p>
 * The values that are represented should be treated using <b>unsigned</b>
 * methods (e.g. {@link Integer#compareUnsigned(int, int)}) as much as possible:
 * 0xFFFFFFFF is <b>not</b> -1 and <b>is greater than</b> 0x00000000.
 * </p>
 * <p>
 * The <b>TypeID</b> dictates what type of file it is, somewhat similar to a
 * file extension (except more strict).<br/>
 * The <b>GroupID</b> indicates what group this file belongs to, such as UI,
 * simulation tuning, objects, etc. This varies depending on what this PAD is
 * for - individual projects and programs will dictate the group schema.<br/>
 * The <b>InstanceID</b> is what uniquely identifies a file. Within the
 * database, only
 * <b>one</b> file may exist with a specific instance ID of the same Type and in
 * the same Group. In other words, Type and Group are <b>NOT</b> unique, whereas
 * Instance is unique.
 * </p>
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public class TypeGroupInstance
        implements Comparable<TypeGroupInstance> {

    public static final TypeGroupInstance ZERO_TGI = new TypeGroupInstance(0, 0, 0L);

    public final int type, group;
    public final long instance;

    /**
     * Creates a TGI with the specified values for type, group, and instance.
     *
     * @param type The TypeID (what kind).
     * @param group The GroupID (for what/with what).
     * @param instance The InstanceID (which one).
     */
    public TypeGroupInstance(int type, int group, long instance) {
        this.type = type;
        this.group = group;
        this.instance = instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeGroupInstance) {
            TypeGroupInstance tgi = (TypeGroupInstance) obj;
            return instance == tgi.instance
                    && group == tgi.group
                    && type == tgi.type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 51;
        hash = 67 * hash + this.type;
        hash = 67 * hash + this.group;
        hash = 67 * hash + (int) (this.instance ^ (this.instance >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return String.format("[0x%08X 0x%08X 0x%016X]", type, group, instance);
    }

    @Override
    public int compareTo(TypeGroupInstance o) {
        if (o == null) {
            return -1;
        }
        //  Must use unsigned comparison.
        if (this.type != o.type) {
            return Integer.compareUnsigned(this.type, o.type);
        }
        if (this.group != o.group) {
            return Integer.compareUnsigned(this.group, o.group);
        }
        return Long.compareUnsigned(this.instance, o.instance);
    }

    @Override
    public TypeGroupInstance clone() {
        return new TypeGroupInstance(type, group, instance);
    }

    
}
