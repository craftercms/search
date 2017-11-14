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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.rest.SearchRestApiConstants.PARAM_CONTENT;
import static org.craftercms.search.rest.SearchRestApiConstants.PARAM_ID;
import static org.craftercms.search.rest.SearchRestApiConstants.PARAM_IGNORE_ROOT_IN_FIELD_NAMES;
import static org.craftercms.search.rest.SearchRestApiConstants.PARAM_INDEX_ID;
import static org.craftercms.search.rest.SearchRestApiConstants.PARAM_SITE;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_COMMIT;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_DELETE;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_ROOT;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_SEARCH;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_UPDATE;
import static org.craftercms.search.rest.SearchRestApiConstants.URL_UPDATE_CONTENT;

/**
 * Abstract class that contains the base client implementation of {@link SearchService}, which uses REST to communicate with the server
 *
 * @author Alfonso Vásquez
 */
public abstract class AbstractRestClientSearchService<T extends Query> implements SearchService<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRestClientSearchService.class);

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    protected String serverUrl;
    protected RestTemplate restTemplate;
    protected Charset charset;

    public AbstractRestClientSearchService() {
        charset = DEFAULT_CHARSET;
        restTemplate = createRestTemplate();
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

    public Map<String, Object> search(Query query) throws SearchException {
        return search(null, query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> search(String indexId, Query query) throws SearchException {
        String searchUrl = createBaseUrl(URL_SEARCH, indexId);
        searchUrl = UrlUtils.addQueryStringFragment(searchUrl, query.toUrlQueryString());

        try {
            return restTemplate.getForObject(new URI(searchUrl), Map.class);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + searchUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Search for query " + query + " failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Search for query " + query + " failed: " + e.getMessage(), e);
        }
    }

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
            throw new SearchException(indexId, "Update for XML '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString(), e);
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
            throw new SearchException(indexId, "Delete for XML '" + id + "' failed: [" + e.getStatusText() + "] " +
                                               e.getResponseBodyAsString(), e);
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
            throw new SearchException(indexId, "Commit failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString(), e);
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
    protected void updateContent(String indexId, String site, String id, Resource resource,
                                 Map<String, List<String>> additionalFields) throws SearchException {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        if (StringUtils.isNotEmpty(indexId)) {
            parts.set(PARAM_INDEX_ID, indexId);
        }
        parts.set(PARAM_SITE, site);
        parts.set(PARAM_ID, id);
        parts.set(PARAM_CONTENT, resource);

        if (MapUtils.isNotEmpty(additionalFields)) {
            for (Map.Entry<String, List<String>> additionalField : additionalFields.entrySet()) {
                String fieldName = additionalField.getKey();

                if (fieldName.equals(PARAM_INDEX_ID) ||
                    fieldName.equals(PARAM_SITE) ||
                    fieldName.equals(PARAM_ID) ||
                    fieldName.endsWith(PARAM_CONTENT)) {
                    throw new SearchException(String.format("An additional field shouldn't have the following names: %s, %s, %s, %s",
                                                            PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_CONTENT));
                }

                parts.put(fieldName, (List) additionalField.getValue());
            }
        }

        String updateUrl = createBaseUrl(URL_UPDATE_CONTENT);

        try {
            Result result = restTemplate.postForObject(new URI(updateUrl), parts, Result.class);

            logger.debug("Result of {}: {}", updateUrl, result);
        } catch (URISyntaxException e) {
            throw new SearchException(indexId, "Invalid URI: " + updateUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(indexId, "Update for content '" + id + "' failed: [" + e.getStatusText() + "] " +
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

    protected String addParam(String url, String name, Object value) {
        try {
            return UrlUtils.addParam(url, name, value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen, UTF-8 is a valid encoding
            throw new RuntimeException();
        }
    }

    protected RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Set charset of the String part converter of the FormHttpMessageConverter.
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof FormHttpMessageConverter) {
                StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(charset);
                stringHttpMessageConverter.setWriteAcceptCharset(false);

                List<HttpMessageConverter<?>> partConverters = new ArrayList<>();
                partConverters.add(new ByteArrayHttpMessageConverter());
                partConverters.add(stringHttpMessageConverter);
                partConverters.add(new ResourceHttpMessageConverter());

                ((FormHttpMessageConverter) converter).setPartConverters(partConverters);
            }
        }

        return restTemplate;
    }

    protected static class ContentResource extends AbstractResource {

        private Content content;
        private String filename;

        public ContentResource(Content content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        @Override
        public String getDescription() {
            return content.toString();
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long contentLength() throws IOException {
            return content.getLength();
        }

        @Override
        public long lastModified() throws IOException {
            return content.getLastModified();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return content.getInputStream();
        }

    }

}
