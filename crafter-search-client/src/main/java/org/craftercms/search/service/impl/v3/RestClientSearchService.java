/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.service.impl.v3;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.service.impl.SolrQuery;
import org.craftercms.search.service.impl.v2.SolrRestClientSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;

import static org.craftercms.search.rest.v3.SearchRestApiConstants.*;
import static org.craftercms.search.service.utils.RestClientUtils.addAdditionalFieldsToMultiPartRequest;
import static org.craftercms.search.service.utils.RestClientUtils.addParam;
import static org.craftercms.search.service.utils.RestClientUtils.getSearchException;

/**
 * REST client implementation of {@link org.craftercms.search.service.SearchService} for Search REST API v3
 * @author joseross
 */
public class RestClientSearchService extends SolrRestClientSearchService {

    private static final Logger logger = LoggerFactory.getLogger(RestClientSearchService.class);

    protected boolean parseContent = true;

    public void setParseContent(final boolean parseContent) {
        this.parseContent = parseContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final SearchRequest request) {
        String searchUrl = createBaseUrl(URL_SEARCH);
        try {
            return restTemplate.postForObject(searchUrl, request, SearchResponse.class);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(request.getIndexId(), "Search for request " + request + " failed: [" +
                    e.getStatusText() + "] " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(request.getIndexId(), "Search for request " + request + " failed: " +
                e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map nativeSearch(final String indexId, final Map params) {
        String searchUrl = createBaseUrl(URL_SEARCH_NATIVE);
        searchUrl = addParam(searchUrl, PARAM_INDEX_ID, indexId);
        try {
            return restTemplate.postForObject(searchUrl, params, Map.class);
        } catch (HttpStatusCodeException e) {
            throw getSearchException(indexId, "Search for query " + params + " failed: [" +
                e.getStatusText() + "] " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new SearchException(indexId, "Search for query " + params + " failed: " +
                e.getMessage(), e);
        }
    }

    @Override
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
        parts.set(PARAM_PARSE, Boolean.toString(parseContent));

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> search(final String indexId, final SolrQuery query) throws SearchException {
        throw new UnsupportedOperationException("Query objects are not supported for API 3");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createBaseUrl(String serviceUrl) {
        return UrlUtils.concat(serverUrl, URL_ROOT, serviceUrl);
    }

}
