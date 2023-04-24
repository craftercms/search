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

package org.craftercms.search.opensearch.impl.client;

import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.search.opensearch.client.OpenSearchClientWrapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.util.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

/**
 * Base implementation of {@link OpenSearchClientWrapper}
 * @author joseross
 * @since 4.0.0
 */
public abstract class AbstractOpenSearchClientWrapper implements OpenSearchClientWrapper {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_NAME_INDEX = "index";
    public static final String PARAM_NAME_SEARCH_TYPE = "search_type";

    /**
     * The OpenSearch client
     */
    protected final OpenSearchClient client;

    /**
     * The filter queries to apply to all searches
     */
    protected String[] filterQueries;

    public AbstractOpenSearchClientWrapper(OpenSearchClient client) {
        this.client = client;
    }

    public void setFilterQueries(final String[] filterQueries) {
        this.filterQueries = filterQueries;
    }

    @Override
    public <T> SearchResponse<T> search(SearchRequest request, Class<T> docClass, Map<String, Object> parameters)
            throws IOException {
        return client.search(new SearchRequestWrapper(request, parameters).build(), docClass);
    }

    //TODO: Figure out the right order
    protected RequestUpdates getRequestUpdates(SearchRequest request, Map<String, Object> parameters) {
        RequestUpdates updates = new RequestUpdates();
        updateIndicesOptions(request, parameters, updates);
        updateQuery(request, parameters, updates);
        updateIndex(request, parameters, updates);
        updateSearchType(request, parameters, updates);
        return updates;
    }

    protected void updateIndex(SearchRequest request, Map<String, Object> parameters, RequestUpdates updates) {
        if (isNotEmpty(parameters) && parameters.containsKey(PARAM_NAME_INDEX)) {
            updates.index = Stream.of(parameters.get(PARAM_NAME_INDEX).toString().split(","))
                                .collect(toList());
        }
    }

    protected void updateSearchType(SearchRequest request, Map<String, Object> parameters, RequestUpdates updates) {
        if (isNotEmpty(parameters) && parameters.containsKey(PARAM_NAME_SEARCH_TYPE)) {
            updates.searchType = SearchType._DESERIALIZER.parse(parameters.get(PARAM_NAME_SEARCH_TYPE).toString());
        }
    }

    protected void updateIndicesOptions(SearchRequest request, Map<String, Object> parameters, RequestUpdates updates) {
        if (isNotEmpty(parameters) && parameters.containsKey("ignore_unavailable")) {
            updates.ignoreUnavailable = Boolean.parseBoolean(parameters.get("ignore_unavailable").toString());
        }
    }

    protected void copyQuery(BoolQuery originalQuery, BoolQuery.Builder builder) {
        builder
            .must(originalQuery.must())
            .should(originalQuery.should())
            .filter(originalQuery.filter())
            .mustNot(originalQuery.mustNot())
            .minimumShouldMatch(originalQuery.minimumShouldMatch());
    }

    /**
     * Updates the filter queries for the given request
     * @param request the request to update
     * @param updates the request updates
     */
    protected void updateQuery(SearchRequest request, Map<String, Object> parameters, RequestUpdates updates) {
        if(ArrayUtils.isEmpty(filterQueries)) {
            logger.debug("No additional filter queries configured");
            return;
        }

        Query originalQuery = request.query();
        BoolQuery.Builder builder = new BoolQuery.Builder();
        if (originalQuery != null) {
            if (originalQuery.isBool()) {
                // copy the original query
                copyQuery(originalQuery.bool(), builder);
            } else {
                // wrap the original query
                builder.must(originalQuery);
            }
        }

        for(String filterQuery : filterQueries) {
            logger.debug("Adding filter query: {}", filterQuery);
            builder.filter(f -> f
                .queryString(q -> q
                    .query(filterQuery)
                )
            );
        }

        updates.query = Query.of(q -> q
            .bool(builder.build())
        );
    }

    public static class RequestUpdates {

        protected List<String> index;

        protected List<Map<String, Double>> indicesBoost;

        protected Query query;

        protected SearchType searchType;

        protected Boolean ignoreUnavailable;

        public List<String> getIndex() {
            return index;
        }

        public void setIndex(List<String> index) {
            this.index = index;
        }

        public List<Map<String, Double>> getIndicesBoost() {
            return indicesBoost;
        }

        public void setIndicesBoost(List<Map<String, Double>> indicesBoost) {
            this.indicesBoost = indicesBoost;
        }

        public Query getQuery() {
            return query;
        }

        public void setQuery(Query query) {
            this.query = query;
        }

        public void setQuery(Function<Query.Builder, ObjectBuilder<Query>> fn) {
            this.query = fn.apply(new Query.Builder()).build();
        }

        public SearchType getSearchType() {
            return searchType;
        }

        public void setSearchType(SearchType searchType) {
            this.searchType = searchType;
        }

        public Boolean isIgnoreUnavailable() {
            return ignoreUnavailable;
        }

        public void setIgnoreUnavailable(Boolean ignoreUnavailable) {
            this.ignoreUnavailable = ignoreUnavailable;
        }

    }

    public class SearchRequestWrapper extends SearchRequest.Builder {

        public SearchRequestWrapper(SearchRequest request, Map<String, Object> parameters) {
            // make a copy of the original request
            source(request.source());
            aggregations(request.aggregations());
            allowNoIndices(request.allowNoIndices());
            allowPartialSearchResults(request.allowPartialSearchResults());
            analyzeWildcard(request.analyzeWildcard());
            analyzer(request.analyzer());
            batchedReduceSize(request.batchedReduceSize());
            ccsMinimizeRoundtrips(request.ccsMinimizeRoundtrips());
            collapse(request.collapse());
            defaultOperator(request.defaultOperator());
            df(request.df());
            docvalueFields(request.docvalueFields());
            expandWildcards(request.expandWildcards());
            explain(request.explain());
            fields(request.fields());
            from(request.from());
            highlight(request.highlight());
            ignoreThrottled(request.ignoreThrottled());
            lenient(request.lenient());
            maxConcurrentShardRequests(request.maxConcurrentShardRequests());
            minCompatibleShardNode(request.minCompatibleShardNode());
            minScore(request.minScore());
            postFilter(request.postFilter());
            preFilterShardSize(request.preFilterShardSize());
            preference(request.preference());
            profile(request.profile());
            q(request.q());
            requestCache(request.requestCache());
            rescore(request.rescore());
            routing(request.routing());
            runtimeMappings(request.runtimeMappings());
            scriptFields(request.scriptFields());
            scroll(request.scroll());
            searchAfter(request.searchAfter());
            seqNoPrimaryTerm(request.seqNoPrimaryTerm());
            size(request.size());
            slice(request.slice());
            sort(request.sort());
            stats(request.stats());
            storedFields(request.storedFields());
            suggest(request.suggest());
            terminateAfter(request.terminateAfter());
            timeout(request.timeout());
            trackScores(request.trackScores());
            trackTotalHits(request.trackTotalHits());
            version(request.version());

            // override values
            RequestUpdates updates = getRequestUpdates(request, parameters);
            ignoreUnavailable(Optional.ofNullable(updates.ignoreUnavailable).orElse(request.ignoreUnavailable()));
            index(Optional.ofNullable(updates.index).orElse(request.index()));
            indicesBoost(Optional.ofNullable(updates.indicesBoost).orElse(request.indicesBoost()));
            query(Optional.ofNullable(updates.query).orElse(request.query()));
            searchType(Optional.ofNullable(updates.searchType).orElse(request.searchType()));
        }

    }

}
