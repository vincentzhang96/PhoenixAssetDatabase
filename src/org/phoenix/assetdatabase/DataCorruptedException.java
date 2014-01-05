package org.phoenix.assetdatabase;

import java.io.IOException;

/**
 *
 * @author Vince
 */
public class DataCorruptedException extends IOException {

    public DataCorruptedException(String message) {
        super(message);
    }
    
}
