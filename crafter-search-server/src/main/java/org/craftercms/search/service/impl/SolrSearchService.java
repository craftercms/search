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
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
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
import org.springframework.beans.factory.annotation.Required;
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

    public static final String FILE_NAME_FIELD_NAME = "file-name";

    private static final Log logger = LogFactory.getLog(SolrSearchService.class);

    /**
     * The endpoint to the Solr server.
     */
    protected SolrServer solrServer;
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
     * Sets the endpoint to the Solr server.
     */
    @Required
    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
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
     * {@inheritDoc}
     */
    public Map<String, Object> search(Query query) {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching: " + query);
        }

        SolrResponse response;
        try {
            response = solrServer.query(toSolrQuery((QueryParams)query));
        } catch (SolrServerException e) {
            throw new SearchException("Search for query " + query + " failed", e);
        }

        // Solr search result is a List<Map.Entry<String,Object>>, where every entry is a (name,value) pair,
        // and there can be
        // duplicate names in the list.
        NamedList<Object> list = response.getResponse();
        // Convert this list into a ,ap where values of the same name are grouped into a list.
        Map<String, Object> map = toMap(list);

        if (logger.isDebugEnabled()) {
            logger.debug("Response for query " + query + ": " + map);
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        String finalId = site + ":" + id;

        try {
            SolrInputDocument solrDoc = solrDocumentBuilder.build(site, id, xml, ignoreRootInFieldNames);
            UpdateResponse response = solrServer.add(solrDoc);

            if (logger.isDebugEnabled()) {
                logger.debug("Update response for '" + finalId + "': " + response.getResponse());
            }

            return "Successfully updated '" + finalId + "'";
        } catch (SolrDocumentBuildException e) {
            throw new SearchException("Unable to build Solr document for '" + finalId + "'", e);
        } catch (IOException e) {
            throw new SearchException("I/O error while executing update for '" + finalId + "'", e);
        } catch (Exception e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String delete(String site, String id) throws SearchException {
        String finalId = site + ":" + id;


        try {
            UpdateResponse response = solrServer.deleteById(finalId);

            if (logger.isDebugEnabled()) {
                logger.debug("Delete response for '" + finalId + "': " + response.getResponse());
            }

            return "Successfully deleted '" + finalId + "'";
        } catch (IOException e) {
            throw new SearchException("I/O error while executing delete for '" + finalId + "'", e);
        } catch (Exception e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String commit() throws SearchException {
        try {
            UpdateResponse response = solrServer.commit();

            if (logger.isDebugEnabled()) {
                logger.debug("Commit response: " + response.getResponse());
            }

            return "Successfully committed";
        } catch (IOException e) {
            throw new SearchException("I/O error while executing commit", e);
        } catch (Exception e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    protected SolrParams toSolrQuery(QueryParams queryParams) {
        return new ModifiableSolrParams(queryParams.getParams());
    }

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
                    group = new ArrayList<Object>();
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
            docListMap.put("documents", new ArrayList<SolrDocument>(docList));

            return docListMap;
        }

        return namedListValue;
    }

    @Override
    public String updateDocument(String site, String id, File document) throws SearchException {
        return updateDocument(site, id, document, null);
    }

    @Override
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        String finalId = site + ":" + id;
        String fileName = (new File(finalId)).getName();
        Map<String, List<String>> multiValueAdditionalFields = getAdditionalFieldMapAsMultiValueMap(additionalFields);
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        String contentType = mimeTypesMap.getContentType(document.getName());

        ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(SOLR_CONTENT_STREAM_UPDATE_URL);
        try {
            request.addFile(document, contentType);

            ModifiableSolrParams params = solrDocumentBuilder.buildParams(site, id, ExtractingParams.LITERALS_PREFIX,
                                                                          null, multiValueAdditionalFields);
            params.set(ExtractingParams.LITERALS_PREFIX + FILE_NAME_FIELD_NAME, fileName);

            request.setParams(params);
            request.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

            solrServer.request(request);
        } catch (SolrServerException e) {
            logger.warn("Unable to update document through content stream request: " + e.getMessage() + ". " +
                        "Attempting to perform just the metadata update");

            try {
                SolrInputDocument inputDocument = solrDocumentBuilder.build(site, id, multiValueAdditionalFields);
                inputDocument.setField(FILE_NAME_FIELD_NAME, fileName);

                solrServer.add(inputDocument);
            } catch (IOException e1) {
                throw new SearchException("I/O error while executing update document for '" + finalId + "'", e1);
            } catch (SolrServerException e1) {
                throw new SearchException(e1.getMessage(), e1);
            }
        } catch (IOException e) {
            throw new SearchException("I/O error while executing update document for '" + finalId + "'", e);
        }

        return "Successfully updated document '" + id + "'";
    }

    protected Map<String, List<String>> getAdditionalFieldMapAsMultiValueMap(Map<String, String> originalMap) {
        MultiValueMap multiValueMap = new LinkedMultiValueMap(originalMap.size());
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

}
