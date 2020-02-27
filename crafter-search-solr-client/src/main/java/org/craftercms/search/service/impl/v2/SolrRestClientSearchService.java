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
package org.craftercms.search.service.impl.v2;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.ResourceAwareSearchService;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.SolrQuery;
import org.craftercms.search.service.utils.ContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.rest.v2.SearchRestApiConstants.*;
import static org.craftercms.search.service.utils.RestClientUtils.*;

/**
 * Solr based REST client implementation of {@link SearchService} for Search REST API v2.
 *
 * @author Alfonso Vásquez
 */
public class SolrRestClientSearchService implements ResourceAwareSearchService<SolrQuery> {

    private static final Logger logger = LoggerFactory.getLogger(SolrRestClientSearchService.class);

    protected static final String[] NON_ADDITIONAL_FIELD_NAMES = {PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_CONTENT};

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    protected String serverUrl;
    protected RestTemplate restTemplate;
    protected Charset charset;

    public SolrRestClientSearchService() {
        charset = DEFAULT_CHARSET;
        restTemplate = createRestTemplate(charset);
    }

    @Required
    public void setServerUrl(String serverUrl) {
        this.serverUrl = StringUtils.stripEnd(serverUrl, "/");
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
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
    public Map<String, Object> search(SolrQuery query) throws SearchException {
        return search(null, query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> search(String indexId, SolrQuery query) throws SearchException {
        String searchUrl = createBaseUrl(URL_SEARCH, indexId);
        searchUrl = UrlUtils.addQueryStringFragment(searchUrl, query.toUrlQueryString());

        try {
            return restTemplate.getForObject(new URI(searchUrl), Map.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + searchUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Search for query " + query + " failed: [" + e.getStatusText() + "] "
                                                + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Search for query " + query + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        update(null, site, id, xml, ignoreRootInFieldNames);
    }

    @Override
    public void update(String indexId, String site, String id, String xml,
                       boolean ignoreRootInFieldNames) throws SearchException {
        String updateUrl = createBaseUrl(URL_UPDATE, indexId);
        updateUrl = addParam(updateUrl, PARAM_SITE, site);
        updateUrl = addParam(updateUrl, PARAM_ID, id);
        updateUrl = addParam(updateUrl, PARAM_IGNORE_ROOT_IN_FIELD_NAMES, ignoreRootInFieldNames);

        try {
            MediaType contentType = new MediaType(MediaType.TEXT_XML, charset);
            RequestEntity request = RequestEntity.post(new URI(updateUrl)).contentType(contentType).body(xml);
            Result result = restTemplate.exchange(request, Result.class).getBody();

            logger.debug("Result of {}: {}", updateUrl, result);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + updateUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Update for XML '" + id + "' failed: [" + e.getStatusText() + "] "
                                                + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Update for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public void delete(String site, String id) throws SearchException {
        delete(null, site, id);
    }

    @Override
    public void delete(String indexId, String site, String id) throws SearchException {
        String deleteUrl = createBaseUrl(URL_DELETE, indexId);
        deleteUrl = addParam(deleteUrl, PARAM_SITE, site);
        deleteUrl = addParam(deleteUrl, PARAM_ID, id);

        try {
            Result result = restTemplate.postForObject(new URI(deleteUrl), null, Result.class);

            logger.debug("Result of {}: {}", deleteUrl, result);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + deleteUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Delete for XML '" + id + "' failed: [" + e.getStatusText() + "] "
                                                + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Delete for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public void commit() throws SearchException {
        commit(null);
    }

    @Override
    public void commit(String indexId) throws SearchException {
        String commitUrl = createBaseUrl(URL_COMMIT, indexId);

        try {
            Result result = restTemplate.postForObject(new URI(commitUrl), null, Result.class);

            logger.debug("Result of {}: {}", commitUrl, result);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + commitUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Commit failed: [" + e.getStatusText() + "] " +
                                                e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Commit failed: " + e.getMessage(), e);
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
    public void updateContent(String site, String id, File file, Map<String, List<String>> additionalFields) throws SearchException {
        updateContent(null, site, id, file, additionalFields);
    }

    @Override
    public void updateContent(String indexId, String site, String id, File file,
                              Map<String, List<String>> additionalFields) throws SearchException {
        updateContent(indexId, site, id, new FileSystemResource(file), additionalFields);
    }

    @Override
    public void updateContent(String site, String id, Content content) throws SearchException {
        updateContent(null, site, id, content, null);
    }

    @Override
    public void updateContent(String indexId, String site, String id, Content content) throws SearchException {
        updateContent(indexId, site, id, content, null);
    }

    @Override
    public void updateContent(String site, String id, Content content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        updateContent(null, site, id, content, additionalFields);
    }

    @Override
    public void updateContent(String indexId, String site, String id, Content content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        String filename = FilenameUtils.getName(id);
        Resource resource = new ContentResource(content, filename);

        updateContent(indexId, site, id, resource, additionalFields);
    }

    @SuppressWarnings("unchecked")
    public void updateContent(String indexId, String site, String id, Resource resource,
                                 Map<String, List<String>> additionalFields) throws SearchException {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        if (StringUtils.isNotEmpty(indexId)) {
            parts.set(PARAM_INDEX_ID, indexId);
        }
        parts.set(PARAM_SITE, site);
        parts.set(PARAM_ID, id);
        parts.set(PARAM_CONTENT, resource);

        addAdditionalFieldsToMultiPartRequest(additionalFields, parts, NON_ADDITIONAL_FIELD_NAMES, null);

        String updateUrl = createBaseUrl(URL_UPDATE_CONTENT);

        try {
            Result result = restTemplate.postForObject(new URI(updateUrl), parts, Result.class);

            logger.debug("Result of {}: {}", updateUrl, result);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + updateUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Update for content '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Update for content '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    protected String createBaseUrl(String serviceUrl) {
        return UrlUtils.concat(serverUrl, URL_ROOT, serviceUrl);
    }

    protected String createBaseUrl(String serviceUrl, String indexId) {
        String url = createBaseUrl(serviceUrl);

        if (StringUtils.isNotEmpty(indexId)) {
            url = addParam(url, PARAM_INDEX_ID, indexId);
        }

        return url;
    }

}
