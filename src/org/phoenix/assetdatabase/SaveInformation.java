package org.phoenix.assetdatabase;

/**
 *
 * @author Vince
 */
public class SaveInformation {

    public final long diskOffset;
    public final long diskSize;

    public SaveInformation(long diskOffset, long diskSize) {
        this.diskOffset = diskOffset;
        this.diskSize = diskSize;
    }

}
