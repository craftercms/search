/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
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
package org.craftercms.search.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;

/**
 * Provides a basic interface to a search engine, like Solr.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 * @author joseross
 */

public interface SearchService<T extends Query> extends QueryFactory<T>  {

    /**
     * Does a full-text search and returns a Map model.
     *
     * @param query the query object
     *
     * @return search result in a Map data model. The key is the field name and the value is the field single value or
     *         multiple values (as a list)
     *
     * @throws SearchException if any error occurs that makes the search fail
     */
    default Map<String, Object> search(T query) throws SearchException {
        return search(null, query);
    }

    /**
     * Does a full-text search and returns a Map model.
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param query     the query object
     *
     * @return search result in a Map data model. The key is the field name and the value is the field single value or
     *         multiple values (as a list)
     *
     * @throws SearchException if any error occurs that makes the search fail
     */
    Map<String, Object> search(String indexId, T query) throws SearchException;

    /**
     * Updates the search engine's index data of an XML document.
     *
     * @param site                   the Crafter site name the content belongs to
     * @param id                     the id of the XML document, within the site
     * @param xml                    the XML document to update in the index
     * @param ignoreRootInFieldNames ignore the root element of the input XML in field names
     * @throws SearchException
     */
    default void update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        update(null, site, id, xml, ignoreRootInFieldNames);
    }

    /**
     * Updates the search engine's index data of an XML document.
     *
     * @param indexId                   the index ID (core in Solr terminology). Use null for default index.
     * @param site                      the Crafter site name the content belongs to
     * @param id                        the id of the XML document, within the site
     * @param xml                       the XML document to update in the index
     * @param ignoreRootInFieldNames    ignore the root element of the input XML in field names
     * @throws SearchException
     */
    void update(String indexId, String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException;

    /**
     * Deletes the search engine's index data of an XML document.
     *
     * @param site the Crafter site name the content belongs to
     * @param id   the id of the content, within the site
     * @throws SearchException
     */
    default void delete(String site, String id) throws SearchException {
        delete(null, site, id);
    }

    /**
     * Deletes the search engine's index data of an XML document.
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the content, within the site
     * @throws SearchException
     */
    void delete(String indexId, String site, String id) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site  the Crafter site name the content belongs to
     * @param id    the id of the file, within the site
     * @param file  the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String site, String id, File file) throws SearchException {
        updateContent(null, site, id, file, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param file      the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String indexId, String site, String id, File file) throws SearchException {
        updateContent(indexId, site, id, file, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the file, within the site
     * @param file              the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    default void updateContent(String site, String id, File file, Map<String, List<String>> additionalFields)
        throws SearchException {
        updateContent(null, site, id, file, additionalFields);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId           the index ID (core in Solr terminology). Use null for default index.
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the XML document, within the site
     * @param file              the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    void updateContent(String indexId, String site, String id, File file, Map<String, List<String>> additionalFields)
            throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param content   the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String site, String id, Content content) throws SearchException {
        updateContent(null, site, id, content, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param content   the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String indexId, String site, String id, Content content) throws SearchException {
        updateContent(indexId, site, id, content, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the file, within the site
     * @param content           the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    default void updateContent(String site, String id, Content content, Map<String, List<String>> additionalFields)
        throws SearchException {
        updateContent(null, site, id, content, additionalFields);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId           the index ID (core in Solr terminology). Use null for default index.
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the XML document, within the site
     * @param content           the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    void updateContent(String indexId, String site, String id, Content content,
                       Map<String, List<String>> additionalFields) throws SearchException;

    /**
     * Commits any pending changes made to the search engine's default index.
     *
     * @throws SearchException
     */
    default void commit() throws SearchException {
        commit(null);
    }

    /**
     * Commits any pending changes made to the search engine's default index.
     *
     * @throws SearchException
     */
    void commit(String indexId) throws SearchException;

    /**
     * Prepares a new instance of {@link SearchRequest}
     * @return the request object
     */
    default SearchRequest createRequest() {
        return new SearchRequest();
    }

    /**
     * Performs a search using the given request
     * @param request the request object
     * @return the result object
     */
    SearchResponse search(SearchRequest request);

    /**
     * Performs a search using the current provider format for request and response
     * @param indexId the index id
     * @param params the search parameters
     * @return the search results
     */
    Map<String, Object> nativeSearch(String indexId, Map<String, Object> params);

    /**
     * Performs a search using the current provider format for request and response
     * @param params the search parameters
     * @return the search results
     */
    default Map<String, Object> nativeSearch(Map<String, Object> params) {
        return nativeSearch(null, params);
    }

}
