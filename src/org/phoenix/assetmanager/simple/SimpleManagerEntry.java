package org.phoenix.assetmanager.simple;

import java.nio.file.Path;
import org.phoenix.assetdatabase.TypeGroupInstance;

/**
 *
 * @author Vince
 */
public class SimpleManagerEntry {

    public final TypeGroupInstance tgi;
    public final Path padLocation;

    public SimpleManagerEntry(TypeGroupInstance tgi, Path padLocation) {
        this.tgi = tgi;
        this.padLocation = padLocation;
    }
    
}
