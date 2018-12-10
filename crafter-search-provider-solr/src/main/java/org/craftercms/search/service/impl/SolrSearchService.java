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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SearchServerException;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.SolrDocumentBuilder;
import org.craftercms.search.v3.service.internal.QueryBuilder;
import org.craftercms.search.v3.service.internal.SearchProvider;
import org.craftercms.search.v3.service.internal.impl.SolrQueryBuilder;
import org.craftercms.search.v3.service.internal.impl.AbstractSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.search.service.impl.SolrUtils.extractDocs;

/**
 * Implementation of {@link SearchService} using Solr as the underlying search engine.
 *
 * @author Michael Chen
 * @author Alfonso Vasquez
 * @author Dejan Brkic
 */
public class SolrSearchService extends AbstractSearchService<SolrQuery, org.apache.solr.client.solrj.SolrQuery,
    QueryResponse> {

    public static final String SOLR_CONTENT_STREAM_UPDATE_URL = "/update/extract";

    public static final String DOCUMENT_LIST_START_PROPERTY_NAME = "start";
    public static final String DOCUMENT_LIST_NUM_FOUND_PROPERTY_NAME = "numFound";
    public static final String DOCUMENT_LIST_MAX_SCORE_PROPERTY_NAME = "maxScore";
    public static final String DOCUMENT_LIST_DOCUMENTS_PROPERTY_NAME = "documents";

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchService.class);

    /**
     * The Solr client used to execute requests against a Solr server.
     */
    protected SolrClient solrClient;
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

    @Override
    public SolrQuery createQuery() {
        return new SolrQuery();
    }

    @Override
    public SolrQuery createQuery(Map<String, String[]> params) {
        return new SolrQuery(params);
    }

    @Override
    public Map<String, Object> doSearch(String indexId, SolrQuery query) throws SearchException {
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
        return toMap(list);
    }

    @Override
    public void doUpdate(String indexId, String site, String id, String finalId, String xml,
                         boolean ignoreRootInFieldNames) throws SearchException {
        try {
            SolrInputDocument solrDoc = solrDocumentBuilder.build(site, finalId, id, xml, ignoreRootInFieldNames);
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

    @Override
    public void doDelete(String indexId, String finalId, String query) throws SearchException {
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
    public void doUpdateContent(String indexId, String site, String id, String finalId, File file, String fileName,
                                String contentType, Map<String, List<String>> additionalFields) throws SearchException {
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

            SolrInputDocument inputDocument = solrDocumentBuilder.build(site, finalId, id, additionalFields);
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
    public void doCommit(String indexId) throws SearchException {
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
        } else {
            return namedListValue;
        }
    }

    protected String getSuccessfulMessage(String indexId, String idOrQuery, String operation, Object solrResponse) {
        return String.format("%s%s for %s successful: %s", getIndexPrefix(indexId), operation, idOrQuery, solrResponse);
    }

    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchProvider getProvider() {
        return SearchProvider.SOLR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder createQueryBuilder() {
        return new SolrQueryBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected QueryResponse doSearch(final String indexId, final org.apache.solr.client.solrj.SolrQuery request) {
        try {
            return solrClient.query(indexId, request);
        } catch (IOException e) {
            logger.error("Unable to execute search for " + request, e);
            throw new SearchServerException(indexId, "Unable to execute search for " + request, e);
        } catch (Exception e) {
            logger.error("Search for " + request + " failed", e);
            throw new SearchException(indexId, "Search for " + request + " failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map nativeSearch(final String indexId, final Map params) {
        ModifiableSolrParams query = new ModifiableSolrParams();
        params.forEach((key, value) -> {
            if(value instanceof String || value instanceof Number || value instanceof Boolean) {
                query.add((String) key, value.toString());
            } else if(value instanceof List) {
                List list = (List) value;
                list.forEach(val -> query.add((String) key, val.toString()));
            } else {
                logger.warn("Incompatible property '{}' with value '{}'", key, value);
            }
        });
        try {
            return toMap(solrClient.query(indexId, query).getResponse());
        } catch (IOException e) {
            logger.error("Unable to execute search for " + params, e);
            throw new SearchServerException(indexId, "Unable to execute search for " + params, e);
        } catch (Exception e) {
            logger.error("Search for " + params + " failed", e);
            throw new SearchException(indexId, "Search for " + params + " failed", e);
        }
    }

}
