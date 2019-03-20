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
import org.craftercms.search.elasticsearch.ElasticSearchAdminService;
import org.craftercms.search.elasticsearch.exception.ElasticSearchException;
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
 * Default implementation of {@link ElasticSearchAdminService}
 * @author joseross
 */
public class ElasticSearchAdminServiceImpl implements ElasticSearchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchAdminServiceImpl.class);

    /**
     * Index settings file for authoring indices
     */
    protected Resource authoringIndexSettings;

    /**
     * Index settings file for preview indices
     */
    protected Resource previewIndexSettings;

    /**
     * The ElasticSearch client
     */
    protected RestHighLevelClient elasticSearchClient;

    @Required
    public void setAuthoringIndexSettings(final Resource authoringIndexSettings) {
        this.authoringIndexSettings = authoringIndexSettings;
    }

    @Required
    public void setPreviewIndexSettings(final Resource previewIndexSettings) {
        this.previewIndexSettings = previewIndexSettings;
    }

    @Required
    public void setElasticSearchClient(final RestHighLevelClient elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String indexName) throws ElasticSearchException {
        logger.debug("Checking if index {} exits", indexName);
        try {
            return elasticSearchClient.indices().exists(
                new GetIndexRequest().indices(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticSearchException(indexName, "Error consulting index", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String indexName, boolean isAuthoring) throws ElasticSearchException {
        if(isAuthoring) {
            if(!exists(indexName)) {
                logger.info("Creating index {}", indexName);
                try(InputStream is = authoringIndexSettings.getInputStream()) {
                    elasticSearchClient.indices().create(
                        new CreateIndexRequest(indexName)
                            .source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON),
                        RequestOptions.DEFAULT);
                } catch (Exception e) {
                    throw new ElasticSearchException(indexName, "Error creating index", e);
                }
            }
        } else {
            if(!exists(indexName)) {
                logger.info("Creating index {}", indexName);
                try(InputStream is = previewIndexSettings.getInputStream()) {
                    elasticSearchClient.indices().create(
                        new CreateIndexRequest(indexName)
                            .source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON),
                        RequestOptions.DEFAULT);
                } catch (Exception e) {
                    throw new ElasticSearchException(indexName, "Error creating index", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndex(final String indexName) throws ElasticSearchException {
        String[] name = new String[]{ indexName };
        try {
            logger.info("Deleting index {}", indexName);
            elasticSearchClient.indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticSearchException(indexName, "Error deleting index", e);
        }
    }

}
