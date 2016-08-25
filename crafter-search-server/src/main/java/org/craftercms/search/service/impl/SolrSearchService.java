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
package org.craftercms.search.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.activation.FileTypeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.SolrDocumentBuilder;
import org.craftercms.search.utils.SolrServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.craftercms.search.service.SearchRestConstants.SOLR_CONTENT_STREAM_UPDATE_URL;

/**
 * Implementation of {@link SearchService} using Solr as the underlying search engine.
 *
 * @author Michael Chen
 * @author Alfonso Vasquez
 * @author Dejan Brkic
 */
public class SolrSearchService implements SearchService {

    public static final String DEFAULT_FILE_NAME_FIELD_NAME = "file-name";

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchService.class);

    /**
     * The file name field name (default is file-name).
     */
    protected String fileNameFieldName;
    /**
     * Factory that creates index specific SolrServers.
     */
    protected SolrServerFactory solrServerFactory;
    /**
     * The Solr document builder, to build Solr documents from generic XML documents.
     */
    protected SolrDocumentBuilder solrDocumentBuilder;
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

    public SolrSearchService() {
        fileNameFieldName = DEFAULT_FILE_NAME_FIELD_NAME;
    }

    /**
     * Sets the file name field (default is file-name).
     */
    public void setFileNameFieldName(String fileNameFieldName) {
        this.fileNameFieldName = fileNameFieldName;
    }

    /**
     * Sets the factory that creates index specific SolrServers.
     */
    @Required
    public void setSolrServerFactory(SolrServerFactory solrServerFactory) {
        this.solrServerFactory = solrServerFactory;
    }

    /**
     * Sets the Solr document builder, to build Solr documents from generic XML documents.
     */
    @Required
    public void setSolrDocumentBuilder(SolrDocumentBuilder solrDocumentBuilder) {
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
     * {@inheritDoc}
     */
    public Map<String, Object> search(Query query) {
        return search(null, query);
    }

    @Override
    public Map<String, Object> search(String indexId, Query query) throws SearchException {
        logger.info("{}Executing query {}", getIndexPrefix(indexId), query);

        SolrResponse response;
        try {
            response = getSolrServer(indexId).query(toSolrQuery((QueryParams)query));
        } catch (SolrServerException e) {
            throw new SearchException(indexId, "Search for query " + query + " failed", e);
        }

        // Solr search result is a List<Map.Entry<String,Object>>, where every entry is a (name,value) pair,
        // and there can be
        // duplicate names in the list.
        NamedList<Object> list = response.getResponse();
        // Convert this list into a ,ap where values of the same name are grouped into a list.
        Map<String, Object> map = toMap(list);

        if (logger.isDebugEnabled()) {
            logger.debug("{}Response for query {}: {}", getIndexPrefix(indexId), query, map);
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        return update(null, site, id, xml, ignoreRootInFieldNames);
    }

    @Override
    public String update(String indexId, String site, String id, String xml,
                         boolean ignoreRootInFieldNames) throws SearchException {
        String finalId = site + ":" + id;

        // This is done because when a document is updated, and it had children before but not now, the children
        // will be orphaned (SOLR-6096)
        delete(indexId, site, id);

        try {
            SolrInputDocument solrDoc = solrDocumentBuilder.build(site, id, xml, ignoreRootInFieldNames);
            NamedList<Object> response = getSolrServer(indexId).add(solrDoc).getResponse();

            String msg = getSuccessfulMessage(indexId, finalId, "Update", response);

            logger.info(msg);

            return msg;
        } catch (SolrDocumentBuildException e) {
            throw new SearchException(indexId, "Unable to build Solr document for " + finalId, e);
        } catch (IOException e) {
            throw new SearchException(indexId,  "I/O error while executing update for " + finalId, e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error executing update for " + finalId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String delete(String site, String id) throws SearchException {
        return delete(null, site, id);
    }

    @Override
    public String delete(String indexId, String site, String id) throws SearchException {
        String finalId = site + ":" + id;
        String query = getDeleteQuery(finalId);
        NamedList<Object> response;
        String msg;

        try {
            if (StringUtils.isNotEmpty(query)) {
                response = getSolrServer(indexId).deleteByQuery(query).getResponse();
                msg = getSuccessfulMessage(indexId, query, "Delete", response);
            } else {
                response = getSolrServer(indexId).deleteById(finalId).getResponse();
                msg = getSuccessfulMessage(indexId, finalId, "Delete", response);
            }

            logger.info(msg);

            return msg;
        } catch (IOException e) {
            if (StringUtils.isNotEmpty(query)) {
                throw new SearchException(indexId, "I/O error while executing delete for " + query, e);
            } else {
                throw new SearchException(indexId, "I/O error while executing delete for " + finalId, e);
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
    @Deprecated
    public String updateDocument(String site, String id, File document) throws SearchException {
        return updateFile(site, id, document);
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        return updateFile(site, id, document, getAdditionalFieldMapAsMultiValueMap(additionalFields));
    }

    @Override
    public String updateFile(String site, String id, File file) throws SearchException {
        return updateFile(null, site, id, file, null);
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file) throws SearchException {
        return updateFile(indexId, site, id, file, null);
    }

    @Override
    public String updateFile(String site, String id, File file,
                             Map<String, List<String>> additionalFields) throws SearchException {
        return updateFile(null, site, id, file, additionalFields);
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file,
                             Map<String, List<String>> additionalFields) throws SearchException {
        String finalId = site + ":" + id;
        String fileName = FilenameUtils.getName(id);
        FileTypeMap mimeTypesMap = new ConfigurableMimeFileTypeMap();
        String contentType = mimeTypesMap.getContentType(fileName);
        NamedList<Object> response;

        ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(SOLR_CONTENT_STREAM_UPDATE_URL);
        try {
            ModifiableSolrParams params = solrDocumentBuilder.buildParams(site, id, ExtractingParams.LITERALS_PREFIX,
                                                                          null, additionalFields);
            params.set(ExtractingParams.LITERALS_PREFIX + fileNameFieldName, fileName);

            request.setParams(params);
            request.addFile(file, contentType);
            request.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

            response = getSolrServer(indexId).request(request);
        } catch (SolrServerException e) {
            logger.warn("{}Unable to update file through content stream request: {}. Attempting to perform just " +
                        "the metadata update", getIndexPrefix(indexId), e.getMessage());

            try {
                SolrInputDocument inputDocument = solrDocumentBuilder.build(site, id, additionalFields);
                inputDocument.setField(fileNameFieldName, fileName);

                response = getSolrServer(indexId).add(inputDocument).getResponse();
            } catch (IOException e1) {
                throw new SearchException(indexId, "I/O error while executing update file for " + finalId, e1);
            } catch (SolrServerException e1) {
                throw new SearchException(indexId, e1.getMessage(), e1);
            }
        } catch (IOException e) {
            throw new SearchException(indexId, "I/O error while executing update file for " + finalId, e);
        }

        String msg = getSuccessfulMessage(indexId, finalId, "Update file", response);

        logger.info(msg);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    public String commit() throws SearchException {
        return commit(null);
    }

    @Override
    public String commit(String indexId) throws SearchException {
        try {
            NamedList<Object> response = getSolrServer(indexId).commit().getResponse();

            String msg = String.format("%sCommit successful: %s", getIndexPrefix(indexId), response);

            logger.info(msg);

            return msg;
        } catch (IOException e) {
            throw new SearchException(indexId, "I/O error while executing commit", e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error executing commit", e);
        }
    }

    protected SolrParams toSolrQuery(QueryParams queryParams) {
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
        // The value can also be a NamedList, so convert it to map.
        if (namedListValue instanceof NamedList) {
            return toMap((NamedList<Object>)namedListValue);
        }
        // If the value is a SolrDocumentList, copy the list attributes to a map
        if (namedListValue instanceof SolrDocumentList) {
            SolrDocumentList docList = (SolrDocumentList)namedListValue;
            Map<String, Object> docListMap = new HashMap<String, Object>(4);

            docListMap.put("start", docList.getStart());
            docListMap.put("numFound", docList.getNumFound());
            docListMap.put("maxScore", docList.getMaxScore());
            docListMap.put("documents", new ArrayList<>(docList));

            return docListMap;
        }

        return namedListValue;
    }

    protected Map<String, List<String>> getAdditionalFieldMapAsMultiValueMap(Map<String, String> originalMap) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>(originalMap.size());
        for (Map.Entry<String, String> entry : originalMap.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            if (!fieldName.matches(multiValueIgnorePattern)) {
                multiValueMap.put(fieldName, Arrays.asList(fieldValue.split(multiValueSeparator)));
            } else {
                multiValueMap.add(fieldName, fieldValue);
            }
        }

        return multiValueMap;
    }

    protected SolrServer getSolrServer(String indexId) {
        return solrServerFactory.getSolrServer(indexId);
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

}
