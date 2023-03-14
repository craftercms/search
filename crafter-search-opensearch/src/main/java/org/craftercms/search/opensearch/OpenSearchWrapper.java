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

package org.craftercms.search.opensearch;

import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.client.RequestOptions;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Exposes the search related operations from the OpenSearch client
 * @author joseross
 */
public interface OpenSearchWrapper {

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(SearchRequest request) throws OpenSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    SearchResponse search(SearchRequest request, RequestOptions options) throws OpenSearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(Map<String, Object> request) throws OpenSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(Map<String, Object> request, RequestOptions options) throws OpenSearchException {
        return search(request, emptyMap(), options);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param parameters the parameters for the search
     * @param options the request options
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    SearchResponse search(Map<String, Object> request, Map<String, Object> parameters, RequestOptions options)
            throws OpenSearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @param parameters the parameters for the search
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(Map<String, Object> request, Map<String, Object> parameters)
            throws OpenSearchException {
        return search(request, parameters, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(String request) throws OpenSearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(String request, RequestOptions options) throws OpenSearchException {
        return search(request, emptyMap(), options);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param parameters the parameters for the search
     * @param options the request options
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    SearchResponse search(String request, Map<String, Object> parameters, RequestOptions options)
            throws OpenSearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @param parameters the parameters for the search
     * @return the search response
     * @throws OpenSearchException if there is any error executing the search
     */
    default SearchResponse search(String request, Map<String, Object> parameters) throws OpenSearchException {
        return search(request, parameters, RequestOptions.DEFAULT);
    }

}
