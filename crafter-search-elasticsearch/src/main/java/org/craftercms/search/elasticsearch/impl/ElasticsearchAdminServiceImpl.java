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

package org.craftercms.search.elasticsearch.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Default implementation of {@link ElasticsearchAdminService}
 * @author joseross
 */
public class ElasticsearchAdminServiceImpl implements ElasticsearchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAdminServiceImpl.class);

    public static final String DEFAULT_INDEX_NAME_SUFFIX = "_v1";

    /**
     * The suffix to add to all index names during creation
     */
    protected String indexNameSuffix = DEFAULT_INDEX_NAME_SUFFIX;

    /**
     * Index settings file for authoring indices
     */
    protected Resource authoringIndexSettings;

    /**
     * Index settings file for preview indices
     */
    protected Resource previewIndexSettings;

    /**
     * The Elasticsearch client
     */
    protected RestHighLevelClient elasticsearchClient;

    public ElasticsearchAdminServiceImpl(final Resource authoringIndexSettings, final Resource previewIndexSettings,
                                         final RestHighLevelClient elasticsearchClient) {
        this.authoringIndexSettings = authoringIndexSettings;
        this.previewIndexSettings = previewIndexSettings;
        this.elasticsearchClient = elasticsearchClient;
    }

    public void setIndexNameSuffix(final String indexNameSuffix) {
        this.indexNameSuffix = indexNameSuffix;
    }

    /**
     * Checks if a given index already exists in Elasticsearch
     * @param client the elasticsearch client
     * @param indexName the index name
     */
    protected boolean exists(RestHighLevelClient client, String indexName) {
        logger.debug("Checking if index {} exits", indexName);
        try {
            return client.indices().exists(
                new GetIndexRequest().indices(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticsearchException(indexName, "Error consulting index", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String indexName, boolean isAuthoring) throws ElasticsearchException {
        doCreateIndex(elasticsearchClient, indexName, isAuthoring);
    }

    /**
     * Performs the index creation using the given Elasticsearch client
     */
    protected void doCreateIndex(RestHighLevelClient client, String indexName, boolean isAuthoring) {
        Resource settings = isAuthoring? authoringIndexSettings : previewIndexSettings;
        if(!exists(client, indexName)) {
            logger.info("Creating index {}", indexName);
            try(InputStream is = settings.getInputStream()) {
                client.indices().create(
                    new CreateIndexRequest(indexName + indexNameSuffix)
                        .source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON)
                        .alias(new Alias(indexName)),
                    RequestOptions.DEFAULT);
            } catch (Exception e) {
                throw new ElasticsearchException(indexName, "Error creating index", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndex(final String indexName) throws ElasticsearchException {
        doDeleteIndex(elasticsearchClient, indexName);
    }

    /**
     * Performs the index delete using the given Elasticsearch client
     */
    protected void doDeleteIndex(RestHighLevelClient client, String indexName) {
        try {
            GetAliasesResponse indices = client.indices().getAlias(
                new GetAliasesRequest(indexName),
                RequestOptions.DEFAULT);
            Set<String> actualIndices = indices.getAliases().keySet();
            logger.info("Deleting indices {}", actualIndices);
            client.indices().delete(
                new DeleteIndexRequest(actualIndices.toArray(new String[]{})),
                RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticsearchException(indexName, "Error deleting index " + indexName, e);
        }
    }

    @Override
    public void close() throws Exception {
        elasticsearchClient.close();
    }

}
