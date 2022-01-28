/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.client;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyMap;

/**
 * Exposes the search related operations from {@link co.elastic.clients.elasticsearch.ElasticsearchClient}
 * @implNote The method signatures are copied from the original class because there is no interface and the class is
 *           final, so it can't be properly extended to be a drop-in replacement.
 * @author joseross
 * @since 4.0.0
 */
public interface ElasticsearchClientWrapper {

    /**
     * Executes the given request
     */
    default <T> SearchResponse<T> search(SearchRequest request, Class<T> documentClass)
            throws IOException, ElasticsearchException {
        return search(request, documentClass, emptyMap());
    }

    /**
     * Executes the given request
     */
    <T> SearchResponse<T> search(SearchRequest request, Class<T> documentClass, Map<String, Object> parameters)
            throws IOException, ElasticsearchException;

    /**
     * Creates and executes a request using the given function
     */
    default <T> SearchResponse<T> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function,
                                 Class<T> documentClass) throws IOException, ElasticsearchException {
        return search(function, documentClass, emptyMap());
    }

    /**
     * Creates and executes a request using the given function
     */
    default <T> SearchResponse<T> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function,
                                 Class<T> documentClass, Map<String, Object> parameters)
            throws IOException, ElasticsearchException {
        return search(SearchRequest.of(function), documentClass, parameters);
    }

}
