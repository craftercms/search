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

import org.craftercms.search.exception.SearchException;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

/**
 * Extension of {@link SearchService} that provides methods for indexing binary/document files accessible through
 * Spring {@link Resource}s.
 *
 * @author avasquez
 */
public interface ResourceAwareSearchService<T extends Query> extends SearchService<T> {

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param resource  the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String site, String id, Resource resource) throws SearchException {
        updateContent(null, site, id, resource, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId   the index ID (core in Solr terminology). Use null for default index.
     * @param site      the Crafter site name the content belongs to
     * @param id        the id of the file, within the site
     * @param resource  the file content to update in the index
     * @throws SearchException
     */
    default void updateContent(String indexId, String site, String id, Resource resource) throws SearchException {
        updateContent(indexId, site, id, resource, null);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the file, within the site
     * @param resource          the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    default void updateContent(String site, String id, Resource resource, Map<String, List<String>> additionalFields)
            throws SearchException {
        updateContent(null, site, id, resource, additionalFields);
    }

    /**
     * Updates the search engine's index data of a binary or structured document (PDF, Word, Office).
     *
     * @param indexId           the index ID (core in Solr terminology). Use null for default index.
     * @param site              the Crafter site name the content belongs to
     * @param id                the id of the XML document, within the site
     * @param resource          the file content to update in the index
     * @param additionalFields  additional metadata fields to be indexed (shouldn't have the name site, id or
     *                          document)
     * @throws SearchException
     */
    void updateContent(String indexId, String site, String id, Resource resource,
                       Map<String, List<String>> additionalFields) throws SearchException;

}
