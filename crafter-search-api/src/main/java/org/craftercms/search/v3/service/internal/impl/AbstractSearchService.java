/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.v3.service.internal.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.activation.FileTypeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.v3.service.internal.SearchMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

/**
 * Basic operations for all {@link SearchService} implementations
 * @param <T> type for query objects
 * @param <I> type for native search requests
 * @param <O> type for native search responses
 * @author joseross
 */
public abstract class AbstractSearchService<T extends Query, I, O> implements SearchService<T> {

    public static final String DEFAULT_FILE_NAME_FIELD_NAME = "file-name";

    private static final Logger logger = LoggerFactory.getLogger(AbstractSearchService.class);

    /**
     * The ID of the default index.
     */
    protected String defaultIndexId;

    /**
     * Set of additional filter queries that should be used on all search requests
     */
    protected String[] additionalFilterQueries;

    /**
     * ID regex/delete query mappings that can be used to specify special delete queries for certain files,
     * e.g. delete XML documents with their sub-documents.
     */
    protected Map<String, String> deleteQueryMappings;

    /**
     * The file name field name (default is file-name).
     */
    protected String fileNameFieldName;

    /**
     * Mime type map used to retrieve the mime types of files when submitting binary/structured content for indexing.
     */
    protected FileTypeMap mimeTypesMap;

    /**
     * Mapper used to translate requests and responses
     */
    protected SearchMapper<I, O> searchMapper;

    public AbstractSearchService() {
        fileNameFieldName = DEFAULT_FILE_NAME_FIELD_NAME;
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
    }

    /**
     * Sets the ID of the default index.
     */
    public void setDefaultIndexId(String defaultIndexId) {
        this.defaultIndexId = defaultIndexId;
    }

    /**
     * Sets the set of additional filter queries that should be used on all search requests
     */
    public void setAdditionalFilterQueries(String[] additionalFilterQueries) {
        this.additionalFilterQueries = additionalFilterQueries;
    }

    /**
     * Sets ID regex/delete query mappings that can be used to specify special delete queries for certain
     * files, e.g. delete XML documents with their sub-documents.
     */
    public void setDeleteQueryMappings(Map<String, String> deleteQueryMappings) {
        this.deleteQueryMappings = deleteQueryMappings;
    }

    /**
     * Sets the file name field (default is file-name).
     */
    public void setFileNameFieldName(String fileNameFieldName) {
        this.fileNameFieldName = fileNameFieldName;
    }

    @Required
    public void setSearchMapper(final SearchMapper<I, O> searchMapper) {
        this.searchMapper = searchMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> search(String indexId, final T query) throws SearchException {
        String finalIndexId = getIndexId(indexId);

        addAdditionalFilterQueries(finalIndexId, query);

        if (logger.isDebugEnabled()) {
            logger.debug("{}Executing query {}", getIndexPrefix(finalIndexId), query);
        }

        Map<String, Object> map = doSearch(finalIndexId, query);

        if (logger.isDebugEnabled()) {
            logger.debug("{}Response for query {}: {}", getIndexPrefix(finalIndexId), query, map);
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String indexId, final String site, final String id, final String xml,
                       final boolean ignoreRootInFieldNames) throws SearchException {
        String finalIndexId = getIndexId(indexId);

        String finalId = getFinalId(site, id);

        // This is done because when a document is updated, and it had children before but not now, the children
        // would be orphaned (SOLR-6096)
        delete(finalIndexId, site, id);

        doUpdate(finalIndexId, site, id, finalId, xml, ignoreRootInFieldNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String indexId, final String site, final String id) throws SearchException {
        String finalIndexId = getIndexId(indexId);

        String finalId = getFinalId(site, id);
        String query = getDeleteQuery(finalId);

        doDelete(finalIndexId, finalId, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateContent(String indexId, final String site, final String id, final File file,
                              final Map<String, List<String>> additionalFields) throws SearchException {
        String finalIndexId = getIndexId(indexId);

        String finalId = getFinalId(site, id);
        String fileName = FilenameUtils.getName(id);
        String contentType = mimeTypesMap.getContentType(fileName);

        doUpdateContent(finalIndexId, site, id, finalId, file, fileName, contentType, additionalFields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateContent(final String site, final String id, final Content content) throws SearchException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateContent(final String indexId, final String site, final String id, final Content content) throws SearchException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateContent(final String site, final String id, final Content content, final Map<String,
        List<String>> additionalFields) throws SearchException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateContent(final String indexId, final String site, final String id, final Content content,
                              final Map<String, List<String>> additionalFields) throws SearchException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit(String indexId) throws SearchException {
        indexId = getIndexId(indexId);

        doCommit(indexId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(SearchRequest request) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setOffset(request.getOffset());
        searchResponse.setLimit(request.getLimit());
        addAdditionalFilterQueries(request);
        I nativeRequest = searchMapper.mapRequest(request);
        O nativeResponse = doSearch(getIndexId(request.getIndexId()), nativeRequest);
        searchMapper.mapResponse(nativeResponse, searchResponse);
        return searchResponse;
    }

    protected void addAdditionalFilterQueries(SearchRequest request) {
        if(request.isDisableAdditionalFilters()) {
            return;
        }

        if(additionalFilterQueries != null) {
            for (String additionalQuery : additionalFilterQueries) {
                boolean add = true;
                if (StringUtils.contains(request.getMainQuery(), additionalQuery)) {
                    continue;
                }
                if (CollectionUtils.isNotEmpty(request.getFilterQueries())) {
                    for (String filterQuery : request.getFilterQueries()) {
                        if (StringUtils.contains(filterQuery, additionalQuery)) {
                            add = false;
                            break;
                        }
                    }
                }
                if(add) {
                    request.addFilterQuery(additionalQuery);
                }
            }
        }
    }

    protected String getDeleteQuery(String id) {
        if (MapUtils.isNotEmpty(deleteQueryMappings)) {
            for (Map.Entry<String, String> mapping : deleteQueryMappings.entrySet()) {
                if (id.matches(mapping.getKey())) {
                    return String.format(mapping.getValue(), id);
                }
            }
        }

        return null;
    }

    protected String getIndexPrefix(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "[" + indexId + "] " : "";
    }

    protected String getIndexId(String indexId) {
        return StringUtils.isEmpty(indexId)? defaultIndexId : indexId;
    }

    protected String getFinalId(String site, String id) {
        return site + ":" + id;
    }

    /**
     * Adds the additional filter queries to the given {@link Query}
     * @param indexId the index id
     * @param query the query
     */
    protected abstract void addAdditionalFilterQueries(String indexId, T query);

    /**
     * Performs a search for the given {@link Query}
     * @param indexId the index id
     * @param query the query
     * @return the search result
     */
    protected abstract Map<String, Object> doSearch(String indexId, T query);

    /**
     * Performs an update for the given xml
     * @param indexId the index name
     * @param site the site name
     * @param id the local id for the document
     * @param finalId the global id for the document
     * @param xml the XML
     * @param ignoreRootInFieldNames
     */
    protected abstract void doUpdate(String indexId, String site, String id, String finalId, String xml,
                                     boolean ignoreRootInFieldNames);

    /**
     * Performs a delete for the given document id
     * @param indexId the index id
     * @param finalId the global id of the document
     * @param query the query used to delete
     */
    protected abstract void doDelete(String indexId, String finalId, String query);

    /**
     * Performs an update for the given file
     * @param indexId the index id
     * @param site the site name
     * @param id the local id for the document
     * @param finalId the global id for the document
     * @param file the file to upload
     * @param fileName the file name
     * @param contentType the content type of the file
     * @param additionalFields the additional fields to add
     */
    protected abstract void doUpdateContent(String indexId, String site, String id, String finalId, File file,
                                            String fileName, String contentType, Map<String,
                                            List<String>> additionalFields);

    /**
     * Performs a commit for the given index
     * @param indexId the index id
     */
    protected abstract void doCommit(String indexId);

    /**
     * Performs a search using the given request
     * @param indexId the index id
     * @param request the native search request
     * @return the native search response
     */
    protected abstract O doSearch(final String indexId, final I request);

}
