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
package org.craftercms.search.rest.controller.v2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.craftercms.search.rest.v2.SearchRestApiConstants.PARAM_CONTENT;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.PARAM_ID;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.PARAM_IGNORE_ROOT_IN_FIELD_NAMES;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.PARAM_INDEX_ID;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.PARAM_SITE;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_COMMIT;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_DELETE;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_ROOT;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_SEARCH;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_UPDATE;
import static org.craftercms.search.rest.v2.SearchRestApiConstants.URL_UPDATE_CONTENT;

/**
 * REST controller for the search service.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
@RestController
@RequestMapping(URL_ROOT)
@SuppressWarnings("unchecked")
public class SearchRestController {

    private static final String[] NON_ADDITIONAL_FIELD_NAMES = {PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_CONTENT};

    private SearchService searchService;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = URL_SEARCH, method = RequestMethod.GET)
    public Map<String, Object> search(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                                      HttpServletRequest request) throws SearchException {
        Query query = searchService.createQuery(request.getParameterMap());

        return searchService.search(indexId, query);
    }

    @RequestMapping(value = URL_UPDATE, method = RequestMethod.POST)
    public Result update(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(PARAM_ID) String id,
                         @RequestParam(PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        searchService.update(indexId, site, id, xml, ignoreRootInFieldNames);

        return Result.OK;
    }

    @RequestMapping(value = URL_DELETE, method = RequestMethod.POST)
    public Result delete(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(value = PARAM_ID) String id) throws SearchException {
        searchService.delete(indexId, site, id);

        return Result.OK;
    }

    @RequestMapping(value = URL_COMMIT, method = RequestMethod.POST)
    public Result commit(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId) throws SearchException {
        searchService.commit(indexId);

        return Result.OK;
    }

    @RequestMapping(value = URL_UPDATE_CONTENT, method = RequestMethod.POST)
    public Result updateContent(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                                @RequestParam(PARAM_SITE) String site,
                                @RequestParam(PARAM_ID) String id,
                                @RequestPart(PARAM_CONTENT) MultipartFile file,
                                HttpServletRequest request) throws SearchException {

        try {
            File tmpFile = File.createTempFile("crafter-" + file.getOriginalFilename(), "");
            MultiValueMap<String, String> additionalFields = getAdditionalMultiValueFields(request);

            file.transferTo(tmpFile);

            try {
                searchService.updateContent(indexId, site, id, tmpFile, additionalFields);

                return Result.OK;
            } finally {
                FileUtils.forceDelete(tmpFile);
            }
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    protected MultiValueMap<String, String> getAdditionalMultiValueFields(HttpServletRequest request) {
        MultiValueMap<String, String> additionalFields = new LinkedMultiValueMap<>();

        for (Enumeration<String> i = request.getParameterNames(); i.hasMoreElements(); ) {
            String paramName = i.nextElement();
            if (!ArrayUtils.contains(NON_ADDITIONAL_FIELD_NAMES, paramName)) {
                additionalFields.put(paramName, Arrays.asList(request.getParameterValues(paramName)));
            }
        }

        return additionalFields;
    }

}
