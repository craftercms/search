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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.craftercms.search.elasticsearch.ElasticsearchWrapper;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base implementation of {@link ElasticsearchWrapper}
 * @author joseross
 */
public abstract class AbstractElasticsearchWrapper implements ElasticsearchWrapper, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchWrapper.class);

    /**
     * The Elasticsearch client
     */
    protected RestHighLevelClient client;

    /**
     * The server urls for Elasticsearch
     */
    protected String[] serverUrls;

    /**
     * The username for Elasticsearch
     */
    protected String username;

    /**
     * The password for Elasticsearch
     */
    protected String password;

    /**
     * The filter queries to apply to all searches
     */
    protected String[] filterQueries;

    @Required
    public void setServerUrls(final String[] serverUrls) {
        this.serverUrls = serverUrls;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setFilterQueries(final String[] filterQueries) {
        this.filterQueries = filterQueries;
    }

    @Override
    public void afterPropertiesSet() {
        HttpHost[] hosts = Stream.of(serverUrls).map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder clientBuilder = RestClient.builder(hosts);
        if (StringUtils.isNoneEmpty(username, password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            clientBuilder
                .setHttpClientConfigCallback(builder -> builder.setDefaultCredentialsProvider(credentialsProvider));
        }
        client = new RestHighLevelClient(clientBuilder);
    }

    @Override
    public void destroy() throws Exception {
        client.close();
    }

    /**
     * Updates the value of the index for the given request
     * @param request the request to update
     */
    protected abstract void updateIndex(SearchRequest request);

    /**
     * Updates the filter queries for the given request
     * @param request the request to update
     */
    protected void updateFilters(SearchRequest request) {
        if(ArrayUtils.isEmpty(filterQueries)) {
            logger.debug("No additional filter queries configured");
            return;
        }

        BoolQueryBuilder boolQueryBuilder;
        if(request.source().query() instanceof BoolQueryBuilder) {
            boolQueryBuilder = (BoolQueryBuilder) request.source().query();
        } else {
            boolQueryBuilder = new BoolQueryBuilder().must(request.source().query());
        }

        for(String filterQuery : filterQueries) {
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
        logger.debug("Performing search for request: {}", request);
        updateIndex(request);
        updateFilters(request);
        try {
            return client.search(request, options);
        } catch (Exception e) {
            throw new ElasticsearchException(request.indices()[0], "Error executing search request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final Map<String, Object> request, final RequestOptions options) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(request);
            return search(json, options);
        } catch (IOException e) {
            throw new ElasticsearchException(null, "Error parsing request " + request, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final String request, final RequestOptions options) {
        SearchModule module = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        try {
            SearchSourceBuilder builder =
                SearchSourceBuilder.fromXContent(XContentFactory.xContent(XContentType.JSON)
                    .createParser(new NamedXContentRegistry(module.getNamedXContents()),
                        DeprecationHandler.THROW_UNSUPPORTED_OPERATION, request));
            return search(new SearchRequest().source(builder), options);
        } catch (IOException e) {
            throw new ElasticsearchException(null, "Error parsing request " + request, e);
        }
    }

}
