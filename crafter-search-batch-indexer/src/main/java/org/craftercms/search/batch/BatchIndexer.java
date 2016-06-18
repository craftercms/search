package org.craftercms.search.batch;

import java.util.List;

import org.craftercms.search.batch.exception.BatchIndexingException;

/**
 * Classes that implement this interface update or delete batches of files from a specified search index.
 *
 * @author avasquez
 */
public interface BatchIndexer {

    /**
     * Updates the specified search index with the given batch of files.
     *
     * @param indexId       the ID of the index, or null to use a default index
     * @param siteName      the name of the site the files belong to
     * @param rootFolder    the root folder in the file system where the files are
     * @param fileNames     the names or paths of the files to update/delete, relative to the root folder
     * @param delete        if the given files should be deleted from the index, otherwise they're added
     *
     * @return the number of files that where actually added/deleted
     */
    int updateIndex(String indexId, String siteName, String rootFolder, List<String> fileNames,
                    boolean delete) throws BatchIndexingException;

}
