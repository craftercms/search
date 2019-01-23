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
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;

/**
 * Implementation of {@link BatchIndexer} that updates existing entries in the index with additional metadata.
 * @author joseross
 */
public abstract class AbstractMetadataBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(AbstractMetadataBatchIndexer.class);

    /**
     * Pattern of files that should be included
     */
    protected List<String> includePatterns;

    public void setIncludePatterns(final List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateIndex(final String indexName, final String siteName,
                            final ContentStoreService contentStoreService, final Context context,
                            final UpdateSet updateSet, final UpdateStatus updateStatus) throws BatchIndexingException {

        logger.info("Start processing @ " + indexName);

        List<String> updatedPaths = updateSet.getUpdatePaths();
        List<String> includedPaths = Collections.emptyList();
        List<String> compatiblePaths = Collections.emptyList();

        if(CollectionUtils.isNotEmpty(updatedPaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Filtering updated files @ " + indexName + ": " + updatedPaths);
            }
            includedPaths = updatedPaths.stream()
                                .filter(path -> CollectionUtils.isEmpty(includePatterns) ||
                                                    RegexUtils.matchesAny(path, includePatterns))
                                .collect(Collectors.toList());
        }

        if(CollectionUtils.isNotEmpty(includedPaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Filtering matched files @ " + indexName + ": " + includedPaths);
            }
            compatiblePaths = includedPaths.stream()
                                .filter(path -> isCompatible(getItem(contentStoreService, context, path)))
                                .collect(Collectors.toList());
        }

        if(CollectionUtils.isNotEmpty(compatiblePaths)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Processing compatible files @ " + indexName + ": " + compatiblePaths);
            }
            compatiblePaths.forEach(path -> {
                if(logger.isDebugEnabled()) {
                    logger.debug("Fetching metadata for file "+ path +"  @ " + indexName);
                }
                Map<String, Object> metadata = getMetadata(getItem(contentStoreService, context, path),
                    contentStoreService, context);
                if(MapUtils.isNotEmpty(metadata)) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("Fetching existing data for file "+ path +"  @ " + indexName);
                    }
                    Map<String, Object> currentData = getCurrentData(indexName, siteName, path);
                    if(MapUtils.isNotEmpty(currentData)) {
                        if(logger.isDebugEnabled()) {
                            logger.debug("Indexing new metadata for file "+ path +"  @ " + indexName);
                        }
                        currentData.putAll(metadata);
                        updateIndex(indexName, siteName, path, currentData);
                    }
                }
            });
        }

        logger.info("Completed processing @ " + indexName);

    }

    /**
     * Looks up a specific file from the content store
     * @param contentStoreService the content store service instance
     * @param context the context instance
     * @param path the path of the file
     * @return instance of Item
     */
    protected Item getItem(final ContentStoreService contentStoreService, final Context context, final String path) {
        return contentStoreService.getItem(context, path);
    }

    /**
     * Updates the index for the given file
     * @param indexName the name of the index
     * @param siteName the name of the site
     * @param path the path of the file
     * @param data the metadata to add
     */
    protected abstract void updateIndex(final String indexName, final String siteName, final String path,
                                        final Map<String, Object> data);

    /**
     * Queries the index to get existing data for a given file
     * @param indexName the name of the index
     * @param siteName the name of the site
     * @param path the path of the file
     * @return the existing data
     */
    protected abstract Map<String, Object> getCurrentData(final String indexName, final String siteName,
                                                          final String path);

    /**
     * Gets the additional metadata that will be added to the index for the given {@link Item}
     * @param item the item to process
     * @param contentStoreService the content store service instance
     * @param context the context instance
     * @return the additional metadata
     */
    protected abstract Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                                       final Context context);

    /**
     * Verifies if the given {@link Item} can be handled by this class
     * @param item the item to verify
     * @return true if the item can be handled
     */
    protected abstract boolean isCompatible(final Item item);

}
