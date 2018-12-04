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

package org.craftercms.search.rest.controller.v3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.v3.service.DocumentParser;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.craftercms.search.rest.v3.SearchRestApiConstants.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Rest controller for Search API v3
 * @author joseross
 */
@RestController
@RequestMapping(URL_ROOT)
public class SearchRestController {

    private static final String[] NON_ADDITIONAL_FIELD_NAMES = {PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_CONTENT,
        PARAM_PARSE};

    protected SearchService searchService;

    protected DocumentParser documentParser;

    @Required
    public void setSearchService(final SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setDocumentParser(final DocumentParser documentParser) {
        this.documentParser = documentParser;
    }

    @RequestMapping(value = URL_SEARCH, method = { GET, POST })
    public SearchResponse search(@RequestBody SearchRequest request) {
        return searchService.search(request);
    }

    @RequestMapping(value = URL_SEARCH_NATIVE, method = { GET, POST })
    public Object search(@RequestParam String indexId, @RequestBody Map<String,Object> params) {
        return searchService.nativeSearch(indexId, params);
    }

    @RequestMapping(value = URL_DELETE, method = POST)
    public Result delete(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(value = PARAM_ID) String id) throws SearchException {
        searchService.delete(indexId, site, id);

        return Result.OK;
    }

    @RequestMapping(value = URL_COMMIT, method = POST)
    public Result commit(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId) throws SearchException {
        searchService.commit(indexId);

        return Result.OK;
    }

    @RequestMapping(value = URL_UPDATE, method = POST)
    public Result update(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(PARAM_ID) String id,
                         @RequestParam(PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        searchService.update(indexId, site, id, xml, ignoreRootInFieldNames);

        return Result.OK;
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = URL_UPDATE_CONTENT, method = POST)
    public Result updateContent(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                                @RequestParam(PARAM_SITE) String site,
                                @RequestParam(PARAM_ID) String id,
                                @RequestParam(value = PARAM_PARSE, defaultValue = "true") boolean parseContent,
                                @RequestPart(PARAM_CONTENT) MultipartFile file,
                                HttpServletRequest request) throws SearchException {

        try {
            File tmpFile = File.createTempFile("crafter-" + file.getOriginalFilename(), "");
            MultiValueMap<String, String> additionalFields = getAdditionalMultiValueFields(request);

            file.transferTo(tmpFile);

            try {
                if(parseContent) {
                    try(InputStream is = new FileInputStream(tmpFile)) {
                        String xml = documentParser.parseToXml(is, additionalFields);
                        searchService.update(indexId, site, id, xml, true);
                    }
                } else {
                    searchService.updateContent(indexId, site, id, tmpFile, additionalFields);
                }

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
