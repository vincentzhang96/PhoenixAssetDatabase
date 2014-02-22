package org.phoenix.assetmanager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.DoubleConsumer;
import org.phoenix.assetdatabase.Subfile;
import org.phoenix.assetdatabase.TypeGroupInstance;

/**
 *
 * @author Vince
 */
public interface PAssetManager {

    public static final double STATUS_SCANNING = -2.0,
            STATUS_FAILED = Double.NEGATIVE_INFINITY;
    
    /**
     * Indexes assets from the databases managed by this PAssetManager. <br/>
     * <b>This function should be called asynchronously from a main or UI thread.</b>
     * Generally, this means scanning a directory and reading the index of valid database files.<br/>
     * Duplicate addresses will result in the last loaded taking precedence, as TGIs are expected to be unique. Which subfile is loaded last depends on the implementation.
     */
    public void indexAssets();

    /**
     * Indexes assets from the databases managed by this PAssetManager. <br/>
     * <b>This function should be called asynchronously from a main or UI thread.</b>
     * Generally, this means scanning a directory and reading the index of valid database files.<br/>
     * Duplicate addresses will result in the last loaded taking precedence, as TGIs are expected to be unique. Which subfile is loaded last depends on the implementation.
     *
     * @param progressUpdateHandler A Consumer that handles progress updates, a float value between 0 and 1, or special negative values. This Consumer is called on the same thread that this function is called on, so the Consumer should dispatch events to the proper thread.
     */
    public void indexAssets(DoubleConsumer progressUpdateHandler);

    /**
     * Retrieves a subfile at the given TGI.
     *
     * @param tgi The TGI of the subfile to get.
     * @return A {@link Subfile} containing the result.
     * @throws FileNotFoundException If no subfile with the specified TGI was found.
     * @throws IOException If there was an issue reading the subfile from the source.
     */
    public Subfile getSubfile(TypeGroupInstance tgi) throws FileNotFoundException, IOException;

    /**
     * Reads multiple subfiles. (bulk operation).
     *
     * @see PAssetManager#getSubfile(TypeGroupInstance)
     * @param tgis A collection of TGIs to load. Repeated elements are ignored but discouraged for potential performance reasons (<i>varies on implementation</i>).
     * @return A Map of results.
     * @throws FileNotFoundException If no subfile(s) could be found with a given TGI in the collection.
     * @throws IOException If there was an issue reading any subfiles.
     */
    public Map<TypeGroupInstance, Subfile> getSubfiles(Collection<TypeGroupInstance> tgis) throws FileNotFoundException, IOException;

    /**
     * Returns whether or not the specified TGI exists.
     *
     * @param tgi The TGI to check.
     * @return True if the TGI exists in the index, false otherwise.
     */
    public boolean contains(TypeGroupInstance tgi);

    /**
     * Returns whether or not all of the TGIs exist.
     *
     * @param tgis A collection of TGIs to check. Repeated elements are ignored but discouraged for potential performance reasons (<i>varies on implementation</i>).
     * @return True if all TGIs exist in the index, false otherwise.
     */
    public boolean containsAll(Collection<TypeGroupInstance> tgis);

    /**
     * Checks if TGIs exist and returns the ones that do.
     *
     * @param tgis A collection of TGIs to check. Repeated elements are ignored but discouraged for potential performance reasons (<i>varies on implementation</i>).
     * @return A Collection containing the TGIs that do exist.
     */
    public Collection<TypeGroupInstance> containsAny(Collection<TypeGroupInstance> tgis);

    /**
     * Clears the PAssetManager of indexed subfiles.
     */
    public void clearIndex();

    /**
     * Clears the PAssetManager cache, if any.
     */
    public void clearCache();

}
