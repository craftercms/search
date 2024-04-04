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

import java.io.IOException;
import java.util.Locale;

/**
 * Provides operations to manage indices in OpenSearch
 * @author joseross
 * @since 3.1.0
 */
public interface OpenSearchAdminService extends AutoCloseable {

    /**
     * Creates an index
     * @param aliasName the name of the alias
     * @throws OpenSearchException if there is any error during the operation
     */
    void createIndex(String aliasName) throws OpenSearchException;

    /**
     * Indicates if an index exists for the given alias
     *
     * @param aliasName the index alias
     * @return true if the index exists, false otherwise
     * @throws OpenSearchException if there is any error while checking the index
     */
    boolean indexExists(String aliasName) throws OpenSearchException;

    /**
     * Creates an index for the given locale
     * @param aliasName the name of the alias
     * @param locale the locale for the index
     */
    void createIndex(String aliasName, Locale locale);

    /**
     * Deletes all indexes assigned to the given alias
     * @param aliasName the name of the alias
     * @throws OpenSearchException if there is any error during the operation
     */
    void deleteIndexes(String aliasName) throws OpenSearchException;

    /**
     * Recreates an existing index
     *
     * @param aliasName the name of the alias
     * @throws OpenSearchException if there is any error during the operation
     */
    void recreateIndex(String aliasName) throws OpenSearchException;

    /**
     * Checks if the OpenSearch cluster is ready to receive requests
     */
    void waitUntilReady();

    /**
     * Create a new index with the same settings and mappings as the source index,
     * then reindex all data to the newly created index.
     *
     * @param srcIndex  the existing source index
     * @param destIndex the new index to be created
     * @throws OpenSearchException if there is any error during the operation
     */
    void duplicateIndex(String srcIndex, String destIndex) throws OpenSearchException;

}
