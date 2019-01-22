/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;

/**
 * @author joseross
 */
public abstract class AbstractMetadataBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(AbstractMetadataBatchIndexer.class);

    protected String includePath;

    public void setIncludePath(final String includePath) {
        this.includePath = includePath;
    }

    @Override
    public void updateIndex(final String indexId, final String siteName,
                            final ContentStoreService contentStoreService, final Context context,
                            final UpdateSet updateSet, final UpdateStatus updateStatus) throws BatchIndexingException {

        logger.info("Start processing @ " + indexId);

        List<String> updatedPaths = updateSet.getUpdatePaths();
        List<String> includedPaths = Collections.emptyList();
        List<String> compatiblePaths = Collections.emptyList();

        if(CollectionUtils.isNotEmpty(updatedPaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Filtering updated files @ " + indexId + ": " + updatedPaths);
            }
            includedPaths = updatedPaths.stream()
                                .filter(path -> StringUtils.isEmpty(includePath) || path.matches(includePath))
                                .collect(Collectors.toList());
        }

        if(CollectionUtils.isNotEmpty(includedPaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Filtering matched files @ " + indexId + ": " + includedPaths);
            }
            compatiblePaths = includedPaths.stream()
                                .filter(path -> isCompatible(getItem(contentStoreService, context, path)))
                                .collect(Collectors.toList());
        }

        if(CollectionUtils.isNotEmpty(compatiblePaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Processing compatible files @ " + indexId + ": " + compatiblePaths);
            }
            compatiblePaths.forEach(path -> {
                if(logger.isDebugEnabled()) {
                    logger.debug("Fetching metadata for file "+ path +"  @ " + indexId);
                }
                Map<String, Object> metadata = getMetadata(getItem(contentStoreService, context, path),
                    contentStoreService, context);
                if(MapUtils.isNotEmpty(metadata)) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("Fetching existing data for file "+ path +"  @ " + indexId);
                    }
                    Map<String, Object> currentData = getCurrentData(indexId, siteName, path);
                    if(MapUtils.isNotEmpty(currentData)) {
                        if(logger.isDebugEnabled()) {
                            logger.debug("Indexing new metadata for file "+ path +"  @ " + indexId);
                        }
                        currentData.putAll(metadata);
                        index(indexId, siteName, path, currentData);
                    }
                }
            });
        }

        logger.info("Completed processing @ " + indexId);

    }

    protected Item getItem(final ContentStoreService contentStoreService, final Context context, final String path) {
        return contentStoreService.getItem(context, path);
    }

    protected abstract void index(final String indexId, final String siteName, final String path,
                                  final Map<String, Object> data);

    protected abstract Map<String, Object> getCurrentData(final String indexId, final String siteName,
                                                          final String path);

    protected abstract Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                                       final Context context);

    protected abstract boolean isCompatible(final Item item);

}
