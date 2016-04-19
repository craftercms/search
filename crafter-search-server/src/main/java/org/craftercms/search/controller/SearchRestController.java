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
package org.craftercms.search.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.QueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_DOCUMENT;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_FILE;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_ID;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_INDEX_ID;
import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_SITE;
import static org.craftercms.search.service.SearchRestConstants.URL_COMMIT;
import static org.craftercms.search.service.SearchRestConstants.URL_DELETE;
import static org.craftercms.search.service.SearchRestConstants.URL_PARTIAL_DOCUMENT_UPDATE;
import static org.craftercms.search.service.SearchRestConstants.URL_ROOT;
import static org.craftercms.search.service.SearchRestConstants.URL_SEARCH;
import static org.craftercms.search.service.SearchRestConstants.URL_UPDATE;
import static org.craftercms.search.service.SearchRestConstants.URL_UPDATE_DOCUMENT;
import static org.craftercms.search.service.SearchRestConstants.URL_UPDATE_FILE;

/**
 * REST controller for the search service.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
@Controller
@RequestMapping(URL_ROOT)
public class SearchRestController {

    private static final Logger logger = LoggerFactory.getLogger(SearchRestController.class);

    private static final String[] NON_ADDITIONAL_FIELD_NAMES =
        {REQUEST_PARAM_INDEX_ID, REQUEST_PARAM_SITE, REQUEST_PARAM_ID, REQUEST_PARAM_DOCUMENT};

    private SearchService searchService;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = URL_SEARCH, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> search(@RequestParam(value = REQUEST_PARAM_INDEX_ID, required = false) String indexId,
                                      HttpServletRequest request) throws SearchException {
        return searchService.search(indexId, new QueryParams(request.getParameterMap()));
    }

    @RequestMapping(value = URL_UPDATE, method = RequestMethod.POST)
    @ResponseBody
    public String update(@RequestParam(value = REQUEST_PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(REQUEST_PARAM_SITE) String site,
                         @RequestParam(REQUEST_PARAM_ID) String id,
                         @RequestParam(REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        return searchService.update(indexId, site, id, xml, ignoreRootInFieldNames);
    }

    @RequestMapping(value = URL_DELETE, method = RequestMethod.POST)
    @ResponseBody
    public String delete(@RequestParam(value = REQUEST_PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(REQUEST_PARAM_SITE) String site,
                         @RequestParam(value = REQUEST_PARAM_ID) String id) throws SearchException {
        return searchService.delete(indexId, site, id);
    }

    @RequestMapping(value = URL_COMMIT, method = RequestMethod.POST)
    @ResponseBody
    public String commit(@RequestParam(value = REQUEST_PARAM_INDEX_ID, required = false) String indexId)
        throws SearchException {
        return searchService.commit(indexId);
    }

    @RequestMapping(value = {URL_UPDATE_DOCUMENT, URL_PARTIAL_DOCUMENT_UPDATE}, method = RequestMethod.POST)
    @ResponseBody
    @Deprecated
    public String updateDocument(@RequestPart(REQUEST_PARAM_SITE) String site,
                                 @RequestPart(REQUEST_PARAM_ID) String id,
                                 @RequestPart(REQUEST_PARAM_DOCUMENT) MultipartFile document,
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
    @ResponseBody
    public String updateFile(@RequestParam(value = REQUEST_PARAM_INDEX_ID, required = false) String indexId,
                             @RequestPart(REQUEST_PARAM_SITE) String site,
                             @RequestPart(REQUEST_PARAM_ID) String id,
                             @RequestPart(REQUEST_PARAM_FILE) MultipartFile file,
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

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        logger.error("RESTful request " + request.getRequestURI() + " failed", e);

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage(e));
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
