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
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SpatialParams;
import org.apache.solr.spelling.suggest.SuggesterParams;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.v3.model.facet.Facet;
import org.craftercms.search.rest.v3.requests.FacetRequest;
import org.craftercms.search.v3.model.facet.FacetValue;
import org.craftercms.search.v3.model.geo.DistanceFilter;
import org.craftercms.search.rest.v3.requests.LocationRequest;
import org.craftercms.search.v3.model.geo.RegionFilter;
import org.craftercms.search.v3.model.highlight.Highlight;
import org.craftercms.search.v3.model.highlight.HighlightField;
import org.craftercms.search.rest.v3.requests.HighlightRequest;
import org.craftercms.search.v3.model.sort.FieldSort;
import org.craftercms.search.rest.v3.requests.SortRequest;
import org.craftercms.search.rest.v3.requests.SuggestRequest;
import org.craftercms.search.v3.model.suggest.Suggestion;
import org.craftercms.search.v3.service.internal.SearchMapper;

import static org.craftercms.search.service.impl.SolrUtils.extractDocs;

/**
 * Implementation of {@link SearchMapper} for Solr documents
 * @author joseross
 */
public class SolrSearchMapper implements SearchMapper<SolrQuery, QueryResponse> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQuery buildRequest(final SearchRequest request) {
        SolrQuery query = new SolrQuery();
        query.setQuery(request.getMainQuery());
        query.setFilterQueries(request.getFilterQueries().toArray(new String[0]));
        query.setFields(request.getFields());
        query.setStart(request.getOffset());
        query.setRows(request.getLimit());

        return query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapSortRequest(final SortRequest sortRequest, final SolrQuery query) {
        List<FieldSort> fields = sortRequest.getFields();
        if(CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(field -> query.addOrUpdateSort(field.getName(),
                SolrQuery.ORDER.valueOf(field.getOrder().toString().toLowerCase())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapFacetRequest(final FacetRequest facetRequest, final SolrQuery query) {
        query.addFacetField(facetRequest.getFields());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapHighlightRequest(final HighlightRequest highlightRequest, final SolrQuery query) {
        query.addHighlightField(StringUtils.join(highlightRequest.getFields(), ","));
        query.setHighlightSnippets(highlightRequest.getMaxFragments());
        query.setHighlightFragsize(highlightRequest.getFragmentSize());
        query.setHighlightSimplePre(highlightRequest.getPrefix());
        query.setHighlightSimplePost(highlightRequest.getPostfix());
        if(StringUtils.isNotEmpty(highlightRequest.getQuery())) {
            query.set(HighlightParams.Q, highlightRequest.getQuery());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapSuggestRequest(final SuggestRequest suggestRequest, final SolrQuery query) {
        query.set("suggest", true);
        query.set(SuggesterParams.SUGGEST_COUNT, suggestRequest.getMax());
        if(StringUtils.isNotEmpty(suggestRequest.getName())) {
            query.set(SuggesterParams.SUGGEST_DICT, suggestRequest.getName());
        }
        if(StringUtils.isNotEmpty(suggestRequest.getQuery())) {
            query.set(SuggesterParams.SUGGEST_Q, suggestRequest.getQuery());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapLocationRequest(final LocationRequest locationRequest, final SolrQuery query) {
        List<DistanceFilter> distances = locationRequest.getDistances();
        if(CollectionUtils.isNotEmpty(distances)) {
            distances.forEach(distance -> {
                String parser = distance.isSquare()? "bbox" : "geofilt";
                query.addFilterQuery(String.format("{!%s %s=%s %s=%s %s=%s,%s}", parser, SpatialParams.FIELD,
                    distance.getField(), SpatialParams.DISTANCE, distance.getDistance(), SpatialParams.POINT,
                    distance.getPoint().getLatitude(), distance.getPoint().getLongitude()));
            });
        }
        List<RegionFilter> regions = locationRequest.getRegions();
        if(CollectionUtils.isNotEmpty(regions)) {
            regions.forEach(region -> query.addFilterQuery(String.format("%s:[%s,%s TO %s,%s]", region.getField(),
                region.getBottomLeft().getLatitude(), region.getBottomLeft().getLongitude(),
                region.getTopRight().getLatitude(), region.getTopRight().getLongitude())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long extractTotal(final QueryResponse result) {
        return result.getResults().getNumFound();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> extractItems(final QueryResponse result) {
        return (List<Map<String, Object>>) extractDocs(result.getResults());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Facet> extractFacets(final QueryResponse response) {
        List<Facet> facets = new LinkedList<>();
        List<FacetField> fields = response.getFacetFields();
        if(CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(field -> {
                List<FacetValue> values = new LinkedList<>();
                field.getValues().forEach(value -> values.add(new FacetValue(value.getName(), value.getCount())));
                facets.add(new Facet(field.getName(), values));
            });
        }
        return facets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Highlight> extractHighlights(final QueryResponse response) {
        List<Highlight> highlights = new LinkedList<>();
        Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
        if(highlighting != null && highlighting.size() > 0) {
            highlighting.forEach((id, values) -> {
                List<HighlightField> fields = new LinkedList<>();
                values.forEach((name, fragments) -> fields.add(new HighlightField(name, fragments)));
                highlights.add(new Highlight(id, fields));
            });
        }
        return highlights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Suggestion> extractSuggestions(final QueryResponse response) {
        List<Suggestion> suggestions = new LinkedList<>();
        SuggesterResponse suggester = response.getSuggesterResponse();
        if(suggester != null) {
            Map<String, List<String>> terms = suggester.getSuggestedTerms();
            if(terms != null) {
                terms.forEach((term, suggested) -> suggestions.add(new Suggestion(term, suggested)));
            }
        }
        return suggestions;
    }

}
