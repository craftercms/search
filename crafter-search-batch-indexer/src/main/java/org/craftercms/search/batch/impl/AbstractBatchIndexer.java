/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.search.batch.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.search.batch.utils.IndexingUtils.*;

/**
 * Base class for {@link BatchIndexer}s. Basically sub-classes only need to provide the processing of each of the files to be indexed.
 *
 * @author avasquez
 */
public abstract class AbstractBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(AbstractBatchIndexer.class);

    protected SearchService searchService;
    protected List<String> includeFileNamePatterns;
    protected List<String> excludeFileNamePatterns;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setIncludeFileNamePatterns(List<String> includeFileNamePatterns) {
        this.includeFileNamePatterns = includeFileNamePatterns;
    }

    public void setExcludeFileNamePatterns(List<String> excludeFileNamePatterns) {
        this.excludeFileNamePatterns = excludeFileNamePatterns;
    }

    @Override
    public void updateIndex(String indexId, String siteName, ContentStoreService contentStoreService, Context context, List<String> paths,
                            boolean delete, IndexingStatus status) throws BatchIndexingException {
        for (String path : paths) {
            if (include(path)) {
                try {
                    doSingleFileUpdate(indexId, siteName, contentStoreService, context, path, delete, status);
                } catch (Exception e) {
                    if (delete) {
                        logger.error("Error while trying to perform delete of file " + getSiteBasedPath(siteName, path), e);

                        status.addFailedDelete(path);
                    } else {
                        logger.error("Error while trying to perform update of file " + getSiteBasedPath(siteName, path), e);

                        status.addFailedUpdate(path);
                    }
                }
            }
        }
    }

    protected boolean include(String fileName) {
        boolean update = true;

        if (CollectionUtils.isNotEmpty(includeFileNamePatterns) &&
            !RegexUtils.matchesAny(fileName, includeFileNamePatterns)) {
            update = false;
        }
        if (CollectionUtils.isNotEmpty(excludeFileNamePatterns) &&
            RegexUtils.matchesAny(fileName, excludeFileNamePatterns)) {
            update = false;
        }

        return update;
    }

    protected abstract void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                                               String path, boolean delete, IndexingStatus status) throws Exception;

}
