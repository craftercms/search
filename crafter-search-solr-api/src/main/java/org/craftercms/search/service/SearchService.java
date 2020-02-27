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
package org.craftercms.search.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;

/**
 * Provides a basic interface to a search engine, like Solr.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
public interface SearchService<T extends Query> extends QueryFactory<T> {

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
    Map<String, Object> search(T query) throws SearchException;

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
    void update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException;

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
    void delete(String site, String id) throws SearchException;

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
    void updateContent(String site, String id, File file) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param file      the file content to update in the index
     * @throws SearchException
     */
    void updateContent(String indexId, String site, String id, File file) throws SearchException;

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
    void updateContent(String site, String id, File file, Map<String, List<String>> additionalFields) throws SearchException;

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
    void updateContent(String site, String id, Content content) throws SearchException;

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param content   the file content to update in the index
     * @throws SearchException
     */
    void updateContent(String indexId, String site, String id, Content content) throws SearchException;

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
    void updateContent(String site, String id, Content content, Map<String, List<String>> additionalFields) throws SearchException;

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
    void commit() throws SearchException;

    /**
     * Commits any pending changes made to the search engine's default index.
     *
     * @throws SearchException
     */
    void commit(String indexId) throws SearchException;

}
