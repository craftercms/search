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

import java.util.List;
import java.util.Map;

import org.craftercms.search.v3.model.facet.Facet;
import org.craftercms.search.v3.model.highlight.Highlight;
import org.craftercms.search.v3.model.suggest.Suggestion;

/**
 * Holds the results from a search operation
 * @author joseross
 */
public class SearchResponse {

    /**
     * Total number of documents matched
     */
    protected long total;

    /**
     * Position of the first document returned
     */
    protected long offset;

    /**
     * Number of documents returned
     */
    protected long limit;

    /**
     * List of the returned fields for each matched document
     */
    protected List<Map<String, Object>> items;

    /**
     * Facets generated during the search
     */
    protected List<Facet> facets;

    /**
     * Highlights generated during the search
     */
    protected List<Highlight> highlights;

    /**
     * Suggestions generated during the search
     */
    protected List<Suggestion> suggestions;

    public long getTotal() {
        return total;
    }

    public void setTotal(final long total) {
        this.total = total;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(final long offset) {
        this.offset = offset;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(final long limit) {
        this.limit = limit;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(final List<Map<String, Object>> items) {
        this.items = items;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(final List<Facet> facets) {
        this.facets = facets;
    }

    public List<Highlight> getHighlights() {
        return highlights;
    }

    public void setHighlights(final List<Highlight> highlights) {
        this.highlights = highlights;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(final List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "SearchResponse{" + "total=" + total + ", offset=" + offset + ", limit=" + limit + ", items=" + items
            + ", facets=" + facets + ", highlights=" + highlights + ", suggestions=" + suggestions + '}';
    }

}
