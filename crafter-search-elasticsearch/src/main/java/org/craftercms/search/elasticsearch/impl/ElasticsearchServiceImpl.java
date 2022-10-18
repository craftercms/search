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

package org.craftercms.search.elasticsearch.impl;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.elasticsearch.DocumentParser;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.core.service.Content;
import org.craftercms.search.commons.utils.ContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;

/**
 * Default implementation of {@link ElasticsearchService}
 * @author joseross
 */
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchServiceImpl.class);

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
    protected ElasticsearchClient elasticsearchClient;

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

    @ConstructorProperties({"documentBuilder", "documentParser", "elasticsearchClient"})
    public ElasticsearchServiceImpl(final ElasticsearchDocumentBuilder documentBuilder,
                                    final DocumentParser documentParser,
                                    final ElasticsearchClient elasticsearchClient) {
        this.documentBuilder = documentBuilder;
        this.documentParser = documentParser;
        this.elasticsearchClient = elasticsearchClient;
    }

    public void setLocalIdFieldName(final String localIdFieldName) {
        this.localIdFieldName = localIdFieldName;
    }

    public void setScrollSize(final int scrollSize) {
        this.scrollSize = scrollSize;
    }

    public void setScrollTimeout(final String scrollTimeout) {
        this.scrollTimeout = scrollTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List<String> searchField(final String aliasName, final String field, final Query query)
        throws ElasticsearchException {
        logger.debug("[{}] Search values for field {} (query -> {})", aliasName, field, query);

        List<String> ids = new LinkedList<>();
        String scrollId = null;

        try {
            logger.debug("[{}] Opening scroll with timeout {}", aliasName, scrollTimeout);
            SearchResponse<Map> response = elasticsearchClient.search(r -> r
                .index(aliasName + "*")
                .scroll(s -> s.time(scrollTimeout))
                .from(0)
                .size(scrollSize)
                .query(query),
                Map.class
            );
            String innerScrollId = response.scrollId();
            scrollId = innerScrollId;

            while(response.hits().hits().size() > 0) {
                response.hits().hits().forEach(hit -> ids.add((String) hit.source().get(field)));

                logger.debug("[{}] Getting next batch for scroll with id {}", aliasName, innerScrollId);
                response = elasticsearchClient.scroll(s -> s
                    .scrollId(innerScrollId)
                    .scroll(t -> t.time(scrollTimeout)),
                    Map.class
                );
            }
        } catch (Exception e) {
            throw new ElasticsearchException(aliasName, "Error executing search for query " + query, e);
        } finally {
            if (StringUtils.isNotEmpty(scrollId)) {
                String innerScrollId = scrollId;
                logger.debug("[{}] Clearing scroll with id {}", aliasName, innerScrollId);
                try {
                    elasticsearchClient.clearScroll(r -> r.scrollId(innerScrollId));
                } catch (IOException e) {
                    logger.error("[{}] Error clearing scroll with id {}", aliasName, innerScrollId, e);
                }
            }
        }

        return ids;
    }

    @Override
    @SuppressWarnings("rawtypes,unchecked")
    public Map<String, Object> searchId(final String aliasName, final String docId) {
        logger.debug("[{}] Search for id {}", aliasName, docId);
        try {
            SearchResponse<Map> response = elasticsearchClient.search(r -> r
                .index(aliasName + "*")
                .query(q -> q
                    .term(t -> t
                        .field(localIdFieldName)
                        .value(v -> v.stringValue(docId))
                    )
                ),
                Map.class
            );
            if(response.hits().total().value() > 0) {
                return response.hits().hits().get(0).source();
            } else {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            throw new ElasticsearchException(aliasName, "Error executing search for id " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final Map<String, Object> doc) {
        doIndex(elasticsearchClient, indexName, siteName, docId, doc);
    }

    /**
     * Performs the index operation using the given Elasticsearch client
     */
    protected void doIndex(ElasticsearchClient client, String indexName, String siteName, String docId,
                           Map<String, Object> doc) {
        try {
            doDelete(client, indexName, siteName, docId);
            logger.debug("[{}] Indexing document {}", indexName, docId);
            client.index(r -> r
                    .index(indexName)
                    .id(getId(docId))
                    .document(doc)
            );
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error indexing document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final String xml,
                      final Map<String, Object> additionalFields) throws ElasticsearchException {
        Map<String, Object> doc = documentBuilder.build(siteName, docId, xml, true);
        Map<String, Object> mergedDoc = mergeMaps(doc, additionalFields);
        index(indexName, siteName, docId, mergedDoc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexBinary(final String indexName, final String siteName, final String path,
                            final Content content, final Map<String, Object> additionalFields)
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
                            final Resource resource, final Map<String, Object> additionalFields)
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
        doDelete(elasticsearchClient, indexName, siteName, docId);
    }

    /**
     * Performs the delete operation using the given Elasticsearch client
     */
    protected void doDelete(ElasticsearchClient client, String indexName, String siteName, String docId) {
        logger.debug("[{}] Deleting document {}", indexName, docId);
        try {
            client.delete(r -> r
                .index(indexName)
                .id(getId(docId))
            );
        } catch (Exception e) {
            throw new ElasticsearchException(indexName, "Error deleting document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final String indexName) throws ElasticsearchException {
        doRefresh(elasticsearchClient, indexName);
    }

    /**
     * Performs the refresh operation using the given Elasticsearch client
     */
    protected void doRefresh(ElasticsearchClient client, String indexName) throws ElasticsearchException {
        logger.debug("[{}] Refreshing index", indexName);
        try {
            client.indices().refresh(r -> r
                .index(indexName)
            );
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

    @Override
    public void close() throws Exception {
        elasticsearchClient._transport().close();
    }

}
