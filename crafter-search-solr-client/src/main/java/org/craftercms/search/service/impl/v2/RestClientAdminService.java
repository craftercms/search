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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v2.requests.CreateIndexRequest;
import org.craftercms.search.rest.v2.requests.DeleteIndexRequest;
import org.craftercms.search.service.AdminService;
import org.craftercms.search.service.utils.AccessTokenAware;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_CREATE_INDEX;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_DELETE_INDEX;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_GET_INDEX_INFO;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_ROOT;
import static org.craftercms.search.service.utils.RestClientUtils.getSearchException;

/**
 * Created by alfonsovasquez on 2/9/17.
 */
public class RestClientAdminService extends AccessTokenAware implements AdminService {

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
            restTemplate.postForObject(new URI(createUrl), request, Result.class);
        } catch (URISyntaxException e) {
            throw new SearchException(id, "Invalid URI: " + createUrl, e);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(id, "Create index '" + id + "' failed: [" + e.getStatusText() + "] " +
                e.getResponseBodyAsString(), e);
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
            throw getSearchException(id, "Get info for index '" + id + "' failed: [" + e.getStatusText() + "] " +
                                          e.getResponseBodyAsString(), e);
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
            throw getSearchException(id, "Delete index '" + id + "' failed: [" + e.getStatusText() + "] " +
                e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(id, "Delete index '" + id + "' failed: " + e.getMessage(), e);
        }
    }

    protected String createBaseUrl(String serviceUrl) {
        return addTokenIfNeeded(UrlUtils.concat(serverUrl, URL_ROOT, serviceUrl));
    }

}
