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

package org.craftercms.search.opensearch.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.Content;
import org.craftercms.search.commons.utils.ContentResource;
import org.craftercms.search.opensearch.DocumentParser;
import org.craftercms.search.opensearch.OpenSearchService;
import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;

/**
 * Default implementation of {@link OpenSearchService}
 *
 * @author joseross
 */
public class OpenSearchServiceImpl implements OpenSearchService {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchServiceImpl.class);

    public static final String DEFAULT_LOCAL_ID_NAME = "localId";

    public static final int DEFAULT_SCROLL_SIZE = 100;

    public static final String DEFAULT_SCROLL_TIMEOUT = "1m";

    /**
     * Document Builder
     */
    protected final OpenSearchDocumentBuilder documentBuilder;

    /**
     * Document Parser
     */
    protected final DocumentParser documentParser;

    /**
     * The OpenSearch client
     */
    protected final OpenSearchClient openSearchClient;

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

    @ConstructorProperties({"documentBuilder", "documentParser", "OpenSearchClient"})
    public OpenSearchServiceImpl(final OpenSearchDocumentBuilder documentBuilder,
                                 final DocumentParser documentParser,
                                 final OpenSearchClient openSearchClient) {
        this.documentBuilder = documentBuilder;
        this.documentParser = documentParser;
        this.openSearchClient = openSearchClient;
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
            throws OpenSearchException {
        logger.debug("[{}] Search values for field {} (query -> {})", aliasName, field, query);

        List<String> ids = new LinkedList<>();
        String scrollId = null;

        try {
            logger.debug("[{}] Opening scroll with timeout {}", aliasName, scrollTimeout);
            SearchResponse<Map> response = openSearchClient.search(r -> r
                            .index(aliasName + "*")
                            .scroll(s -> s.time(scrollTimeout))
                            .from(0)
                            .size(scrollSize)
                            .query(query),
                    Map.class
            );
            String innerScrollId = response.scrollId();
            scrollId = innerScrollId;

            while (response.hits().hits().size() > 0) {
                response.hits().hits().forEach(hit -> ids.add((String) hit.source().get(field)));

                logger.debug("[{}] Getting next batch for scroll with id {}", aliasName, innerScrollId);
                response = openSearchClient.scroll(s -> s
                                .scrollId(innerScrollId)
                                .scroll(t -> t.time(scrollTimeout)),
                        Map.class
                );
            }
        } catch (Exception e) {
            throw new OpenSearchException(aliasName, "Error executing search for query " + query, e);
        } finally {
            if (StringUtils.isNotEmpty(scrollId)) {
                String innerScrollId = scrollId;
                logger.debug("[{}] Clearing scroll with id {}", aliasName, innerScrollId);
                try {
                    openSearchClient.clearScroll(r -> r.scrollId(innerScrollId));
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
            SearchResponse<Map> response = openSearchClient.search(r -> r
                            .index(aliasName + "*")
                            .query(q -> q
                                    .term(t -> t
                                            .field(localIdFieldName)
                                            .value(v -> v.stringValue(docId))
                                    )
                            ),
                    Map.class
            );
            if (response.hits().total().value() > 0) {
                return response.hits().hits().get(0).source();
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new OpenSearchException(aliasName, "Error executing search for id " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final Map<String, Object> doc) {
        doIndex(openSearchClient, indexName, siteName, docId, doc);
    }

    /**
     * Performs the index operation using the given OpenSearch client
     */
    protected void doIndex(OpenSearchClient client, String indexName, String siteName, String docId,
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
            throw new OpenSearchException(indexName, "Error indexing document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final String xml,
                      final Map<String, Object> additionalFields) throws OpenSearchException {
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
            throws OpenSearchException {
        String filename = FilenameUtils.getName(path);
        try {
            index(indexName, siteName, path, documentParser.parseToXml(filename, new ContentResource(content,
                    filename), additionalFields));
        } catch (Exception e) {
            throw new OpenSearchException(indexName, "Error indexing binary document " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexBinary(final String indexName, final String siteName, final String path,
                            final Resource resource, final Map<String, Object> additionalFields)
            throws OpenSearchException {
        String filename = FilenameUtils.getName(path);
        try {
            index(indexName, siteName, path, documentParser.parseToXml(filename, resource, additionalFields));
        } catch (Exception e) {
            throw new OpenSearchException(indexName, "Error indexing binary document " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String indexName, final String siteName, final String docId)
            throws OpenSearchException {
        doDelete(openSearchClient, indexName, siteName, docId);
    }

    /**
     * Performs the delete operation using the given OpenSearch client
     */
    protected void doDelete(OpenSearchClient client, String indexName, String siteName, String docId) {
        logger.debug("[{}] Deleting document {}", indexName, docId);
        try {
            client.delete(r -> r
                    .index(indexName)
                    .id(getId(docId))
            );
        } catch (Exception e) {
            throw new OpenSearchException(indexName, "Error deleting document " + docId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final String indexName) throws OpenSearchException {
        doRefresh(openSearchClient, indexName);
    }

    /**
     * Performs the refresh operation using the given OpenSearch client
     */
    protected void doRefresh(OpenSearchClient client, String indexName) throws OpenSearchException {
        logger.debug("[{}] Refreshing index", indexName);
        try {
            client.indices().refresh(r -> r
                    .index(indexName)
            );
        } catch (IOException e) {
            throw new OpenSearchException(indexName, "Error flushing index", e);
        }
    }

    /**
     * Hashes the full path to use as a unique id for OpenSearch
     *
     * @param path the path of the file
     * @return MD5 hash for the path
     */
    protected String getId(String path) {
        return DigestUtils.md5Hex(path);
    }

    @Override
    public void close() throws Exception {
        openSearchClient._transport().close();
    }

}
