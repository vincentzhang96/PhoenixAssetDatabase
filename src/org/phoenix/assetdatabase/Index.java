package org.phoenix.assetdatabase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author Vince
 */
public class Index {

    /**
     * Contains the entries of this Index
     */
    private final Set<IndexEntry> entries;
    
    
    public Index() {
        entries = new LinkedHashSet<>();
    }
    
    public List<IndexEntry> getEntriesAsListImmutable() {
        return Collections.unmodifiableList(getEntriesAsList());
    }
    
    public List<IndexEntry> getEntriesAsList() {
        return new ArrayList<>(entries);
    }
    
    public Set<IndexEntry> getEntriesImmutable() {
        return Collections.unmodifiableSet(entries);
    }
    
    public Set<IndexEntry> getEntries() {
        return entries;
    }
    
    public IndexEntry getEntry(TypeGroupInstance tgi) {
        requireNonNull(tgi, "TGI cannot be null.");
        for(IndexEntry ie : entries) {
            if(ie.getTgi().equals(tgi)) {
                return ie;
            }
        }
        return null;
    }
    
    public void load(RandomAccessFile in, int numEntries) throws IOException {
        requireNonNull(in, "DataInput cannot be null.");
        entries.clear();
        for(int count = 0; count < numEntries; count++) {
            IndexEntry e = IndexEntry.load(in);
            entries.add(e);
        }
    }
    
    public void save(RandomAccessFile out) throws IOException {
        requireNonNull(out, "DataOut cannot be null.");
        for (IndexEntry ie : entries) {
            ie.save(out);
        }
    }
    
    public int getSizeBytes()
    {
        return IndexEntry.SIZEOF * entries.size();
    }
    
    @Override
    public Index clone() {
        Index i = new Index();
        for(IndexEntry ie : entries) {
            i.entries.add(ie.clone());
        }
        return i;
    }
    
    public void clear(){
        entries.clear();
    }
}
