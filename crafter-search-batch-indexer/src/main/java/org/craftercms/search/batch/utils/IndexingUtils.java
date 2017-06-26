package org.craftercms.search.batch.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.SearchService;

/**
 * Created by alfonso on 6/23/17.
 */
public class IndexingUtils {

    private static final Log logger = LogFactory.getLog(IndexingUtils.class);

    private IndexingUtils() {
    }

    public static void doUpdate(SearchService searchService, String indexId, String siteName, String id, String xml,
                                UpdateStatus updateStatus) {
        searchService.update(indexId, siteName, id, xml, true);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id, Content content,
                                       UpdateStatus updateStatus) throws IOException {
        try (InputStream is = content.getInputStream()) {
            searchService.updateContent(indexId, siteName, id, is);

            logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

            updateStatus.addSuccessfulUpdate(id);
        }
    }

    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id, Content content,
                                       Map<String, List<String>> additionalFields, UpdateStatus updateStatus) throws IOException {
        try (InputStream is = content.getInputStream()) {
            searchService.updateContent(indexId, siteName, id, is, additionalFields);

            logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

            updateStatus.addSuccessfulUpdate(id);
        }
    }

    public static void doDelete(SearchService searchService, String indexId, String siteName, String id, UpdateStatus updateStatus) {
        searchService.delete(indexId, siteName, id);

        logger.info("File " + getSiteBasedPath(siteName, id) + " deleted from index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulDelete(id);
    }

    public static String getSiteBasedPath(String siteName, String path) {
        return siteName + ":" + path;
    }

    public static String getIndexNameStr(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "'" + indexId + "'": "default";
    }

}
