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
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Default implementation of {@link ElasticsearchAdminService}
 * @author joseross
 * @since 3.1.0
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
    public void createIndex(final String aliasName, boolean isAuthoring) throws ElasticsearchException {
        doCreateIndexAndAlias(elasticsearchClient, aliasName, isAuthoring);
    }

    /**
     * Performs the index creation using the given Elasticsearch client
     */
    protected void doCreateIndexAndAlias(RestHighLevelClient client, String aliasName, boolean isAuthoring) {
        doCreateIndexAndAlias(client, aliasName, indexNameSuffix, isAuthoring);
    }

    /**
     * Performs the index creation using the given Elasticsearch client
     */
    protected void doCreateIndexAndAlias(RestHighLevelClient client, String aliasName, String indexSuffix,
                                         boolean isAuthoring) {
        Resource settings = isAuthoring? authoringIndexSettings : previewIndexSettings;
        if(!exists(client, aliasName)) {
            logger.info("Creating index and alias {}", aliasName);
            try(InputStream is = settings.getInputStream()) {
                client.indices().create(
                    new CreateIndexRequest(aliasName + indexSuffix)
                        .source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON)
                        .alias(new Alias(aliasName)),
                    RequestOptions.DEFAULT);
            } catch (Exception e) {
                throw new ElasticsearchException(aliasName, "Error creating index " + aliasName, e);
            }
        }
    }

    /***
     * Performs the index creation without any alias association
     */
    protected void doCreateIndex(RestHighLevelClient client, String aliasName, String indexSuffix,
                                 boolean isAuthoring) {
        logger.info("Creating index {}", aliasName + indexSuffix);
        Resource settings = isAuthoring? authoringIndexSettings : previewIndexSettings;
        try(InputStream is = settings.getInputStream()) {
            client.indices().create(
                    new CreateIndexRequest(aliasName + indexSuffix)
                            .source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON),
                    RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticsearchException(aliasName, "Error creating index " + aliasName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndexes(final String aliasName) throws ElasticsearchException {
        doDeleteIndexes(elasticsearchClient, aliasName);
    }

    /**
     * Performs the index delete using the given Elasticsearch client
     */
    protected void doDeleteIndexes(RestHighLevelClient client, String aliasName) {
        try {
            GetAliasesResponse indices = client.indices().getAlias(
                new GetAliasesRequest(aliasName),
                RequestOptions.DEFAULT);
            Set<String> actualIndices = indices.getAliases().keySet();
            logger.info("Deleting indices {}", actualIndices);
            client.indices().delete(
                new DeleteIndexRequest(actualIndices.toArray(new String[]{})),
                RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticsearchException(aliasName, "Error deleting index " + aliasName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recreateIndex(String aliasName, boolean isAuthoring) throws ElasticsearchException {
        doRecreateIndex(elasticsearchClient, aliasName, isAuthoring);
    }

    /**
     * Performs all operations for recreating an index using the given Elasticsearch client
     */
    protected void doRecreateIndex(RestHighLevelClient client, String aliasName, boolean isAuthoring) {
        logger.info("Recreating index for alias {}", aliasName);
        try {
            // get the version of the existing index
            String existingIndexName = doGetIndexName(client, aliasName);

            logger.info("Found index {} for alias {}", existingIndexName, aliasName);

            String[] tokens = existingIndexName.split("_v");
            if (tokens.length != 2) {
                throw new IllegalStateException("Could not find current version for index: " + existingIndexName);
            }
            int currentVersion = Integer.parseInt(tokens[1]);

            // create a new index
            String newVersion = "_v" + (currentVersion + 1);
            logger.debug("Using new version {} for index {}", newVersion, aliasName);
            doCreateIndex(client, aliasName, newVersion, isAuthoring);
            String newIndexName = aliasName + newVersion;

            // index all existing content into the new index
            doReindex(client, existingIndexName, newIndexName);

            // swap indexes
            doSwap(client, aliasName, existingIndexName, newIndexName);

            // delete the previous index
            doDeleteIndex(client, existingIndexName);
        } catch (Exception e) {
            throw new ElasticsearchException(aliasName, "Error upgrading index " + aliasName, e);
        }
    }

    protected String doGetIndexName(RestHighLevelClient client, String aliasName) throws IOException {
        GetAliasesResponse indices =
                client.indices().getAlias(new GetAliasesRequest(aliasName), RequestOptions.DEFAULT);
        if (indices.getAliases().size() != 1) {
            throw new IllegalStateException("None or more than one index is associated with alias: " + aliasName);
        }
        return indices.getAliases().keySet().iterator().next();
    }

    protected void doReindex(RestHighLevelClient client, String sourceIndex, String destinationIndex)
            throws IOException {
        logger.info("Reindexing all existing content from {} to {}", sourceIndex, destinationIndex);
        BulkByScrollResponse response = client.reindex(
                new ReindexRequest().setSourceIndices(sourceIndex).setDestIndex(destinationIndex),
                RequestOptions.DEFAULT);
        logger.info("Successfully indexed {} docs into {}", response.getTotal(), destinationIndex);
    }

    protected void doSwap(RestHighLevelClient client, String aliasName, String existingIndexName, String newIndexName)
            throws IOException {
        logger.info("Swapping index {} with {}", existingIndexName, newIndexName);
        client.indices().updateAliases(new IndicesAliasesRequest()
                .addAliasAction(
                        new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                                .index(newIndexName)
                                .alias(aliasName))
                .addAliasAction(
                        new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                                .index(existingIndexName)
                                .alias(aliasName)
                ), RequestOptions.DEFAULT);
    }

    protected void doDeleteIndex(RestHighLevelClient client, String indexName) throws IOException {
        logger.info("Deleting index {}", indexName);
        client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    @Override
    public void waitUntilReady() {
        doWaitUntilReady(elasticsearchClient);
    }

    protected void doWaitUntilReady(RestHighLevelClient client) {
        logger.info("Waiting for Elasticsearch cluster to be ready");
        boolean ready = false;
        do {
            try {
                ready = client.ping(RequestOptions.DEFAULT);
            } catch (IOException e) {
                logger.debug("Error pinging Elasticsearch cluster", e);
            }
            if (!ready) {
                logger.info("Elasticsearch cluster not ready, will try again in 5 seconds");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("Error waiting for Elasticsearch cluster to be ready", e);
                }
            }
        } while(!ready);
    }

    @Override
    public void close() throws Exception {
        elasticsearchClient.close();
    }

}
