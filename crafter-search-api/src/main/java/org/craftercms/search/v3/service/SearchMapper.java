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

package org.craftercms.search.v3.service;

import java.util.List;
import java.util.Map;

import org.craftercms.search.v3.model.facet.Facet;
import org.craftercms.search.rest.v3.requests.FacetRequest;
import org.craftercms.search.rest.v3.requests.LocationRequest;
import org.craftercms.search.v3.model.highlight.Highlight;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.rest.v3.requests.HighlightRequest;
import org.craftercms.search.rest.v3.requests.SortRequest;
import org.craftercms.search.rest.v3.requests.SuggestRequest;
import org.craftercms.search.v3.model.suggest.Suggestion;

/**
 * Performs the mapping between the search provider's native objects and instances of {@link SearchRequest} &
 * {@link SearchResponse}
 * @param <I> type for the native search requests
 * @param <O> type for the native search responses
 * @author joseross
 */
public interface SearchMapper<I, O> {

    /**
     * Prepares a new instance of the native request
     * @param request the search request to map
     * @return the native search request
     */
    I buildRequest(final SearchRequest request);

    /**
     * Performs all mapping operations for the given request
     * @param request the search request to map
     * @return the native search request
     */
    default I mapRequest(final SearchRequest request) {
        I nativeRequest = buildRequest(request);

        SortRequest sortRequest = request.getSort();
        if(sortRequest != null) {
            mapSortRequest(sortRequest, nativeRequest);
        }

        FacetRequest facetRequest = request.getFacets();
        if(facetRequest != null) {
            mapFacetRequest(facetRequest, nativeRequest);
        }

        HighlightRequest highlightRequest = request.getHighlights();
        if(highlightRequest != null) {
            mapHighlightRequest(highlightRequest, nativeRequest);
        }

        SuggestRequest suggestRequest = request.getSuggestions();
        if(suggestRequest != null) {
            mapSuggestRequest(suggestRequest, nativeRequest);
        }

        LocationRequest locationRequest = request.getLocations();
        if(locationRequest != null) {
            mapLocationRequest(locationRequest, nativeRequest);
        }

        return nativeRequest;
    }

    /**
     * Performs the mapping for a {@link SortRequest}
     * @param sortRequest the sort request
     * @param request the native search request
     */
    void mapSortRequest(final SortRequest sortRequest, I request);

    /**
     * Performs the mapping for a {@link FacetRequest}
     * @param facetRequest the facet request
     * @param request the native search request
     */
    void mapFacetRequest(final FacetRequest facetRequest, I request);

    /**
     * Performs the mapping for a {@link HighlightRequest}
     * @param highlightRequest the highlight request
     * @param request the native search request
     */
    void mapHighlightRequest(final HighlightRequest highlightRequest, I request);

    /**
     * Performs the mapping for a {@link SuggestRequest}
     * @param suggestRequest the suggest request
     * @param request the native search request
     */
    void mapSuggestRequest(final SuggestRequest suggestRequest, I request);

    /**
     * Performs the mapping for a {@link LocationRequest}
     * @param locationRequest the location request
     * @param request the native search request
     */
    void mapLocationRequest(final LocationRequest locationRequest, I request);

    /**
     * Performs all mapping operations for the given response
     * @param result the native search response
     * @param response the search response
     */
    default void mapResponse(final O result, SearchResponse response) {
        response.setTotal(extractTotal(result));
        response.setItems(extractItems(result));
        response.setFacets(extractFacets(result));
        response.setHighlights(extractHighlights(result));
        response.setSuggestions(extractSuggestions(result));
    }

    /**
     * Extracts the total number of documents matched
     * @param result the native search response
     * @return the total number of documents matched
     */
    long extractTotal(final O result);

    /**
     * Extracts the documents from the given native search response
     * @param result the native search response
     * @return the list of matched documents
     */
    List<Map<String, Object>> extractItems(final O result);

    /**
     * Extracts the facets from the given native search response
     * @param result the native search response
     * @return the list of facets
     */
    List<Facet> extractFacets(final O result);

    /**
     * Extracts the highlights from the given native search response
     * @param result the native search response
     * @return the list of highlights
     */
    List<Highlight> extractHighlights(final O result);

    /**
     * Extracts the suggestions from the given native search response
     * @param result the native search response
     * @return the list of suggestions
     */
    List<Suggestion> extractSuggestions(final O result);

}
