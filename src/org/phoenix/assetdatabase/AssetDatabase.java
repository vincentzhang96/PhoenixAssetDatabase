package org.phoenix.assetdatabase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a Phoenix Asset Database, which is a file database/container
 * format that uses a triplet of numbers to type, group, and identify asset
 * files. Each {@link TypeGroupInstance} triplet is unique within a database and
 * should be kept universally unique within an application. Duplicate entries
 * should result in the last loaded entry overriding any previous entries. Files
 * can be compressed and have validation hashes (currently MD5, support for others
 * in the future possibly)
 * <p>
 *
 * @version 0.0.0.3
 * @since 2013-11-23
 * @author Vince
 */
public interface AssetDatabase {

    public static final int MAGIC_NUMBER = 0x50414442;  //  'PADB'

    /**
     * Saves the database to file.
     *
     * @throws IOException If the database could not be saved.
     */
    public void save() throws IOException;

    /**
     * Loads the database from file.
     *
     * @throws IOException If the database could not be loaded.
     */
    public void load() throws IOException;

    /**
     * Checks whether or not this database contains a certain TGI.
     *
     * @param tgi The TGI to check for.
     * @return True if the database contains the specified TGI or false
     * otherwise.
     */
    public boolean contains(TypeGroupInstance tgi);

    /**
     * Reads a subfile from the database.
     *
     * @param tgi The TGI of the subfile to get.
     * @return A {@link Subfile} containing the result.
     * @throws FileNotFoundException If no subfile with the specified TGI was
     * found in the database.
     * @throws IOException If there was an issue reading the subfile from the
     * file.
     */
    public Subfile loadSubfile(TypeGroupInstance tgi) throws FileNotFoundException, IOException;

    /**
     * Reads multiple subfiles from the database (bulk operation).
     *
     * @see AssetDatabase#loadSubfile(TypeGroupInstance)
     * @param tgis A collection of TGIs to load. Repeated elements are ignored
     * but discouraged for potential performance reasons (<i>varies on
     * implementation</i>).
     * @return A Map of results.
     * @throws FileNotFoundException If no subfile(s) could be found with a
     * given TGI in the collection.
     * @throws IOException If there was an issue reading any subfile from the
     * file.
     */
    public Map<TypeGroupInstance, Subfile> loadSubfiles(Collection<TypeGroupInstance> tgis) throws FileNotFoundException, IOException;

    /**
     * Adds a subfile to the database for writing.
     * <p>
     * Note that depending on implementation the subfile may not be visible
     * through other methods until the database is saved.
     *
     * @param ie An IndexEntry with the TGI field set. All other fields will be
     * updated on save.
     * @param sf A Subfile with the data and compression information set. All
     * other fields will be updated on save.
     */
    public void putSubfile(IndexEntry ie, Subfile sf);

    /**
     * Adds subfiles to the database for writing.
     *
     * @see AssetDatabase#putSubfile(IndexEntry, Subfile)
     * @param files A map of IndexEntry, Subfile pairs of the subfiles to add
     * with the specified index entry information.
     */
    public void putSubfiles(Map<IndexEntry, Subfile> files);

    /**
     * Removes a subfile, if present, from the database.
     * <p>
     * Note that depending on implementation the change may not be visible
     * through other methods until the database is saved.
     *
     * @param tgi The TGI of the subfile to remove.
     */
    public void removeSubfile(TypeGroupInstance tgi);

    /**
     * Returns the database index that reflects the database on disk.
     * <p>
     * Depending on implementation the returned index may or may not reflect any
     * unsaved changes made to the database.
     *
     * @return
     */
    public Index getIndex();

    /**
     * Returns the database metadata.
     * <p>
     * Changes made to the metadata tables are immediately visible, but are not
     * permanent until the database is saved.
     *
     * @return
     */
    public MetadataList getMetadata();

    /**
     * Clears the database.
     * <p>
     * Note that depending on implementation the change may not be visible
     * through other methods until the database is saved.
     */
    public void clear();

}
