/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.requests.CreateIndexRequest;
import org.craftercms.search.rest.requests.DeleteIndexRequest;
import org.craftercms.search.service.AdminService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.rest.AdminRestApiConstants.URL_CREATE_INDEX;
import static org.craftercms.search.rest.AdminRestApiConstants.URL_DELETE_INDEX;
import static org.craftercms.search.rest.AdminRestApiConstants.URL_GET_INDEX_INFO;
import static org.craftercms.search.rest.AdminRestApiConstants.URL_ROOT;

/**
 * Created by alfonsovasquez on 2/9/17.
 */
public class RestClientAdminService implements AdminService {

    protected String serverUrl;
    protected RestTemplate restTemplate;

    public RestClientAdminService() {
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

    @Override
    public void createIndex(String id) throws SearchException {
        String createUrl = createBaseUrl(URL_CREATE_INDEX);
        CreateIndexRequest request = new CreateIndexRequest(id);

        try {
            restTemplate.postForObject(new URI(createUrl), request, String.class);
        } catch (URISyntaxException e) {
            throw new SearchException(id, "Invalid URI: " + createUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(id, "Create index '" + id + "' failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(id, "Create index '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getIndexInfo(String id) throws SearchException {
        String getUrl = createBaseUrl(URL_GET_INDEX_INFO);

        try {
            return restTemplate.getForObject(getUrl, Map.class, id);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(id, "Get info for index '" + id + "' failed: [" + e.getStatusText() + "] " +
                                          e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(id, "Get info for index '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(String id, IndexDeleteMode mode) throws SearchException {
        String deleteUrl = createBaseUrl(URL_DELETE_INDEX);
        DeleteIndexRequest request = new DeleteIndexRequest();

        request.setDeleteMode(mode);

        try {
            restTemplate.postForObject(deleteUrl, request, String.class, id);
        } catch (HttpStatusCodeException e) {
            throw new SearchException(id, "Delete index '" + id + "' failed: [" + e.getStatusText() + "] " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new SearchException(id, "Delete index '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    protected String createBaseUrl(String serviceUrl) {
        return UrlUtils.concat(serverUrl, URL_ROOT, serviceUrl);
    }

}
