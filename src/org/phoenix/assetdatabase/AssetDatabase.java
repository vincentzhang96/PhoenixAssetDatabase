package org.phoenix.assetdatabase;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Vince
 */
public interface AssetDatabase {
    
    public void save() throws IOException;
    
    public void load() throws IOException;
    
    public boolean contains(TypeGroupInstance tgi);
    
    public Subfile loadSubfile(TypeGroupInstance tgi) throws IOException;
    
    public Map<TypeGroupInstance, Subfile> loadSubfiles(Collection<TypeGroupInstance> tgis) throws IOException;
    
    public void putSubfile(IndexEntry ie, Subfile sf);
    
    public void putSubfiles(Map<IndexEntry,Subfile> files);
    
    public void removeSubfile(TypeGroupInstance tgi);
    
    public Index getIndex();
    
    public MetadataList getMetadata();
    
    public void clear();
    
}
