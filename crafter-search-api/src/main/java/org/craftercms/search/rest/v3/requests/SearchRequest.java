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

package org.craftercms.search.rest.v3.requests;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds the data needed to perform a search operation
 * @author joseross
 */
public class SearchRequest {

    /**
     * The id of the index where the search will be performed
     */
    protected String indexId;

    // Queries

    /**
     * The main query for the search
     */
    protected String mainQuery;

    /**
     * The list of queries to filter results without affecting the score of the main query
     */
    protected List<String> filterQueries = new LinkedList<>();

    /**
     * Indicates if the search service should skip its internal filters
     */
    protected boolean disableAdditionalFilters = false;

    // Selection

    /**
     * List of names of the fields to return for the matched documents
     */
    protected String[] fields = new String[] { "localId" };

    // Pagination

    /**
     * Position of the first document to return
     */
    protected int offset = 0;

    /**
     * Number of documents to return
     */
    protected int limit = 10;

    // Sort
    protected SortRequest sort;

    // Facets
    protected FacetRequest facets;

    // Highlight
    protected HighlightRequest highlights;

    // Suggestions
    protected SuggestRequest suggestions;

    // Geo Filters
    protected LocationRequest locations;

    public String getIndexId() {
        return indexId;
    }

    public SearchRequest setIndexId(final String indexId) {
        this.indexId = indexId;
        return this;
    }

    public String getMainQuery() {
        return mainQuery;
    }

    public SearchRequest setMainQuery(final String mainQuery) {
        this.mainQuery = mainQuery;
        return this;
    }

    public List<String> getFilterQueries() {
        return filterQueries;
    }

    public SearchRequest setFilterQueries(final List<String> filterQueries) {
        this.filterQueries = filterQueries;
        return this;
    }

    public SearchRequest addFilterQuery(final String filterQuery) {
        filterQueries.add(filterQuery);
        return this;
    }

    public boolean isDisableAdditionalFilters() {
        return disableAdditionalFilters;
    }

    public SearchRequest setDisableAdditionalFilters(final boolean disableAdditionalFilters) {
        this.disableAdditionalFilters = disableAdditionalFilters;
        return this;
    }

    public String[] getFields() {
        return fields;
    }

    public SearchRequest setFields(final String... fields) {
        this.fields = fields;
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public SearchRequest setOffset(final int offset) {
        this.offset = offset;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public SearchRequest setLimit(final int limit) {
        this.limit = limit;
        return this;
    }

    public SortRequest getSort() {
        return sort;
    }

    public SearchRequest setSort(final SortRequest sort) {
        this.sort = sort;
        return this;
    }

    public SortRequest enableSort() {
        sort = new SortRequest();
        return sort;
    }

    public FacetRequest getFacets() {
        return facets;
    }

    public SearchRequest setFacets(final FacetRequest facets) {
        this.facets = facets;
        return this;
    }

    public FacetRequest enableFacets() {
        facets = new FacetRequest();
        return facets;
    }

    public HighlightRequest getHighlights() {
        return highlights;
    }

    public SearchRequest setHighlights(final HighlightRequest highlights) {
        this.highlights = highlights;
        return this;
    }

    public HighlightRequest enableHighlight() {
        highlights = new HighlightRequest();
        return highlights;
    }

    public SuggestRequest getSuggestions() {
        return suggestions;
    }

    public SearchRequest setSuggestions(final SuggestRequest suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    public SuggestRequest enableSuggestions() {
        suggestions = new SuggestRequest();
        return suggestions;
    }

    public LocationRequest getLocations() {
        return locations;
    }

    public SearchRequest setLocations(final LocationRequest locations) {
        this.locations = locations;
        return this;
    }

    public LocationRequest enableLocations() {
        locations = new LocationRequest();
        return locations;
    }

    @Override
    public String toString() {
        return "SearchRequest{" + "indexId='" + indexId + '\'' + ", mainQuery='" + mainQuery + '\'' + ", "
            + "filterQueries=" + filterQueries + ", disableAdditionalFilters=" + disableAdditionalFilters + ", fields"
            + "=" + Arrays.toString(fields) + ", offset=" + offset + ", limit=" + limit + ", sort=" + sort + ", "
            + "facets=" + facets + ", highlights=" + highlights + ", suggestions=" + suggestions + ", locations="
            + locations + '}';
    }

}
