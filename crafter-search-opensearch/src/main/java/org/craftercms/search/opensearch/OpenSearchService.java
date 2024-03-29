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

import java.util.List;
import java.util.Map;

import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.craftercms.core.service.Content;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.core.io.Resource;

/**
 * Provides access to indexing operations in OpenSearch
 * @author joseross
 */
public interface OpenSearchService extends AutoCloseable {

    /**
     * Performs a search for a specific field
     * @param indexName the name of the index
     * @param field the name of the field
     * @param query the filters to apply
     * @return the list of values that match the search
     * @throws OpenSearchException if there is any error during the operation
     */
    List<String> searchField(String indexName, String field, Query query) throws OpenSearchException;

    Map<String, Object> searchId(String indexName, String docId);

    void index(String indexName, String siteId, String docId, Map<String, Object> doc);

    /**
     * Performs an index for the given xml file
     * @param indexName the name of the index
     * @param siteId the name of the site
     * @param docId the id of the document
     * @param xml the content of the document
     * @throws OpenSearchException if there is any error during the operation
     */
    default void index(String indexName, String siteId, String docId, String xml) throws OpenSearchException {
        index(indexName, siteId, docId, xml, null);
    }

    /**
     * Performs an index for the given xml file
     * @param indexName the name of the index
     * @param siteId the name of the site
     * @param docId the id of the document
     * @param xml the content of the document
     * @param additionalFields additional fields to index
     * @throws OpenSearchException if there is any error during the operation
     */
    void index(String indexName, String siteId, String docId, String xml, Map<String, Object> additionalFields)
            throws OpenSearchException;

    /**
     * Performs an index for the given binary file
     * @param indexName the name of the index
     * @param siteName the name of the site
     * @param path the path of the document
     * @param additionalFields the additional fields to index
     * @param content the content of the document
     * @throws OpenSearchException if there is any error during the operation
     */
    void indexBinary(String indexName, String siteName, String path, Content content,
                     Map<String, Object> additionalFields) throws OpenSearchException;

    default void indexBinary(String indexName, String siteName, String path, Content content)
            throws OpenSearchException {
        indexBinary(indexName, siteName, path, content,null);
    }

    void indexBinary(String indexName, String siteName, String path, Resource resource,
                     Map<String, Object> additionalFields) throws OpenSearchException;

    default void indexBinary(String indexName, String siteName, String path, Resource resource)
            throws OpenSearchException {
        indexBinary(indexName, siteName, path, resource, null);
    }

    /**
     * Performs a delete for the given document
     * @param indexName the name of the index
     * @param siteId the id of the site
     * @param docId the id of the document
     * @throws OpenSearchException if there is any error during the operation
     */
    void delete(String indexName, String siteId, String docId) throws OpenSearchException;

    /**
     * Performs a refresh for a given index
     * @param indexName the name of the index
     * @throws OpenSearchException if there is any error during the operation
     */
    void refresh(String indexName) throws OpenSearchException;

}
