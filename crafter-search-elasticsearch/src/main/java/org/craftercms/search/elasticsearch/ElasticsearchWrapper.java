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

package org.craftercms.search.elasticsearch;

import java.util.Map;

import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;

/**
 * Exposes the search related operations from the Elasticsearch client
 * @author joseross
 */
public interface ElasticsearchWrapper {

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    default SearchResponse search(SearchRequest request) throws ElasticsearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    SearchResponse search(SearchRequest request, RequestOptions options) throws ElasticsearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    default SearchResponse search(Map<String, Object> request) throws ElasticsearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    SearchResponse search(Map<String, Object> request, RequestOptions options) throws ElasticsearchException;

    /**
     * Performs a search operation
     * @param request the search request
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    default SearchResponse search(String request) throws ElasticsearchException {
        return search(request, RequestOptions.DEFAULT);
    }

    /**
     * Performs a search operation
     * @param request the search request
     * @param options the request options
     * @return the search response
     * @throws ElasticsearchException if there is any error executing the search
     */
    SearchResponse search(String request, RequestOptions options) throws ElasticsearchException;

}
