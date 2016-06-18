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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.service.SearchRestConstants.*;

/**
 * Client implementation of {@link SearchService}, which uses REST to communicate with the server
 *
 * @author Alfonso Vásquez
 */
public class RestClientSearchService implements SearchService {

    protected String serverUrl;
    protected RestTemplate restTemplate;

    public RestClientSearchService() {
        restTemplate = new RestTemplate();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @Required
    public void setServerUrl(String serverUrl) {
        this.serverUrl = StringUtils.stripEnd(serverUrl, "/");
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> search(Query query) throws SearchException {
        return search(null, query);
    }

    @Override
    public Map<String, Object> search(String indexId, Query query) throws SearchException {
        String searchUrl = createBaseUrl(URL_SEARCH, indexId);
        searchUrl = UrlUtils.addQueryStringFragment(searchUrl, query.toQueryString());

        try {
            return restTemplate.getForObject(new URI(searchUrl.toString()), Map.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + searchUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Search for query " + query + " failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(indexId, "Search for query " + query + " failed: " + e.getMessage(), e);
        }
    }

    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        return update(null, site, id, xml, ignoreRootInFieldNames);
    }

    @Override
    public String update(String indexId, String site, String id, String xml,
                         boolean ignoreRootInFieldNames) throws SearchException {
        String updateUrl = createBaseUrl(URL_UPDATE, indexId);
        updateUrl = addParam(updateUrl, REQUEST_PARAM_SITE, site);
        updateUrl = addParam(updateUrl, REQUEST_PARAM_ID, id);
        updateUrl = addParam(updateUrl, REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES, ignoreRootInFieldNames);

        try {
            return restTemplate.postForObject(new URI(updateUrl), xml, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + updateUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Update for XML '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(indexId, "Update for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public String delete(String site, String id) throws SearchException {
        return delete(null, site, id);
    }

    @Override
    public String delete(String indexId, String site, String id) throws SearchException {
        String deleteUrl = createBaseUrl(URL_DELETE, indexId);
        deleteUrl = addParam(deleteUrl, REQUEST_PARAM_SITE, site);
        deleteUrl = addParam(deleteUrl, REQUEST_PARAM_ID, id);

        try {
            return restTemplate.postForObject(new URI(deleteUrl), null, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + deleteUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Delete for XML '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(indexId, "Delete for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public String commit() throws SearchException {
        return commit(null);
    }

    @Override
    public String commit(String indexId) throws SearchException {
        String commitUrl = createBaseUrl(URL_COMMIT, indexId);

        try {
            return restTemplate.postForObject(new URI(commitUrl), null, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + commitUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Commit failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(indexId, "Commit failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document) throws SearchException {
        return updateDocument(site, id, document, null);
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        FileSystemResource fsr = new FileSystemResource(document);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        form.add(REQUEST_PARAM_SITE, site);
        form.add(REQUEST_PARAM_ID, id);
        form.add(REQUEST_PARAM_DOCUMENT, fsr);

        if (MapUtils.isNotEmpty(additionalFields)) {
            for (Map.Entry<String, String> additionalField : additionalFields.entrySet()) {
                String fieldName = additionalField.getKey();

                if (fieldName.equals(REQUEST_PARAM_SITE) ||
                    fieldName.equals(REQUEST_PARAM_ID) ||
                    fieldName.equals(REQUEST_PARAM_DOCUMENT)) {
                    throw new SearchException(String.format("An additional field shouldn't have the " +
                                                            "following names: %s, %s, %s", REQUEST_PARAM_SITE,
                                                            REQUEST_PARAM_ID, REQUEST_PARAM_DOCUMENT));
                }

                form.add(fieldName, additionalField.getValue());
            }
        }

        String updateDocumentUrl = createBaseUrl(URL_UPDATE_DOCUMENT);

        try {
            return restTemplate.postForObject(new URI(updateDocumentUrl), form, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + updateDocumentUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException("Update for document '" + id + "' failed: [" + e.getStatusText() + "] " +
                                      e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Update for document '" + id + "' failed: " + e.getMessage(), e);
        }
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
        FileSystemResource fsr = new FileSystemResource(file);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        if (StringUtils.isNotEmpty(indexId)) {
            form.set(REQUEST_PARAM_INDEX_ID, indexId);
        }
        form.set(REQUEST_PARAM_SITE, site);
        form.set(REQUEST_PARAM_ID, id);
        form.set(REQUEST_PARAM_FILE, fsr);

        if (MapUtils.isNotEmpty(additionalFields)) {
            for (Map.Entry<String, List<String>> additionalField : additionalFields.entrySet()) {
                String fieldName = additionalField.getKey();

                if (fieldName.equals(REQUEST_PARAM_INDEX_ID) ||
                    fieldName.equals(REQUEST_PARAM_SITE) ||
                    fieldName.equals(REQUEST_PARAM_ID) ||
                    fieldName.equals(REQUEST_PARAM_DOCUMENT)) {
                    throw new SearchException(String.format("An additional field shouldn't have the " +
                                                            "following names: %s, %s, %s, %s",
                                                            REQUEST_PARAM_INDEX_ID, REQUEST_PARAM_SITE,
                                                            REQUEST_PARAM_ID, REQUEST_PARAM_DOCUMENT));
                }

                form.put(fieldName, (List) additionalField.getValue());
            }
        }

        String updateFileUrl = createBaseUrl(URL_UPDATE_FILE);

        try {
            return restTemplate.postForObject(new URI(updateFileUrl), form, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + updateFileUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Update for file '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(indexId, "Update for file '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    protected String createBaseUrl(String serviceUrl) {
        return serverUrl + URL_ROOT + serviceUrl;
    }

    protected String createBaseUrl(String serviceUrl, String indexId) {
        String url = createBaseUrl(serviceUrl);

        if (StringUtils.isNotEmpty(indexId)) {
            url = addParam(url, REQUEST_PARAM_INDEX_ID, indexId);
        }

        return url;
    }

    protected String addParam(String url, String name, Object value) {
        try {
            return UrlUtils.addParam(url, name, value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen, UTF-8 is a valid encoding
            throw new RuntimeException();
        }
    }

}
