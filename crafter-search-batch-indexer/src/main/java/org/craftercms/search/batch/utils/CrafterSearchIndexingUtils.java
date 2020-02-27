/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
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

import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.Content;
import org.craftercms.search.service.ResourceAwareSearchService;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Utility methods used for simplifying REST search service update calls.
 *
 * @author avasquez
 */
@SuppressWarnings("unchecked")
public abstract class CrafterSearchIndexingUtils extends IndexingUtils {

    private static final Logger logger = LoggerFactory.getLogger(CrafterSearchIndexingUtils.class);

    public static void doUpdate(SearchService searchService, String indexId, String siteName, String id, String xml,
                                UpdateStatus updateStatus) {
        searchService.update(indexId, siteName, id, xml, true);

        logUpdate(indexId, siteName, id);

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id,
                                       Content content, UpdateStatus updateStatus) {
        searchService.updateContent(indexId, siteName, id, content);

        logUpdate(indexId, siteName, id);

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(SearchService searchService, String indexId, String siteName, String id,
                                       Content content, Map<String, List<String>> additionalFields,
                                       UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, content, additionalFields);

        logUpdate(indexId, siteName, id);

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(ResourceAwareSearchService searchService, String indexId, String siteName,
                                       String id, Resource resource, UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, resource);

        logUpdate(indexId, siteName, id);

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doUpdateContent(ResourceAwareSearchService searchService, String indexId, String siteName,
                                       String id, Resource resource, Map<String, List<String>> additionalFields,
                                       UpdateStatus updateStatus)  {
        searchService.updateContent(indexId, siteName, id, resource, additionalFields);

        logUpdate(indexId, siteName, id);

        updateStatus.addSuccessfulUpdate(id);
    }

    public static void doDelete(SearchService searchService, String indexId, String siteName, String id,
                                UpdateStatus updateStatus) {
        searchService.delete(indexId, siteName, id);

        logDelete(indexId, siteName, id);

        updateStatus.addSuccessfulDelete(id);
    }

    private static void logUpdate(String indexId, String siteName, String id) {
        logger.debug("File {}:{} added to index {}", siteName, id,
                     StringUtils.isNotEmpty(indexId) ? indexId : "default");
    }

    private static void logDelete(String indexId, String siteName, String id) {
        logger.debug("File {}:{} deleted to index {}", siteName, id,
                     StringUtils.isNotEmpty(indexId) ? indexId : "default");
    }

}