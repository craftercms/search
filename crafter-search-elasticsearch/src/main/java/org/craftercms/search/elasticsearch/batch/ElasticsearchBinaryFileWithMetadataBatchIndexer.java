/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.batch;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractBinaryFileWithMetadataBatchIndexer;
import org.craftercms.core.service.Content;
import org.craftercms.search.commons.exception.SearchException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

/**
 * Implementation of {@link AbstractBinaryFileWithMetadataBatchIndexer} for Elasticsearch
 * @author joseross
 */
public class ElasticsearchBinaryFileWithMetadataBatchIndexer extends AbstractBinaryFileWithMetadataBatchIndexer {

    /**
     * Elasticsearch service
     */
    protected ElasticsearchService elasticsearchService;

    @Required
    public void setElasticsearchService(final ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                            final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doDelete(elasticsearchService, indexId, siteName, previousBinaryPath, updateStatus);
    }

    @Override
    protected List<String> searchBinaryPathsFromMetadataPath(final String indexId, final String siteName,
                                                             final String metadataPath) {
        try {
            return elasticsearchService.searchField(indexId, localIdFieldName, Query.of(q -> q
                .term(m -> m
                    .field(metadataPathFieldName)
                    .value(v -> v.stringValue(metadataPath))
                )
            ));
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId, "Error executing search for " + metadataPath, e);
        }
    }

    @Override
    protected String searchMetadataPathFromBinaryPath(final String indexId, final String siteName,
                                                      final String binaryPath) {
        try {
            List<String> paths = elasticsearchService.searchField(indexId, metadataPathFieldName, Query.of(q -> q
                .term(m -> m
                    .field(localIdFieldName)
                    .value(v -> v.stringValue(binaryPath))
                )
            ));
            if(CollectionUtils.isNotEmpty(paths)) {
                return paths.get(0);
            } else {
                return null;
            }
        } catch (ElasticsearchException e) {
           throw new SearchException(indexId, "Error executing search for " + binaryPath, e);
        }
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doUpdateBinary(elasticsearchService, indexId, siteName, binaryPath, metadata,
                resource, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doUpdateBinary(elasticsearchService, indexId, siteName, binaryPath, metadata,
                content, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus) {
        doUpdateContent(indexId, siteName, binaryPath, resource, null, updateDetail, updateStatus);
    }

}
