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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.StringHttpMessageConverterExtended;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_DOCUMENT;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_ID;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_SITE;
import static org.craftercms.search.service.SearchRestConstants.URL_COMMIT;
import static org.craftercms.search.service.SearchRestConstants.URL_DELETE;
import static org.craftercms.search.service.SearchRestConstants.URL_ROOT;
import static org.craftercms.search.service.SearchRestConstants.URL_SEARCH;
import static org.craftercms.search.service.SearchRestConstants.URL_UPDATE;
import static org.craftercms.search.service.SearchRestConstants.URL_UPDATE_DOCUMENT;

/**
 * Client implementation of {@link SearchService}, which uses REST to communicate with the server
 *
 * @author Alfonso Vásquez
 */
public class RestClientSearchService implements SearchService {

    private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", RestTemplate.class
        .getClassLoader());

    private static final boolean jacksonPresent =
        ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", RestTemplate.class.getClassLoader()) &&
        ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", RestTemplate.class.getClassLoader());

    private static boolean romePresent = ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", RestTemplate.class
        .getClassLoader());

    protected String serverUrl;
    protected RestTemplate restTemplate;

    public RestClientSearchService() {
        restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverterExtended(Charset.forName(CharEncoding.UTF_8)));
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new SourceHttpMessageConverter());
        messageConverters.add(new XmlAwareFormHttpMessageConverter());

        if (jaxb2Present) {
            messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        }
        if (jacksonPresent) {
            messageConverters.add(new MappingJacksonHttpMessageConverter());
        }
        if (romePresent) {
            messageConverters.add(new AtomFeedHttpMessageConverter());
            messageConverters.add(new RssChannelHttpMessageConverter());
        }

        restTemplate.setMessageConverters(messageConverters);
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
        String searchUrl = serverUrl + URL_ROOT + URL_SEARCH + "?" + query.toQueryString();

        try {
            return restTemplate.getForObject(new URI(searchUrl), Map.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + searchUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(
                "Search for query " + query + " failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Search for query " + query + " failed: " + e.getMessage(), e);
        }
    }

    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        String updateUrl = serverUrl + URL_ROOT + URL_UPDATE + "?" + REQUEST_PARAM_SITE + "=" + site + "&" +
                           REQUEST_PARAM_ID +
                           "=" + id + "&" + REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES + "=" + ignoreRootInFieldNames;

        try {
            return restTemplate.postForObject(new URI(updateUrl), xml, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + updateUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(
                "Update for XML '" + id + "' failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Update for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public String delete(String site, String id) throws SearchException {
        String deleteUrl = serverUrl + URL_ROOT + URL_DELETE + "?" + REQUEST_PARAM_SITE + "=" + site + "&" +
                           REQUEST_PARAM_ID + "=" + id;

        try {
            return restTemplate.postForObject(new URI(deleteUrl), null, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + deleteUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(
                "Delete for XML '" + id + "' failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Delete for XML '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    public String commit() throws SearchException {
        String commitUrl = serverUrl + URL_ROOT + URL_COMMIT;

        try {
            return restTemplate.postForObject(new URI(commitUrl), null, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + commitUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException("Commit failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Commit failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateDocument(String site, String id, File document) throws SearchException {
        return updateDocument(site, id, document, null);
    }

    @Override
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        FileSystemResource fsrDoc = new FileSystemResource(document);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();

        form.add(REQUEST_PARAM_SITE, site);
        form.add(REQUEST_PARAM_ID, id);
        form.add(REQUEST_PARAM_DOCUMENT, fsrDoc);

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

        String updateDocumentUrl = serverUrl + URL_ROOT + URL_UPDATE_DOCUMENT;

        try {
            return restTemplate.postForObject(new URI(updateDocumentUrl), form, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException("Invalid URI: " + updateDocumentUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(
                "Update for document '" + id + "' failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException("Update for document '" + id + "' failed: " + e.getMessage(), e);
        }
    }

}
