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

import org.craftercms.search.elasticsearch.exception.ElasticsearchException;

/**
 * Provides operations to manage indices in Elasticsearch
 * @author joseross
 */
public interface ElasticsearchAdminService extends AutoCloseable {

    /**
     * Creates an index
     * @param indexName the name of the index
     * @param isAuthoring indicates if the index if for authoring
     * @throws ElasticsearchException if there is any error during the operation
     */
    void createIndex(String indexName, boolean isAuthoring) throws ElasticsearchException;

    /**
     * Deletes an index
     * @param indexName the name of the index
     * @throws ElasticsearchException if there is any error during the operation
     */
    void deleteIndex(String indexName) throws ElasticsearchException;

}
