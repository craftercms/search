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

package org.craftercms.search.elasticsearch.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

/**
 * Default implementation of {@link ElasticsearchAdminService}
 * @author joseross
 */
public class ElasticsearchAdminServiceImpl implements ElasticsearchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAdminServiceImpl.class);

    public static final String INDEX_NAME_SUFFIX = "_v1";

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

    @Required
    public void setAuthoringIndexSettings(final Resource authoringIndexSettings) {
        this.authoringIndexSettings = authoringIndexSettings;
    }

    @Required
    public void setPreviewIndexSettings(final Resource previewIndexSettings) {
        this.previewIndexSettings = previewIndexSettings;
    }

    @Required
    public void setElasticsearchClient(final RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String indexName) throws ElasticsearchException {
        logger.debug("Checking if index {} exits", indexName);
        try {
            return elasticsearchClient.indices().exists(
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
        Resource settings = isAuthoring? authoringIndexSettings : previewIndexSettings;
        if(!exists(indexName)) {
            logger.info("Creating index {}", indexName);
            try(InputStream is = settings.getInputStream()) {
                elasticsearchClient.indices().create(
                    new CreateIndexRequest(indexName + INDEX_NAME_SUFFIX)
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
        String[] name = new String[]{ indexName };
        try {
            logger.info("Deleting index {}", indexName);
            elasticsearchClient.indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticsearchException(indexName, "Error deleting index", e);
        }
    }

}
