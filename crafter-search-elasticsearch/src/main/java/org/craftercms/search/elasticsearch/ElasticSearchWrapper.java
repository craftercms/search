/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch;

import java.util.Map;

import org.craftercms.search.elasticsearch.exception.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;

/**
 * Exposes the search related operations from the ElasticSearch client
 * @author joseross
 */
public interface ElasticSearchWrapper {

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    default SearchResponse search(SearchRequest request) throws ElasticSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    SearchResponse search(SearchRequest request, RequestOptions options) throws ElasticSearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    default SearchResponse search(Map<String, Object> request) throws ElasticSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    SearchResponse search(Map<String, Object> request, RequestOptions options) throws ElasticSearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    default SearchResponse search(String request) throws ElasticSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticSearchException if there is any error executing the search
     */
    SearchResponse search(String request, RequestOptions options) throws ElasticSearchException;

}
