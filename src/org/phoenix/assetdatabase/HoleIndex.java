package org.phoenix.assetdatabase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
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
public class HoleIndex {

    private final Set<HoleIndexEntry> entries;

    public HoleIndex() {
        entries = new LinkedHashSet<>();
    }

    public List<HoleIndexEntry> getEntriesAsListImmutable() {
        return Collections.unmodifiableList(new ArrayList<HoleIndexEntry>(entries));
    }

    public List<HoleIndexEntry> getEntriesAsList() {
        return new ArrayList<>(entries);
    }

    public Set<HoleIndexEntry> getEntriesImmutable() {
        return Collections.unmodifiableSet(entries);
    }

    public Set<HoleIndexEntry> getEntries() {
        return entries;
    }

    public void load(DataInput in, int numEntries) throws IOException {
        requireNonNull(in, "DataInput cannot be null.");
        entries.clear();
        for (int count = 0; count < numEntries; count++) {
            HoleIndexEntry e = new HoleIndexEntry();
            e.load(in);
            entries.add(e);
        }
    }

    public void save(DataOutput out) throws IOException {
        requireNonNull(out, "DataOut cannot be null.");
        for (HoleIndexEntry ie : entries) {
            ie.save(out);
        }
    }
    
    public int getSizeBytes()
    {
        return HoleIndexEntry.SIZEOF * entries.size();
    }

    public void clear()
    {
        entries.clear();
    }
    
}
