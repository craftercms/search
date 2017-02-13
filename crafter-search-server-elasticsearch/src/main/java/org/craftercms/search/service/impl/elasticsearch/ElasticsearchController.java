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
package org.craftercms.search.service.impl.elasticsearch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.QueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.craftercms.search.rest.SearchRestApiConstants.*;

/**
 * Copied from {@link org.craftercms.search.rest.controller.SearchRestController} to be used in new Elasticsearch module.
 */
@RestController
@RequestMapping(URL_ROOT)
public class ElasticsearchController {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchController.class);

    private static final String[] NON_ADDITIONAL_FIELD_NAMES =
            {PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_DOCUMENT};

    private final SearchService searchService;

    @Autowired
    public ElasticsearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = URL_SEARCH, method = RequestMethod.GET)
    public Map<String, Object> search(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                                      HttpServletRequest request) throws SearchException {
        return searchService.search(indexId, new QueryParams(request.getParameterMap()));
    }

    @RequestMapping(value = URL_UPDATE, method = RequestMethod.POST)
    public String update(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(PARAM_ID) String id,
                         @RequestParam(PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        return searchService.update(indexId, site, id, xml, ignoreRootInFieldNames);
    }

    @RequestMapping(value = URL_DELETE, method = RequestMethod.POST)
    public String delete(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(value = PARAM_ID) String id) throws SearchException {
        return searchService.delete(indexId, site, id);
    }

    @RequestMapping(value = URL_COMMIT, method = RequestMethod.POST)
    public String commit(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId)
            throws SearchException {
        return searchService.commit(indexId);
    }

    @RequestMapping(value = {URL_UPDATE_DOCUMENT, URL_PARTIAL_DOCUMENT_UPDATE}, method = RequestMethod.POST)
    @Deprecated
    public String updateDocument(@RequestPart(PARAM_SITE) String site,
                                 @RequestPart(PARAM_ID) String id,
                                 @RequestPart(PARAM_DOCUMENT) MultipartFile document,
                                 HttpServletRequest request) throws SearchException {
        try {
            File tmpFile = File.createTempFile("crafter-" + document.getOriginalFilename(), "");
            Map<String, String> additionalFields = getAdditionalFields(request);

            document.transferTo(tmpFile);

            try {
                return searchService.updateDocument(site, id, tmpFile, additionalFields);
            } finally {
                FileUtils.forceDelete(tmpFile);
            }
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    @RequestMapping(value = URL_UPDATE_FILE, method = RequestMethod.POST)
    public String updateFile(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                             @RequestPart(PARAM_SITE) String site,
                             @RequestPart(PARAM_ID) String id,
                             @RequestPart(PARAM_FILE) MultipartFile file,
                             HttpServletRequest request) throws SearchException {

        try {
            File tmpFile = File.createTempFile("crafter-" + file.getOriginalFilename(), "");
            MultiValueMap<String, String> additionalFields = getAdditionalMultiValueFields(request);

            file.transferTo(tmpFile);

            try {
                return searchService.updateFile(indexId, site, id, tmpFile, additionalFields);
            } finally {
                FileUtils.forceDelete(tmpFile);
            }
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    protected Map<String, String> getAdditionalFields(HttpServletRequest request) {
        Map<String, String> additionalFields = new HashMap<>();

        for (Enumeration i = request.getParameterNames(); i.hasMoreElements();) {
            String paramName = (String) i.nextElement();
            if (!ArrayUtils.contains(NON_ADDITIONAL_FIELD_NAMES, paramName)) {
                additionalFields.put(paramName, request.getParameter(paramName));
            }
        }

        return additionalFields;
    }

    protected MultiValueMap<String, String> getAdditionalMultiValueFields(HttpServletRequest request) {
        MultiValueMap<String, String> additionalFields = new LinkedMultiValueMap<>();

        for (Enumeration i = request.getParameterNames(); i.hasMoreElements(); ) {
            String paramName = (String)i.nextElement();
            if (!ArrayUtils.contains(NON_ADDITIONAL_FIELD_NAMES, paramName)) {
                additionalFields.put(paramName, Arrays.asList(request.getParameterValues(paramName)));
            }
        }

        return additionalFields;
    }

}