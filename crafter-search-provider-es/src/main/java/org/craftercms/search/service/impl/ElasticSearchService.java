/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SearchServerException;
import org.craftercms.search.service.Query;
import org.craftercms.search.v3.service.impl.AbstractSearchService;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link org.craftercms.search.service.SearchService} for ElasticSearch
 *
 * <p><strong>Note:</strong> This service does not support search using {@link Query} objects, all related methods will
 * always throw an
 * {@link UnsupportedOperationException}</p>
 * @author joseross
 */
public class ElasticSearchService extends AbstractSearchService<Query, SearchSourceBuilder, SearchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    /**
     * Name of the type to use when indexing documents.
     * ElasticSearch recommends to use this name because in the next release types will be removed completely.
     */
    public static final String ELASTIC_DEFAULT_TYPE = "_doc";

    public static final String DEFAULT_ATTACHMENT_PIPELINE_NAME = "attachment";
    public static final String DEFAULT_ATTACHMENT_FIELD_NAME = "data";

    /**
     * Name of the pipeline to use when indexing binary files
     */
    protected String attachmentPipelineName = DEFAULT_ATTACHMENT_PIPELINE_NAME;

    /**
     * Name of the field to use when indexing binary files
     */
    protected String attachmentFieldName = DEFAULT_ATTACHMENT_FIELD_NAME;

    /**
     * ElasticSearch rest client
     */
    protected RestHighLevelClient client;

    /**
     * DocumentBuilder instance
     */
    protected ElasticDocumentBuilder documentBuilder;

    /**
     * JSON ObjectMapper
     */
    protected ObjectMapper objectMapper;

    public void setAttachmentPipelineName(final String attachmentPipelineName) {
        this.attachmentPipelineName = attachmentPipelineName;
    }

    public void setAttachmentFieldName(final String attachmentFieldName) {
        this.attachmentFieldName = attachmentFieldName;
    }

    @Required
    public void setClient(final RestHighLevelClient client) {
        this.client = client;
    }

    @Required
    public void setDocumentBuilder(final ElasticDocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    @Required
    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void destroy() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            logger.error("Error closing the client", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doUpdate(final String indexId, final String site, final String id, final String finalId,
                            final String xml, final boolean ignoreRootInFieldNames) {
        IndexRequest request = new IndexRequest(indexId, ELASTIC_DEFAULT_TYPE, finalId);
        try {
            Map map = documentBuilder.build(site, id, finalId, xml, false);
            request.source(map);
            IndexResponse response = client.index(request);
            logger.debug("Update for {} result: {}", id, response.getResult());
        } catch (IOException e) {
            logger.error("Unable to execute update for " + finalId, e);
            throw new SearchServerException(indexId, "Unable to execute update for " + finalId, e);
        } catch (Exception e) {
            logger.error("Update for " + finalId + " failed", e);
            throw new SearchException(indexId, "Update for " + finalId + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDelete(final String indexId, final String finalId, final String query) {
        DeleteRequest request = new DeleteRequest(indexId, ELASTIC_DEFAULT_TYPE, finalId);
        try {
            DeleteResponse response = client.delete(request);
            logger.debug("Delete for {} result: {}", finalId, response.getResult());
        } catch (IOException e) {
            logger.error("Unable to execute delete for " + finalId, e);
            throw new SearchServerException(indexId, "Unable to execute delete for " + finalId, e);
        } catch (Exception e) {
            logger.error("Delete for " + finalId + " failed", e);
            throw new SearchException(indexId, "Delete for " + finalId + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doUpdateContent(final String indexId, final String site, final String id, final String finalId,
                                   final File file, final String fileName, final String contentType, final Map<String
        , List<String>> additionalFields) {
        IndexRequest request = new IndexRequest(indexId, ELASTIC_DEFAULT_TYPE, finalId);

        try {
            Map<String, Object> doc = new HashMap<>();
            documentBuilder.addFields(doc, finalId, site, id, additionalFields);

            byte[] content = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(content);

            doc.put(attachmentFieldName, encoded);

            request.source(doc).setPipeline(attachmentPipelineName);

            IndexResponse response = client.index(request);
            logger.debug("Update for {} result {}", finalId, response.status());

        } catch (IOException e) {
            logger.error("Unable to execute update for " + finalId, e);
            throw new SearchServerException(indexId, "Unable to execute update for " + finalId, e);
        } catch (Exception e) {
            logger.error("Update for " + finalId + " failed", e);
            throw new SearchException(indexId, "Update for " + finalId + " failed", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doCommit(final String indexId) {
        try {
            FlushRequest request = new FlushRequest(indexId);
            FlushResponse response = client.indices().flush(request);
            logger.debug("Flush for {} result: {}", indexId, response.getSuccessfulShards());
        } catch (IOException e) {
            logger.error("Unable to execute commit for " + indexId, e);
            throw new SearchServerException(indexId, "Unable to execute commit for " + indexId, e);
        } catch (Exception e) {
            logger.error("Commit for " + indexId + " failed", e);
            throw new SearchException(indexId, "Commit for " + indexId + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SearchResponse doSearch(final String indexId, final SearchSourceBuilder builder) {
        try {
            SearchResponse response = client.search(new SearchRequest(indexId).source(builder));
            return response;
        } catch (IOException e) {
            logger.error("Unable to execute search for " + builder, e);
            throw new SearchServerException(indexId, "Unable to execute search for " + builder, e);
        } catch (Exception e) {
            logger.error("Search for " + builder + " failed", e);
            throw new SearchException(indexId, "Search for " + builder + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses the given parameters to build a native ElasticSearch request in JSON format and returns the native
     * response as a generic map object</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> nativeSearch(final String indexId, final Map<String, Object> params) {
        try {
            String json = objectMapper.writeValueAsString(params);

            // Parse json to a SearchSourceBuilder instance
            SearchModule module = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
            SearchSourceBuilder builder = SearchSourceBuilder.fromXContent(
                XContentFactory.xContent(XContentType.JSON)
                    .createParser(new NamedXContentRegistry(module.getNamedXContents()),
                                    DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                                    json));

            SearchResponse response = client.search(new SearchRequest(indexId).source(builder));
            return objectMapper.convertValue(response, Map.class);
        } catch (IOException e) {
            logger.error("Unable to execute search for " + params, e);
            throw new SearchServerException(indexId, "Unable to execute search for " + params, e);
        } catch (Exception e) {
            logger.error("Search for " + params + " failed", e);
            throw new SearchException(indexId, "Search for " + params + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createQuery() {
        throw new UnsupportedOperationException("Query objects are not supported for API 3");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createQuery(final Map<String, String[]> params) {
        throw new UnsupportedOperationException("Query objects are not supported for API 3");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAdditionalFilterQueries(final String indexId, final Query query) {
        throw new UnsupportedOperationException("Query objects are not supported for API 3");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> doSearch(final String indexId, final Query query) {
        throw new UnsupportedOperationException("Query objects are not supported for API 3");
    }

}