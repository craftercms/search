package org.craftercms.search.batch;

import java.util.List;

import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
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
     * @param indexId               the ID of the index, or null to use a default index
     * @param siteName              the name of the site the files belong to
     * @param contentStoreService   the content store service used to retrieve the files and content to index
     * @param context               the context of the file store being used
     * @param paths                 the paths in the content store of the files to index
     * @param delete                if the given files should be deleted from the index, otherwise they're added
     * @param status                status object used to track index updates and deletes
     *
     * @return the number of files that where actually added/deleted
     */
    void updateIndex(String indexId, String siteName, ContentStoreService contentStoreService, Context context, List<String> paths,
                     boolean delete, IndexingStatus status) throws BatchIndexingException;

}
