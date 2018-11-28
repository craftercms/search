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

import java.util.Map;

import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.service.impl.SolrQuery;
import org.craftercms.search.service.impl.v2.SolrRestClientSearchService;
import org.springframework.web.client.HttpStatusCodeException;

import static org.craftercms.search.rest.v3.SearchRestApiConstants.*;
import static org.craftercms.search.service.utils.RestClientUtils.addParam;
import static org.craftercms.search.service.utils.RestClientUtils.getSearchException;

/**
 * REST client implementation of {@link org.craftercms.search.service.SearchService} for Search REST API v3
 * @author joseross
 */
public class RestClientSearchService extends SolrRestClientSearchService {

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
