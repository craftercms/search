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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.elasticsearch.DocumentParser;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.core.service.Content;
import org.craftercms.search.service.utils.ContentResource;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;

/**
 * Default implementation of {@link ElasticsearchService}
 * @author joseross
 */
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchServiceImpl.class);

    /**
     * According to Elasticsearch documentation this will be removed and this is the recommended value
     */
    public static final String DEFAULT_DOC = "_doc";

    public static final String DEFAULT_LOCAL_ID_NAME = "localId";

    public static final int DEFAULT_SCROLL_SIZE = 100;

    public static final String DEFAULT_SCROLL_TIMEOUT = "1m";

    /**
     * Document Builder
     */
    protected ElasticsearchDocumentBuilder documentBuilder;

    /**
     * Document Parser
     */
    protected DocumentParser documentParser;

    /**
     * The Elasticsearch client
     */
    protected RestHighLevelClient client;

    /**
     * The name of the field for full ids
     */
    protected String localIdFieldName = DEFAULT_LOCAL_ID_NAME;

    /**
     * The number of results to return for each scroll request
     */
    protected int scrollSize = DEFAULT_SCROLL_SIZE;

    /**
     * The timeout for each the scroll request
     */
    protected String scrollTimeout = DEFAULT_SCROLL_TIMEOUT;

    @Required
    public void setDocumentBuilder(final ElasticsearchDocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    @Required
    public void setDocumentParser(final DocumentParser documentParser) {
        this.documentParser = documentParser;
    }

    @Required
    public void setClient(final RestHighLevelClient client) {
        this.client = client;
    }

    public void setLocalIdFieldName(final String localIdFieldName) {
        this.localIdFieldName = localIdFieldName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> searchField(final String indexName, final String field, final QueryBuilder queryBuilder)
        throws ElasticsearchException {
        logger.debug("[{}] Search values for field {} (query -> {})", indexName, field, queryBuilder);

        List<String> ids = new LinkedList<>();
        String scrollId = null;
        SearchRequest request = new SearchRequest(indexName).scroll(scrollTimeout).source(
            new SearchSourceBuilder()
                .fetchSource(field, null)
                .from(0)
                .size(scrollSize)
                .query(queryBuilder)
        );

        try {
            logger.debug("[{}] Opening scroll with timeout {}", indexName, scrollTimeout);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            scrollId = response.getScrollId();

            while(response.getHits().getHits().length > 0) {
                response.getHits().forEach(hit -> ids.add((String) hit.getSourceAsMap().get(localIdFieldName)));

                logger.debug("[{}] Getting next batch for scroll with id {}", indexName, scrollId);
                SearchScrollRequest scrollRequest = new SearchScrollRequest()
                    .scroll(scrollTimeout)
                    .scrollId(scrollId);
                response = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error executing search " + request, e);
        } finally {
            if (StringUtils.isNotEmpty(scrollId)) {
                logger.debug("[{}] Clearing scroll with id {}", indexName, scrollId);
                ClearScrollRequest clearRequest = new ClearScrollRequest();
                clearRequest.addScrollId(scrollId);
                try {
                    client.clearScroll(clearRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    logger.error("[{}] Error clearing scroll with id {}", indexName, scrollId, e);
                }
            }
        }

        return ids;
    }

    @Override
    public Map<String, Object> searchId(final String indexName, final String docId) {
        logger.debug("[{}] Search for id {}", indexName, docId);
        SearchRequest request = new SearchRequest(indexName).source(
            new SearchSourceBuilder()
                .query(QueryBuilders.termQuery(localIdFieldName, docId))
        );

        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            if(response.getHits().totalHits > 0) {
                return response.getHits().getHits()[0].getSourceAsMap();
            } else {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error executing search " + request, e);
        }
    }

    @Override
    public void index(final String indexName, final String siteName, final String docId, final Map<String, Object> doc) {
        try {
            delete(indexName, siteName, docId);
            logger.debug("[{}] Indexing document {}", indexName, docId);
            client.index(new IndexRequest(indexName, DEFAULT_DOC, getId(docId)).source(doc), RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error indexing document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final String xml,
                      final MultiValueMap<String, String> additionalFields) throws ElasticsearchException {
        Map<String, Object> doc = documentBuilder.build(siteName, docId, xml, true);
        if(MapUtils.isNotEmpty(additionalFields)) {
            additionalFields.forEach((key, value) -> {
                if(value.size() == 1) {
                    doc.put(key, value.get(0));
                } else {
                    doc.put(key, value);
                }
            });
        }
        index(indexName, siteName, docId, doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexBinary(final String indexName, final String siteName, final String path,
                            MultiValueMap<String, String> additionalFields, final Content content)
        throws ElasticsearchException {
        String filename = FilenameUtils.getName(path);
        try {
            index(indexName, siteName, path, documentParser.parseToXml(filename, new ContentResource(content,
                    filename), additionalFields));
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error indexing binary document " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexBinary(final String indexName, final String siteName, final String path,
                            MultiValueMap<String, String> additionalFields, final Resource resource)
        throws ElasticsearchException {
        String filename = FilenameUtils.getName(path);
        try {
            index(indexName, siteName, path, documentParser.parseToXml(filename, resource, additionalFields));
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error indexing binary document " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String indexName, final String siteName, final String docId)
        throws ElasticsearchException {
        logger.debug("[{}] Deleting document {}", indexName, docId);
        try {
            client.delete(new DeleteRequest(indexName, DEFAULT_DOC, getId(docId)), RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error deleting document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final String indexName) throws ElasticsearchException {
        logger.debug("[{}] Refreshing index", indexName);
        try {
            client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticsearchException(indexName, "Error flushing index", e);
        }
    }

    /**
     * Hashes the full path to use as a unique id for Elasticsearch
     * @param path the path of the file
     * @return MD5 hash for the path
     */
    protected String getId(String path) {
        return DigestUtils.md5Hex(path);
    }

}
