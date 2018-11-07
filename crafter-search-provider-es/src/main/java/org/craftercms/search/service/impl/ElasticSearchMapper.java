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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.v3.model.facet.Facet;
import org.craftercms.search.rest.v3.requests.FacetRequest;
import org.craftercms.search.v3.model.facet.FacetValue;
import org.craftercms.search.rest.v3.requests.LocationRequest;
import org.craftercms.search.v3.model.highlight.Highlight;
import org.craftercms.search.rest.v3.requests.HighlightRequest;
import org.craftercms.search.rest.v3.requests.SortRequest;
import org.craftercms.search.rest.v3.requests.SuggestRequest;
import org.craftercms.search.v3.model.suggest.Suggestion;
import org.craftercms.search.v3.service.SearchMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;

/**
 * Implementation of {@link SearchMapper} for ElasticSearch
 * @author joseross
 */
public class ElasticSearchMapper implements SearchMapper<SearchSourceBuilder, SearchResponse> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchSourceBuilder buildRequest(final SearchRequest request) {
        return new SearchSourceBuilder()
            .fetchSource(request.getFields(), null)
            .from(request.getOffset())
            .size(request.getLimit())
            .query(buildQuery(request));
    }

    protected QueryBuilder buildQuery(SearchRequest request) {
        QueryBuilder query = QueryBuilders.queryStringQuery(request.getMainQuery());

        if(CollectionUtils.isNotEmpty(request.getFilterQueries())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(query);
            request.getFilterQueries().forEach(filter -> boolQuery.filter(QueryBuilders.queryStringQuery(filter)));
            query = boolQuery;
        }

        return query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapSortRequest(final SortRequest sortRequest, final SearchSourceBuilder builder) {
        sortRequest.getFields().forEach(sort ->
            builder.sort(SortBuilders.fieldSort(sort.getName()).order(SortOrder.valueOf(sort.getOrder().name()))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapFacetRequest(final FacetRequest facetRequest, final SearchSourceBuilder builder) {
        for(String field : facetRequest.getFields()) {
            builder.aggregation(AggregationBuilders.terms(field).field(field));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapHighlightRequest(final HighlightRequest highlightRequest, final SearchSourceBuilder builder) {
        HighlightBuilder highlightBuilder = new HighlightBuilder()
            .preTags(highlightRequest.getPrefix())
            .postTags(highlightRequest.getPostfix())
            .highlightQuery(QueryBuilders.queryStringQuery(highlightRequest.getQuery()));
        for(String field : highlightRequest.getFields()) {
            highlightBuilder.field(field, highlightRequest.getFragmentSize(), highlightRequest.getMaxFragments());
        }
        builder.highlighter(highlightBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapSuggestRequest(final SuggestRequest suggestRequest, final SearchSourceBuilder builder) {
        builder.suggest(new SuggestBuilder().addSuggestion(suggestRequest.getName(),
            SuggestBuilders.termSuggestion(suggestRequest.getField())
                            .text(suggestRequest.getQuery())
                            .size(suggestRequest.getMax())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapLocationRequest(final LocationRequest locationRequest, final SearchSourceBuilder builder) {
        QueryBuilder query = builder.query();
        if(!(query instanceof BoolQueryBuilder)) {
            query = QueryBuilders.boolQuery().must(query);
        }
        BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
        if(CollectionUtils.isNotEmpty(locationRequest.getDistances())) {
            locationRequest.getDistances().forEach(distanceFilter -> boolQuery.filter(
                QueryBuilders.geoDistanceQuery(distanceFilter.getField())
                    .point(distanceFilter.getPoint().getLatitude(), distanceFilter.getPoint().getLongitude())
                    .distance(distanceFilter.getDistance(), DistanceUnit.fromString(distanceFilter.getUnits()))
                    .ignoreUnmapped(true)));
        }
        if(CollectionUtils.isNotEmpty(locationRequest.getRegions())) {
            locationRequest.getRegions().forEach(regionFilter ->
                boolQuery.filter(QueryBuilders.geoBoundingBoxQuery(regionFilter.getField())
                    .setCorners(regionFilter.getTopRight().getLatitude(), regionFilter.getBottomLeft().getLongitude(),
                        regionFilter.getBottomLeft().getLatitude(), regionFilter.getTopRight().getLongitude())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long extractTotal(final SearchResponse result) {
        return result.getHits().totalHits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> extractItems(final SearchResponse result) {
        List<Map<String, Object>> items = new LinkedList<>();
        for(SearchHit hit : result.getHits().getHits()) {
            items.add(hit.getSourceAsMap());
        }
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Facet> extractFacets(final SearchResponse result) {
        List<Facet> facets = new LinkedList<>();
        if(result.getAggregations() != null) {
            List<Aggregation> aggregations = result.getAggregations().asList();
            if (CollectionUtils.isNotEmpty(aggregations)) {
                aggregations.forEach(aggregation -> {
                    if (aggregation instanceof Terms) {
                        List<FacetValue> values = new LinkedList<>();
                        Terms terms = (Terms)aggregation;
                        terms.getBuckets().forEach(bucket ->
                            values.add(new FacetValue(bucket.getKeyAsString(), bucket.getDocCount())));
                        facets.add(new Facet(aggregation.getName(), values));
                    }
                });
            }
        }
        return facets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Highlight> extractHighlights(final SearchResponse result) {
        List<Highlight> highlights = new LinkedList<>();
        SearchHits hits = result.getHits();
        if(hits != null) {
            hits.forEach(hit -> {
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if(highlightFields != null) {
                    List<org.craftercms.search.v3.model.highlight.HighlightField> fields = new LinkedList<>();
                    highlightFields.forEach((field, values) -> {
                        List<String> fragments = new LinkedList<>();
                        for(Text text : values.getFragments()) {
                            fragments.add(text.toString());
                        }
                        fields.add(new org.craftercms.search.v3.model.highlight.HighlightField(field, fragments));
                    });
                    if(CollectionUtils.isNotEmpty(fields)) {
                        highlights.add(new Highlight(hit.getId(), fields));
                    }
                }
            });
        }
        return highlights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Suggestion> extractSuggestions(final SearchResponse result) {
        List<Suggestion> suggestions = new LinkedList<>();
        Suggest suggest = result.getSuggest();
        if(suggest != null) {
            suggest.forEach(suggestion -> {
                List<String> options = new LinkedList<>();
                suggestion.forEach(entry -> entry.forEach(option -> options.add(option.getText().toString())));
                suggestions.add(new Suggestion(suggestion.getName(), options));
            });
        }
        return suggestions;
    }

}
