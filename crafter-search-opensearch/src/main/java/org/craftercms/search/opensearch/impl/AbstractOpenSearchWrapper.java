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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.search.opensearch.OpenSearchWrapper;
import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.client.Node;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.search.SearchModule;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.opensearch.action.search.SearchRequest.DEFAULT_INDICES_OPTIONS;

/**
 * Base implementation of {@link OpenSearchWrapper}
 *
 * @author joseross
 */
public abstract class AbstractOpenSearchWrapper implements OpenSearchWrapper {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_NAME_INDEX = "index";
    public static final String PARAM_NAME_SEARCH_TYPE = "search_type";

    /**
     * The OpenSearch client
     */
    protected final RestHighLevelClient client;

    /**
     * The filter queries to apply to all searches
     */
    protected String[] filterQueries;

    public AbstractOpenSearchWrapper(final RestHighLevelClient client) {
        this.client = client;
    }

    public void setFilterQueries(final String[] filterQueries) {
        this.filterQueries = filterQueries;
    }

    /**
     * Updates the value of the index for the given request
     *
     * @param request the request to update
     */
    protected abstract void updateIndex(SearchRequest request);

    /**
     * Updates the filter queries for the given request
     *
     * @param request the request to update
     */
    protected void updateFilters(SearchRequest request) {
        if (ArrayUtils.isEmpty(filterQueries)) {
            logger.debug("No additional filter queries configured");
            return;
        }

        BoolQueryBuilder boolQueryBuilder;
        if (request.source().query() instanceof BoolQueryBuilder) {
            boolQueryBuilder = (BoolQueryBuilder) request.source().query();
        } else {
            boolQueryBuilder = new BoolQueryBuilder().must(request.source().query());
        }

        for (String filterQuery : filterQueries) {
            logger.debug("Adding filter query: {}", filterQuery);
            boolQueryBuilder.filter(new QueryStringQueryBuilder(filterQuery));
        }

        request.source().query(boolQueryBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final SearchRequest request, final RequestOptions options) {
        logger.debug("Original search request: {}", request);
        updateIndex(request);
        updateFilters(request);
        logger.debug("Updated search request: {}", request);
        if (logger.isDebugEnabled()) {
            var urls = client.getLowLevelClient().getNodes().stream()
                    .map(Node::getHost)
                    .collect(toList());
            logger.debug("Executing search request for urls {}", urls);
        }
        try {
            return client.search(request, options);
        } catch (Exception e) {
            throw new OpenSearchException(request.indices()[0], "Error executing search request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final Map<String, Object> request, final Map<String, Object> parameters,
                                 final RequestOptions options) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(request);
            return search(json, parameters, options);
        } catch (IOException e) {
            throw new OpenSearchException(null, "Error parsing request " + request, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final String request, final Map<String, Object> parameters,
                                 final RequestOptions options) {
        SearchModule module = new SearchModule(Settings.EMPTY, Collections.emptyList());
        try {
            SearchSourceBuilder builder =
                    SearchSourceBuilder.fromXContent(JsonXContent.jsonXContent
                            .createParser(new NamedXContentRegistry(module.getNamedXContents()),
                                    DeprecationHandler.THROW_UNSUPPORTED_OPERATION, request));

            SearchRequest searchRequest = new SearchRequest();
            searchRequest.source(builder);

            if (isNotEmpty(parameters)) {
                if (parameters.containsKey(PARAM_NAME_INDEX)) {
                    searchRequest.indices(parameters.get(PARAM_NAME_INDEX).toString().split(","));
                }
                searchRequest.searchType((String) parameters.get(PARAM_NAME_SEARCH_TYPE));
                searchRequest.indicesOptions(IndicesOptions.fromMap(parameters, DEFAULT_INDICES_OPTIONS));
            }

            return search(searchRequest, options);
        } catch (IOException e) {
            throw new OpenSearchException(null, "Error parsing request " + request, e);
        }
    }

}
