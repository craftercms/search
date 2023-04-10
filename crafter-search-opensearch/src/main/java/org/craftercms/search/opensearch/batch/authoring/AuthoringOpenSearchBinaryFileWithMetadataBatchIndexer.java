/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.batch.authoring;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.opensearch.OpenSearchService;
import org.craftercms.search.opensearch.batch.OpenSearchBinaryFileWithMetadataBatchIndexer;
import org.craftercms.search.batch.UpdateStatus;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.craftercms.search.batch.utils.IndexingUtils.isMimeTypeSupported;
import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;

/**
 * Implementation of {@link OpenSearchBinaryFileWithMetadataBatchIndexer} for OpenSearch of authoring.
 * Override method {@link org.craftercms.search.batch.impl.AbstractBinaryFileWithMetadataBatchIndexer#doUpdates(String, String, ContentStoreService, Context, UpdateSet, UpdateStatus)}
 * and method {@link org.craftercms.search.batch.impl.AbstractBinaryFileWithMetadataBatchIndexer#doDeletes(String, String, ContentStoreService, Context, List, UpdateStatus)}
 * to support authoring binary indexing
 * @author Phil Nguyen
 */
public class AuthoringOpenSearchBinaryFileWithMetadataBatchIndexer extends OpenSearchBinaryFileWithMetadataBatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(AuthoringOpenSearchBinaryFileWithMetadataBatchIndexer.class);
    protected List<String> binaryPathPatterns;
    protected List<String> binarySearchablePathPatterns;

    public AuthoringOpenSearchBinaryFileWithMetadataBatchIndexer(final OpenSearchService openSearchService) {
        super(openSearchService);
    }

    @Override
    protected void doUpdates(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             UpdateSet updateSet, UpdateStatus updateStatus) {
        Set<String> metadataUpdatePaths = new LinkedHashSet<>();
        Set<String> binaryUpdatePaths = new LinkedHashSet<>();
        Set<String> binarySearchablePaths = new LinkedHashSet<>();

        buildUpdatePaths(updateSet, metadataUpdatePaths, binarySearchablePaths, binaryUpdatePaths);

        updateMetadataPaths(indexId, siteName, contentStoreService, context, updateSet, updateStatus,
                metadataUpdatePaths, binaryUpdatePaths);

        addBinariesFromSearchablePaths(siteName, contentStoreService, context, binarySearchablePaths, binaryUpdatePaths);

        updateBinaryPaths(indexId, siteName, contentStoreService, context, updateSet, updateStatus,
                binaryUpdatePaths);
    }

    @Override
    protected void doDeletes(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             List<String> deletePaths, UpdateStatus updateStatus) {
        for (String path : deletePaths) {
            if (isMetadata(path)) {
                doDeleteMetadata(indexId, siteName, contentStoreService, context, path, updateStatus);
            } else if (isBinary(path)) {
                doDelete(indexId, siteName, path, updateStatus);
            }
        }
    }

    /**
     * Build index paths by type of binary or metadata
     * @param updateSet indexing update set
     * @param metadataUpdatePaths metadata paths set
     * @param binarySearchablePaths binary searchable paths set
     * @param binaryUpdatePaths binary paths set
     */
    private void buildUpdatePaths(UpdateSet updateSet, Set<String> metadataUpdatePaths,
                                        Set<String> binarySearchablePaths, Set<String> binaryUpdatePaths) {
        List<String> updatePaths = updateSet.getUpdatePaths();
        for (String path : updatePaths) {
            if (isMetadata(path)) {
                metadataUpdatePaths.add(path);
            } else if (isBinary(path)) {
                binaryUpdatePaths.add(path);
            } else if (isBinarySearchable(path)) {
                binarySearchablePaths.add(path);
            }
        }
    }

    /**
     * Update metadata paths with a side effect to update list of binary should be updated
     * @param indexId the index to update
     * @param siteName site name
     * @param contentStoreService instance of content store service
     * @param context the context
     * @param updateSet update set
     * @param updateStatus update status
     * @param metadataUpdatePaths metadata paths to be indexed
     * @param binaryUpdatePaths binary paths to be updated
     */
    private void updateMetadataPaths(String indexId, String siteName, ContentStoreService contentStoreService,
                                     Context context, UpdateSet updateSet, UpdateStatus updateStatus,
                                     Set<String> metadataUpdatePaths, Set<String> binaryUpdatePaths) {
        for (String metadataPath : metadataUpdatePaths) {
            Collection<String> newBinaryPaths = Collections.emptyList();
            List<String> previousBinaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, metadataPath);
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);

            if (metadataDoc != null) {
                newBinaryPaths = getBinaryFilePaths(metadataDoc);
            }

            // If there are previous binaries that are not associated to the metadata anymore, reindex them without
            // metadata or delete them if they're child binaries.
            updatePreviousBinaries(indexId, siteName, metadataPath, previousBinaryPaths, newBinaryPaths,
                    binaryUpdatePaths, context, contentStoreService, updateSet.getUpdateDetail(metadataPath), updateStatus);

            // Index the new associated binaries
            if (isNotEmpty(newBinaryPaths)) {
                Map<String, Object> metadata = extractMetadata(metadataPath, metadataDoc);

                for (String newBinaryPath : newBinaryPaths) {
                    binaryUpdatePaths.remove(newBinaryPath);

                    Map<String, Object> additionalFields = collectMetadata(metadataPath, contentStoreService, context);
                    Map<String, Object> mergedMetadata = mergeMaps(metadata, additionalFields);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, newBinaryPath,
                            mergedMetadata, updateSet.getUpdateDetail(metadataPath), updateStatus);
                }
            }
        }
    }

    /**
     * Update binary paths from a list of paths which may contain associated binaries
     * @param siteName the site name
     * @param contentStoreService instance of content store service
     * @param context the context
     * @param binarySearchablePaths paths to search for associated binaries
     * @param binaryUpdatePaths list of binaries paths to be updated
     */
    private void addBinariesFromSearchablePaths(String siteName, ContentStoreService contentStoreService,
                                                Context context, Set<String> binarySearchablePaths, Set<String> binaryUpdatePaths) {
        // add binary path from xml document which contains binaries references
        for (String binarySearchPath : binarySearchablePaths) {
            Collection<String> newBinaryPaths = Collections.emptyList();
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, binarySearchPath);

            if (metadataDoc != null) {
                newBinaryPaths = getBinaryFilePaths(metadataDoc);
            }

            if (isNotEmpty(newBinaryPaths)) {
                binaryUpdatePaths.addAll(newBinaryPaths);
            }
        }
    }

    /**
     * Load metadata from set of binaries and do index accordingly
     * @param indexId name of the index
     * @param siteName site name
     * @param contentStoreService content store service
     * @param context index context
     * @param updateSet update set
     * @param updateStatus update status
     * @param binaryUpdatePaths set of binary paths to be indexed
     */
    private void updateBinaryPaths(String indexId, String siteName, ContentStoreService contentStoreService,
                                            Context context, UpdateSet updateSet, UpdateStatus updateStatus,
                                            Set<String> binaryUpdatePaths) {
        for (String binaryPath : binaryUpdatePaths) {
            String metadataPath = searchMetadataPathFromBinaryPath(indexId, siteName, binaryPath);
            if (StringUtils.isNotEmpty(metadataPath)) {
                // If the binary file has an associated metadata, index the file with the metadata
                Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);
                if (metadataDoc != null) {
                    Map<String, Object> metadata = extractMetadata(metadataPath, metadataDoc);

                    Map<String, Object> additionalFields = collectMetadata(metadataPath, contentStoreService, context);
                    Map<String, Object> mergedMetadata = mergeMaps(metadata, additionalFields);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, binaryPath,
                            mergedMetadata, updateSet.getUpdateDetail(metadataPath), updateStatus);
                }
            } else {
                // If not, index by itself
                updateBinary(indexId, siteName, contentStoreService, context, binaryPath,
                        updateSet.getUpdateDetail(binaryPath), updateStatus);
            }
        }
    }

    /**
     * Delete a metadata path.
     * Search for all binary associated with this metadata and delete them
     * @param indexId the index id
     * @param siteName the site name
     * @param contentStoreService instance of content store service
     * @param context the context
     * @param path path to process delete indexing
     * @param updateStatus update status
     */
    private void doDeleteMetadata(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             String path, UpdateStatus updateStatus) {
        List<String> binaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, path);
        for (String binaryPath : binaryPaths) {
            if (isChildBinary(binaryPath)) {
                logger.debug("Parent of binary {} deleted. Deleting child binary too", binaryPath);

                // If the binary is a child binary, when the metadata file is deleted, then delete it
                doDelete(indexId, siteName, binaryPath, updateStatus);
            } else {
                logger.debug("Metadata with reference of binary {} deleted. Reindexing without metadata...",
                        binaryPath);

                // Else, update binary without metadata
                updateBinary(indexId, siteName, contentStoreService, context, binaryPath,null, updateStatus);
            }
        }
    }

    /**
     * Check if a path is binary or not
     * @param path the path to check
     * @return true if matched binary pattern and is a supported mime-type, false otherwise
     */
    protected boolean isBinary(String path) {
        return RegexUtils.matchesAny(path, binaryPathPatterns) &&
                isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, path);
    }

    /**
     * Check if a path could be searched for any binary
     * @param path to check
     * @return true if matched the defined pattern, false otherwise
     */
    protected boolean isBinarySearchable(String path) {
        return RegexUtils.matchesAny(path, binarySearchablePathPatterns);
    }

    /**
     * Setter method for binary path patterns
     * @param binaryPathPatterns binary path patterns
     */
    public void setBinaryPathPatterns(List<String> binaryPathPatterns) {
        this.binaryPathPatterns = binaryPathPatterns;
    }

    /**
     * Setter method for binary searchable patterns
     * @param binarySearchablePathPatterns binary searchable patterns
     */
    public void setBinarySearchablePathPatterns(List<String> binarySearchablePathPatterns) {
        this.binarySearchablePathPatterns = binarySearchablePathPatterns;
    }
}
