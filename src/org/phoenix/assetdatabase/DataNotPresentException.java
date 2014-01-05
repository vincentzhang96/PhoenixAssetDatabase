package org.phoenix.assetdatabase;

import java.io.IOException;

/**
 *
 * @author Vince
 */
public class DataNotPresentException extends IOException {

    public DataNotPresentException(String message) {
        super(message);
    }
    
}
