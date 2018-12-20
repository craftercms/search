/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.batch.utils;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.ResourceAwareSearchService;
import org.craftercms.search.service.SearchService;
import org.springframework.core.io.Resource;

import javax.activation.FileTypeMap;

/**
 * Utility methods used for simplifying REST search service update calls.
 *
 * @author avasquez
 */
public class IndexingUtils {

    private static final Log logger = LogFactory.getLog(IndexingUtils.class);

    private IndexingUtils() {
    }

    public static boolean isMimeTypeSupported(FileTypeMap mimeTypesMap, List<String> supportedMimeTypes,
                                              String filename) {
        if (mimeTypesMap != null && CollectionUtils.isNotEmpty(supportedMimeTypes)) {
            return supportedMimeTypes.contains(mimeTypesMap.getContentType(filename.toLowerCase()));
        } else {
            return true;
        }
    }

    public static void doUpdate(SearchService searchService, String indexId, String siteName, String id, String xml,
                                UpdateStatus updateStatus) {
        searchService.update(indexId, siteName, id, xml, true);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id,
                                       Content content, UpdateStatus updateStatus) {
        searchService.updateContent(indexId, siteName, id, content);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    @SuppressWarnings("unchecked")
    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id,
                                       Content content, Map<String, List<String>> additionalFields,
                                       UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, content, additionalFields);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(ResourceAwareSearchService searchService, String indexId, String siteName,
                                       String id, Resource resource, UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, resource);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(ResourceAwareSearchService searchService, String indexId, String siteName,
                                       String id, Resource resource, Map<String, List<String>> additionalFields,
                                       UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, resource, additionalFields);

        logger.info("File " + getSiteBasedPath(siteName, id) + " added to index " + getIndexNameStr(indexId));

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doDelete(SearchService searchService, String indexId, String siteName, String id,
                                UpdateStatus updateStatus) {
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
