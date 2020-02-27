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
package org.craftercms.search.batch.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.metadata.impl.AbstractMetadataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link BatchIndexer}s. Basically sub-classes only need to provide the processing of each of the files to be indexed.
 *
 * @author avasquez
 */
public abstract class AbstractBatchIndexer extends AbstractMetadataCollector implements BatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBatchIndexer.class);

    protected List<String> includePathPatterns;
    protected List<String> excludePathPatterns;

    public void setIncludePathPatterns(List<String> includePathPatterns) {
        this.includePathPatterns = includePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }

    @Override
    public void updateIndex(String indexId, String siteName, ContentStoreService contentStoreService,
                            Context context, UpdateSet updateSet, UpdateStatus updateStatus) throws BatchIndexingException {
        for (String path : updateSet.getUpdatePaths()) {
            if (include(path)) {
                try {
                    Map<String, String> metadata = collectMetadata(path, contentStoreService, context);
                    doSingleFileUpdate(indexId, siteName, contentStoreService, context, path, false,
                        updateSet.getUpdateDetail(path), updateStatus, metadata);
                } catch (Exception e) {
                    logger.error("Error while trying to perform update of file {}:{}", siteName, path, e);

                    updateStatus.addFailedUpdate(path);
                }
            }
        }

        for (String path : updateSet.getDeletePaths()) {
            if (include(path)) {
                try {
                    doSingleFileUpdate(indexId, siteName, contentStoreService, context, path, true, null,
                        updateStatus, Collections.emptyMap());
                } catch (Exception e) {
                    logger.error("Error while trying to perform delete of file {}:{}", siteName, path, e);

                    updateStatus.addFailedDelete(path);
                }
            }
        }
    }

    protected boolean include(String path) {
        return (CollectionUtils.isEmpty(includePathPatterns) || RegexUtils.matchesAny(path, includePathPatterns)) &&
               (CollectionUtils.isEmpty(excludePathPatterns) || !RegexUtils.matchesAny(path, excludePathPatterns));
    }

    protected abstract void doSingleFileUpdate(String indexId, String siteName,
                                               ContentStoreService contentStoreService, Context context,
                                               String path, boolean delete, UpdateDetail updateDetail,
                                               UpdateStatus updateStatus, Map<String, String> metadata) throws Exception;

}
