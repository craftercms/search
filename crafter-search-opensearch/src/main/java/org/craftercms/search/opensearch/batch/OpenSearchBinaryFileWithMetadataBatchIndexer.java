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

package org.craftercms.search.opensearch.batch;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractBinaryFileWithMetadataBatchIndexer;
import org.craftercms.search.commons.exception.SearchException;
import org.craftercms.search.opensearch.OpenSearchService;
import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.core.io.Resource;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AbstractBinaryFileWithMetadataBatchIndexer} for OpenSearch
 * @author joseross
 */
public class OpenSearchBinaryFileWithMetadataBatchIndexer extends AbstractBinaryFileWithMetadataBatchIndexer {

    /**
     * OpenSearch service
     */
    protected final OpenSearchService searchService;

    @ConstructorProperties({"searchService"})
    public OpenSearchBinaryFileWithMetadataBatchIndexer(final OpenSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                            final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doDelete(searchService, indexId, siteName, previousBinaryPath, updateStatus);
    }

    @Override
    protected List<String> searchBinaryPathsFromMetadataPath(final String indexId, final String siteName,
                                                             final String metadataPath) {
        try {
            return searchService.searchField(indexId, localIdFieldName, Query.of(q -> q
                .term(m -> m
                    .field(metadataPathFieldNameWithKeyword())
                    .value(v -> v.stringValue(metadataPath))
                )
            ));
        } catch (OpenSearchException e) {
            throw new SearchException(indexId, "Error executing search for " + metadataPath, e);
        }
    }

    @Override
    protected String searchMetadataPathFromBinaryPath(final String indexId, final String siteName,
                                                      final String binaryPath) {
        try {
            List<String> paths = searchService.searchField(indexId, metadataPathFieldName, Query.of(q -> q
                .bool(b -> b
                    .filter(m -> m
                        .term(t -> t
                            .field(localIdFieldName)
                            .value(v -> v.stringValue(binaryPath))
                        )
                    )
                    .filter(m -> m
                        .exists(e -> e
                            .field(metadataPathFieldName)
                        )
                    )
                )
            ));
            if(CollectionUtils.isNotEmpty(paths)) {
                return paths.get(0);
            } else {
                return null;
            }
        } catch (OpenSearchException e) {
           throw new SearchException(indexId, "Error executing search for " + binaryPath, e);
        }
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doUpdateBinary(searchService, indexId, siteName, binaryPath, metadata,
                resource, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doUpdateBinary(searchService, indexId, siteName, binaryPath, metadata,
                content, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus) {
        doUpdateContent(indexId, siteName, binaryPath, resource, null, updateDetail, updateStatus);
    }

    /**
     * * Add `.keyword` to field name to search with keyword
     * @return metadataPath with `.keyword` ending
     */
    protected String metadataPathFieldNameWithKeyword() {
        return metadataPathFieldName + ".keyword";
    }

}
