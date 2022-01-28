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

package org.craftercms.search.elasticsearch;

import org.craftercms.search.elasticsearch.exception.ElasticsearchException;

import java.util.Locale;

/**
 * Provides operations to manage indices in Elasticsearch
 * @author joseross
 * @since 3.1.0
 */
public interface ElasticsearchAdminService extends AutoCloseable {

    /**
     * Creates an index
     * @param aliasName the name of the alias
     * @throws ElasticsearchException if there is any error during the operation
     */
    void createIndex(String aliasName) throws ElasticsearchException;

    /**
     * Creates an index for the given locale
     * @param aliasName the name of the alias
     * @param locale the locale for the index
     */
    void createIndex(String aliasName, Locale locale);

    /**
     * Deletes all indexes assigned to the given alias
     * @param aliasName the name of the alias
     * @throws ElasticsearchException if there is any error during the operation
     */
    void deleteIndexes(String aliasName) throws ElasticsearchException;

    /**
     * Recreates an existing index
     *
     * @param aliasName the name of the alias
     * @throws ElasticsearchException if there is any error during the operation
     */
    void recreateIndex(String aliasName) throws ElasticsearchException;

    /**
     * Checks if the Elasticsearch cluster is ready to receive requests
     */
    void waitUntilReady();

}
