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

import java.util.List;
import java.util.Map;

import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.core.service.Content;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;

/**
 * Provides access to indexing operations in Elasticsearch
 * @author joseross
 */
public interface ElasticsearchService extends AutoCloseable {

    /**
     * Performs a search for a specific field
     * @param indexName the name of the index
     * @param field the name of the field
     * @param queryBuilder the filters to apply
     * @return the list of values that match the search
     * @throws ElasticsearchException if there is any error during the operation
     */
    List<String> searchField(String indexName, String field, QueryBuilder queryBuilder) throws ElasticsearchException;

    Map<String, Object> searchId(String indexName, String docId);

    void index(String indexName, String siteId, String docId, Map<String, Object> doc);

    /**
     * Performs an index for the given xml file
     * @param indexName the name of the index
     * @param siteId the name of the site
     * @param docId the id of the document
     * @param xml the content of the document
     * @throws ElasticsearchException if there is any error during the operation
     */
    default void index(String indexName, String siteId, String docId, String xml) throws ElasticsearchException {
        index(indexName, siteId, docId, xml, null);
    }

    /**
     * Performs an index for the given xml file
     * @param indexName the name of the index
     * @param siteId the name of the site
     * @param docId the id of the document
     * @param xml the content of the document
     * @param additionalFields additional fields to index
     * @throws ElasticsearchException if there is any error during the operation
     */
    void index(String indexName, String siteId, String docId, String xml,
               MultiValueMap<String, String> additionalFields) throws ElasticsearchException;

    /**
     * Performs an index for the given binary file
     * @param indexName the name of the index
     * @param siteName the name of the site
     * @param path the path of the document
     * @param additionalFields the additional fields to index
     * @param content the content of the document
     * @throws ElasticsearchException if there is any error during the operation
     */
    void indexBinary(String indexName, String siteName, String path, MultiValueMap<String, String> additionalFields,
                     Content content) throws ElasticsearchException;

    void indexBinary(String indexName, String siteName, String path, MultiValueMap<String, String> additionalFields,
                     Resource resource) throws ElasticsearchException;

    /**
     * Performs a delete for the given document
     * @param indexName the name of the index
     * @param siteId the id of the site
     * @param docId the id of the document
     * @throws ElasticsearchException if there is any error during the operation
     */
    void delete(String indexName, String siteId, String docId) throws ElasticsearchException;

    /**
     * Performs a refresh for a given index
     * @param indexName the name of the index
     * @throws ElasticsearchException if there is any error during the operation
     */
    void refresh(String indexName) throws ElasticsearchException;

}
