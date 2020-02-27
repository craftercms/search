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
package org.craftercms.search.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.activation.FileTypeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SearchServerException;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

/**
 * Implementation of {@link SearchService} using Solr as the underlying search engine.
 *
 * @author Michael Chen
 * @author Alfonso Vasquez
 * @author Dejan Brkic
 */
public class SolrSearchService implements SearchService<SolrQuery> {

    public static final String DEFAULT_FILE_NAME_FIELD_NAME = "file-name";
    public static final String SOLR_CONTENT_STREAM_UPDATE_URL = "/update/extract";

    public static final String DOCUMENT_LIST_START_PROPERTY_NAME = "start";
    public static final String DOCUMENT_LIST_NUM_FOUND_PROPERTY_NAME = "numFound";
    public static final String DOCUMENT_LIST_MAX_SCORE_PROPERTY_NAME = "maxScore";
    public static final String DOCUMENT_LIST_DOCUMENTS_PROPERTY_NAME = "documents";

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchService.class);

    /**
     * The ID of the default index.
     */
    protected String defaultIndexId;
    /**
     * The file name field name (default is file-name).
     */
    protected String fileNameFieldName;
    /**
     * The Solr client used to execute requests against a Solr server.
     */
    protected SolrClient solrClient;
    /**
     * The Solr document builder, to build Solr documents from generic XML documents.
     */
    protected SolrDocumentBuilderImpl solrDocumentBuilder;
    /**
     * Multi value separator for additional fields of structured documents.
     */
    protected String multiValueSeparator;
    /**
     * The regex pattern used to ignore those additional fields that shouldn't be parsed for multi value.
     */
    protected String multiValueIgnorePattern;
    /**
     * ID regex/delete query mappings that can be used to specify special delete queries for certain files,
     * e.g. delete XML documents with their sub-documents.
     */
    protected Map<String, String> deleteQueryMappings;
    /**
     * Set of additional filter queries that should be used on all search requests
     */
    protected String[] additionalFilterQueries;
    /**
     * Mime type map used to retrieve the mime types of files when submitting binary/structured content for indexing.
     */
    protected FileTypeMap mimeTypesMap;

    public SolrSearchService() {
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
     * Sets the file name field (default is file-name).
     */
    public void setFileNameFieldName(String fileNameFieldName) {
        this.fileNameFieldName = fileNameFieldName;
    }

    /**
     * Sets the Solr client used to execute requests against a Solr server.
     */
    @Required
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * Sets the Solr document builder, to build Solr documents from generic XML documents.
     */
    @Required
    public void setSolrDocumentBuilder(SolrDocumentBuilderImpl solrDocumentBuilder) {
        this.solrDocumentBuilder = solrDocumentBuilder;
    }

    /**
     * Sets the multi value separator for additional fields of structured documents.
     */
    @Required
    public void setMultiValueSeparator(String multiValueSeparator) {
        this.multiValueSeparator = multiValueSeparator;
    }

    /**
     * Sets the regex pattern used to ignore those additional fields that shouldn't be parsed for multi value.
     */
    @Required
    public void setMultiValueIgnorePattern(String multiValueIgnorePattern) {
        this.multiValueIgnorePattern = multiValueIgnorePattern;
    }

    /**
     * Sets ID regex/delete query mappings that can be used to specify special delete queries for certain
     * files, e.g. delete XML documents with their sub-documents.
     */
    public void setDeleteQueryMappings(Map<String, String> deleteQueryMappings) {
        this.deleteQueryMappings = deleteQueryMappings;
    }

    /**
     * Sets the set of additional filter queries that should be used on all search requests
     */
    public void setAdditionalFilterQueries(String[] additionalFilterQueries) {
        this.additionalFilterQueries = additionalFilterQueries;
    }

    @Override
    public SolrQuery createQuery() {
        return new SolrQuery();
    }

    @Override
    public SolrQuery createQuery(Map<String, String[]> params) {
        return new SolrQuery(params);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> search(SolrQuery query) {
        return search(null, query);
    }

    @Override
    public Map<String, Object> search(String indexId, SolrQuery query) throws SearchException {
        if (StringUtils.isEmpty(indexId)) {
            indexId = defaultIndexId;
        }

        addAdditionalFilterQueries(indexId, query);

        if (logger.isDebugEnabled()) {
            logger.debug("{}Executing query {}", getIndexPrefix(indexId), query);
        }

        SolrResponse response;
        try {
            response = solrClient.query(indexId, toActualSolrQuery(query));
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(indexId, "Unable to execute query " + query, e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Search for query " + query + " failed", e);
        }

        // Solr search result is a List<Map.Entry<String,Object>>, where every entry is a (name,value) pair,
        // and there can be duplicate names in the list.
        NamedList<Object> list = response.getResponse();
        // Convert this list into a map where values of the same name are grouped into a list.
        Map<String, Object> map = toMap(list);

        if (logger.isDebugEnabled()) {
            logger.debug("{}Response for query {}: {}", getIndexPrefix(indexId), query, map);
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public void update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        update(null, site, id, xml, ignoreRootInFieldNames);
    }

    @Override
    public void update(String indexId, String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        if (StringUtils.isEmpty(indexId)) {
            indexId = defaultIndexId;
        }

        String finalId = site + ":" + id;

        // This is done because when a document is updated, and it had children before but not now, the children
        // would be orphaned (SOLR-6096)
        delete(indexId, site, id);

        try {
            SolrInputDocument solrDoc = solrDocumentBuilder.build(site, id, xml, ignoreRootInFieldNames);
            NamedList<Object> response = solrClient.add(indexId, solrDoc).getResponse();

            if (logger.isDebugEnabled()) {
                logger.debug(getSuccessfulMessage(indexId, finalId, "Update", response));
            }
        } catch (SolrDocumentBuildException e) {
            throw new SearchException(indexId, "Unable to build Solr document for " + finalId, e);
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(indexId, "Unable to execute update for " + finalId, e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error executing update for " + finalId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(String site, String id) throws SearchException {
        delete(null, site, id);
    }

    @Override
    public void delete(String indexId, String site, String id) throws SearchException {
        if (StringUtils.isEmpty(indexId)) {
            indexId = defaultIndexId;
        }

        String finalId = site + ":" + id;
        String query = getDeleteQuery(finalId);
        NamedList<Object> response;

        try {
            if (StringUtils.isNotEmpty(query)) {
                response = solrClient.deleteByQuery(indexId, query).getResponse();

                if (logger.isDebugEnabled()) {
                    logger.debug(getSuccessfulMessage(indexId, query, "Delete", response));
                }
            } else {
                response = solrClient.deleteById(indexId, finalId).getResponse();

                if (logger.isDebugEnabled()) {
                    logger.debug(getSuccessfulMessage(indexId, finalId, "Delete", response));
                }
            }
        } catch (SolrServerException | IOException e) {
            if (StringUtils.isNotEmpty(query)) {
                throw new SearchServerException(indexId, "Unable to execute delete for " + query, e);
            } else {
                throw new SearchServerException(indexId, "Unable to execute delete for " + finalId, e);
            }
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(query)) {
                throw new SearchException(indexId, "Error executing delete for " + query, e);
            } else {
                throw new SearchException(indexId, "Error executing delete for " + finalId, e);
            }
        }
    }

    @Override
    public void updateContent(String site, String id, File file) throws SearchException {
        updateContent(null, site, id, file, null);
    }

    @Override
    public void updateContent(String indexId, String site, String id, File file) throws SearchException {
        updateContent(indexId, site, id, file, null);
    }

    @Override
    public void updateContent(String site, String id, File file,
                              Map<String, List<String>> additionalFields) throws SearchException {
        updateContent(null, site, id, file, additionalFields);
    }

    @Override
    public void updateContent(String indexId, String site, String id, File file,
                              Map<String, List<String>> additionalFields) throws SearchException {
        if (StringUtils.isEmpty(indexId)) {
            indexId = defaultIndexId;
        }

        String finalId = site + ":" + id;
        String fileName = FilenameUtils.getName(id);
        String contentType = mimeTypesMap.getContentType(fileName);
        ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(SOLR_CONTENT_STREAM_UPDATE_URL);
        NamedList<Object> response;

        try {
            ModifiableSolrParams params = solrDocumentBuilder.buildParams(site, id, ExtractingParams.LITERALS_PREFIX,
                                                                          null, additionalFields);
            params.set(ExtractingParams.LITERALS_PREFIX + fileNameFieldName, fileName);

            request.setParams(params);
            request.addFile(file, contentType);
            request.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

            response = solrClient.request(request, indexId);
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(indexId, "Unable to execute update file for " + finalId, e);
        } catch (Exception e) {
            logger.warn("{} Unable to update file through content stream request: {}. Attempting to perform just " +
                        "the metadata update", getIndexPrefix(indexId), e.getMessage());

            SolrInputDocument inputDocument = solrDocumentBuilder.build(site, id, additionalFields);
            inputDocument.setField(fileNameFieldName, fileName);

            try {
                response = solrClient.add(indexId, inputDocument).getResponse();
            } catch (SolrServerException | IOException e2) {
                throw new SearchServerException(indexId, "Unable to execute update file for " + finalId, e2);
            } catch (Exception e1) {
                throw new SearchException(indexId, e1.getMessage(), e1);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(getSuccessfulMessage(indexId, finalId, "Update file", response));
        }
    }

    @Override
    public void updateContent(String site, String id, Content content) throws SearchException {
        throw new UnsupportedOperationException("Only use updateContent methods that receive a file");
    }

    @Override
    public void updateContent(String indexId, String site, String id, Content content) throws SearchException {
        throw new UnsupportedOperationException("Only use updateContent methods that receive a file");
    }

    @Override
    public void updateContent(String site, String id, Content content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        throw new UnsupportedOperationException("Only use updateContent methods that receive a file");
    }

    @Override
    public void updateContent(String indexId, String site, String id, Content content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        throw new UnsupportedOperationException("Only use updateContent methods that receive a file");
    }

    public void commit() throws SearchException {
        commit(null);
    }

    @Override
    public void commit(String indexId) throws SearchException {
        if (StringUtils.isEmpty(indexId)) {
            indexId = defaultIndexId;
        }

        try {
            NamedList<Object> response = solrClient.commit(indexId).getResponse();

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%sCommit successful: %s", getIndexPrefix(indexId), response));
            }
        } catch (SolrServerException | IOException e) {
            throw new SearchException(indexId, "Unable to execute commit", e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error executing commit", e);
        }
    }

    protected SolrParams toActualSolrQuery(QueryParams queryParams) {
        return new ModifiableSolrParams(queryParams.getParams());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> toMap(NamedList<Object> namedList) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, Object> entry : namedList) {
            Object valueInMap = map.get(entry.getKey());
            // If we have already a value in the map for the same key, that means the key has multiple values,
            // so group them in a list.
            if (valueInMap != null) {
                List<Object> group;

                if (valueInMap instanceof List) {
                    group = (List<Object>)valueInMap;
                } else {
                    group = new ArrayList<>();
                    group.add(valueInMap);

                    map.put(entry.getKey(), group);
                }

                group.add(toSerializableValue(entry.getValue()));
            } else {
                map.put(entry.getKey(), toSerializableValue(entry.getValue()));
            }
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    protected Object toSerializableValue(Object namedListValue) {
        if (namedListValue instanceof NamedList) {
            // The value can also be a NamedList, so convert it to map.
            return toMap((NamedList<Object>)namedListValue);
        } else if (namedListValue instanceof SolrDocumentList) {
            // If the value is a SolrDocumentList, copy the list attributes to a map
            SolrDocumentList docList = (SolrDocumentList)namedListValue;
            Map<String, Object> docListMap = new HashMap<String, Object>(4);

            docListMap.put(DOCUMENT_LIST_START_PROPERTY_NAME, docList.getStart());
            docListMap.put(DOCUMENT_LIST_NUM_FOUND_PROPERTY_NAME, docList.getNumFound());
            docListMap.put(DOCUMENT_LIST_MAX_SCORE_PROPERTY_NAME, docList.getMaxScore());
            docListMap.put(DOCUMENT_LIST_DOCUMENTS_PROPERTY_NAME, extractDocs(docList));

            return docListMap;
        } else if (namedListValue instanceof List) {
            List<Object> originalList = (List<Object>) namedListValue;
            List<Object> serializableList = new LinkedList<>();
            for(Object originalValue : originalList) {
                serializableList.add(toSerializableValue(originalValue));
            }
            return serializableList;
        } else {
            return namedListValue;
        }
    }

    protected Collection<Map<String, Object>> extractDocs(SolrDocumentList docList) {
        Collection<Map<String, Object>> docs = new ArrayList<>(docList.size());

        for (SolrDocument doc : docList) {
            Map<String, Object> docMap = new LinkedHashMap<>();

            for (Map.Entry<String, Object> field : doc) {
                String name = field.getKey();
                Object value = field.getValue();

                // If the value is an Iterable with a single value, return just that one value. This is done for backwards
                // compatibility since Solr 4 did this for us before
                if (value instanceof Iterable) {
                    Iterator<?> iter = ((Iterable<?>)value).iterator();
                    if (iter.hasNext()) {
                        Object first = iter.next();
                        if (!iter.hasNext()) {
                            value = first;
                        }
                    }
                }

                docMap.put(name, value);
            }

            docs.add(docMap);
        }

        return docs;
    }

    protected String getSuccessfulMessage(String indexId, String idOrQuery, String operation, Object solrResponse) {
        return String.format("%s%s for %s successful: %s", getIndexPrefix(indexId), operation, idOrQuery, solrResponse);
    }

    protected String getIndexPrefix(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "[" + indexId + "] " : "";
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

    protected void addAdditionalFilterQueries(String indexId, SolrQuery solrQuery) {
        if(solrQuery.isDisableAdditionalFilters()) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Additional filters disabled for query {}", getIndexPrefix(indexId), solrQuery);
            }

            return;
        }

        String query = solrQuery.getQuery();
        String[] filterQueries = solrQuery.getFilterQueries();

        for (String additionalFilterQuery : additionalFilterQueries) {
            boolean add = true;

            if (StringUtils.isNotEmpty(query)) {
                if (query.contains(additionalFilterQuery)) {
                    add = false;
                }
            }

            if (ArrayUtils.isNotEmpty(filterQueries)) {
                for (String filterQuery : filterQueries) {
                    if (filterQuery.contains(additionalFilterQuery)) {
                        add = false;
                        break;
                    }
                }
            }

            if (add) {
                solrQuery.addFilterQuery(additionalFilterQuery);
            }
        }
    }

}
