package org.craftercms.search.service;

import org.craftercms.search.exception.SearchException;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.List;
import java.util.Map;

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
