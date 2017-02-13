package org.craftercms.search.service.impl.elasticsearch;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link SearchService} using Elasticsearch as the underlying search engine.
 *
 * @author Matt Wittmann
 */
@Service("crafter.searchService.elasticsearch")
public class ElasticsearchSearchService implements SearchService {
    static final String ID_DELIMITER = ":";
    static final String DEFAULT_INDEX = "default_index";
    static final String ELASTICSEARCH_TYPE = "crafter_search";
    private final QueryComponent queryComponent;
    private final DeletionComponent deletionComponent;
    private final UpdateComponent updateComponent;

    @Autowired
    public ElasticsearchSearchService(QueryComponent queryComponent, DeletionComponent deletionComponent, UpdateComponent updateComponent) {
        this.queryComponent = queryComponent;
        this.deletionComponent = deletionComponent;
        this.updateComponent = updateComponent;
    }

    @Override
    public Map<String, Object> search(Query query) throws SearchException {
        return queryComponent.search(query);
    }

    @Override
    public Map<String, Object> search(String indexId, Query query) throws SearchException {
        return queryComponent.search(indexId, query);
    }

    @Override
    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        return update(DEFAULT_INDEX, site, id, xml, ignoreRootInFieldNames);
    }

    @Override
    public String update(String indexId, String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        String finalId = getFinalId(site, id);
        UpdateResponse response = updateComponent.update(indexId, ELASTICSEARCH_TYPE, finalId, xml);

        return getSuccessfulMessage(indexId, finalId, "Update", response);
    }

    @Override
    public String delete(String site, String id) throws SearchException {
        return delete(DEFAULT_INDEX, site, id);
    }

    @Override
    public String delete(String indexId, String site, String id) throws SearchException {
        String finalId = getFinalId(site, id);
        DeleteResponse response = deletionComponent.delete(indexId, ELASTICSEARCH_TYPE, finalId);

        return getSuccessfulMessage(indexId, finalId, "Delete", response);
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document, Map<String, String> additionalFields) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    public String updateFile(String site, String id, File file) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    public String updateFile(String site, String id, File file, Map<String, List<String>> additionalFields) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file, Map<String, List<String>> additionalFields) throws SearchException {
        throw new NotImplementedException("This method has not yet been implemented!");
    }

    @Override
    public String commit() throws SearchException {
        throw new UnsupportedOperationException("Elasticsearch does not support a commit operation.");
    }

    @Override
    public String commit(String indexId) throws SearchException {
        throw new UnsupportedOperationException("Elasticsearch does not support a commit operation.");
    }

    private static String getFinalId(String site, String id) {
        return site + ID_DELIMITER + id;
    }

    private static String getSuccessfulMessage(String indexId, String idOrQuery, String operation, DocWriteResponse response) {
        return String.format("%s%s for %s successful: %s", getIndexPrefix(indexId), operation, idOrQuery, response);
    }

    private static String getIndexPrefix(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "[" + indexId + "] " : "";
    }
}
