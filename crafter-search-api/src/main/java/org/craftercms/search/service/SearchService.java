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

import org.craftercms.search.exception.SearchException;

/**
 * Provides a basic interface to a search engine, like Solr.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
public interface SearchService {

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
    Map<String, Object> search(Query query) throws SearchException;

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
    Map<String, Object> search(String indexId, Query query) throws SearchException;

    /**
     * Updates the search engine's index data of an XML document.
     *
     * @param site                   the Crafter site name the content belongs to
     * @param id                     the id of the XML document, within the site
     * @param xml                    the XML document to update in the index
     * @param ignoreRootInFieldNames ignore the root element of the input XML in field names
     * @return a success string
     * @throws SearchException
     */
    String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException;

    /**
     * Updates the search engine's index data of an XML document.
     *
     * @param indexId                   the index ID (core in Solr terminology). Use null for default index.
     * @param site                      the Crafter site name the content belongs to
     * @param id                        the id of the XML document, within the site
     * @param xml                       the XML document to update in the index
     * @param ignoreRootInFieldNames    ignore the root element of the input XML in field names
     * @return a success string
     * @throws SearchException
     */
    String update(String indexId, String site, String id, String xml,
                  boolean ignoreRootInFieldNames) throws SearchException;

    /**
     * Deletes the search engine's index data of an XML document.
     *
     * @param site the Crafter site name the content belongs to
     * @param id   the id of the content, within the site
     * @return a success string
     * @throws SearchException
     */
    String delete(String site, String id) throws SearchException;

    /**
     * Deletes the search engine's index data of an XML document.
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the content, within the site
     * @return a success string
     * @throws SearchException
     */
    String delete(String indexId, String site, String id) throws SearchException;

    /**
     * Updates the search engine's index data of a structured document (PDF, Word, Office).
     *
     * @param site     the Crafter site name the content belongs to
     * @param id       the id of the XML document, within the site
     * @param document the document to update in the index
     *
     * @return a success string
     * @throws SearchException
     * @deprecated use {@link #updateFile(String, String, File)} instead
     */
    @Deprecated
    String updateDocument(String site, String id, File document) throws SearchException;

    /**
     * Updates the search engine's index data of a structured document (PDF, Word, Office).
     *
     * @param site                  the Crafter site name the content belongs to
     * @param id                    the id of the XML document, within the site
     * @param document              the document to update in the index
     * @param additionalFields      additional metadata fields to be indexed (shouldn't have the name site, id or
     *                              document)
     * @return a success string
     * @throws SearchException
     * @deprecated use {@link #updateFile(String, String, File, Map<String, List<String>>)} instead
     */
    @Deprecated
    String updateDocument(String site, String id, File document,
                          Map<String, String> additionalFields) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site  the Crafter site name the content belongs to
     * @param id    the id of the file, within the site
     * @param file  the file to update in the index
     *
     * @return a success string
     * @throws SearchException
     */
    String updateFile(String site, String id, File file) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param file      the file to update in the index
     *
     * @return a success string
     * @throws SearchException
     */
    String updateFile(String indexId, String site, String id, File file) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the file, within the site
     * @param file              the file to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @return a success string
     * @throws SearchException
     */
    String updateFile(String site, String id, File file,
                      Map<String, List<String>> additionalFields) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId           the index ID (core in Solr terminology). Use null for default index.
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the XML document, within the site
     * @param file              the file to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     *
     * @return a success string
     * @throws SearchException
     */
    String updateFile(String indexId, String site, String id, File file,
                      Map<String, List<String>> additionalFields) throws SearchException;

    /**
     * Commits any pending changes made to the search engine's default index.
     *
     * @return a success string
     * @throws SearchException
     */
    String commit() throws SearchException;

    /**
     * Commits any pending changes made to the search engine's default index.
     *
     * @return a success string
     * @throws SearchException
     */
    String commit(String indexId) throws SearchException;

}
