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
import java.io.Writer;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.QueryParams;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import static org.craftercms.search.service.SearchRestConstants.REQUEST_PARAM_DESCRIPTOR;
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
 * REST controller for the search service.
 *
 * @author Alfonso Vásquez
 */
@Controller
@RequestMapping(URL_ROOT)
public class SearchRestController {

    private static final Log logger = LogFactory.getLog(SearchRestController.class);

    private SearchService searchService;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = URL_SEARCH, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> search(HttpServletRequest request) throws MissingServletRequestParameterException,
        SearchException {
        return searchService.search(new QueryParams(request.getParameterMap()));
    }

    @RequestMapping(value = URL_UPDATE, method = RequestMethod.POST)
    @ResponseBody
    public String update(@RequestParam(REQUEST_PARAM_SITE) String site, @RequestParam(REQUEST_PARAM_ID) String id,
                         @RequestParam(REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        return searchService.update(site, id, xml, ignoreRootInFieldNames);
    }

    @RequestMapping(value = URL_DELETE, method = RequestMethod.POST)
    @ResponseBody
    public String delete(@RequestParam(REQUEST_PARAM_SITE) String site, @RequestParam(value = REQUEST_PARAM_ID)
    String id) throws SearchException {
        return searchService.delete(site, id);
    }

    @RequestMapping(value = URL_COMMIT, method = RequestMethod.POST)
    @ResponseBody
    public String commit() throws SearchException {
        return searchService.commit();
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response,
                                Writer responseWriter) throws IOException {
        logger.error("RESTful request " + request.getRequestURI() + " failed", e);

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage(e));
    }

    @RequestMapping(value = URL_UPDATE_DOCUMENT, method = RequestMethod.POST)
    @ResponseBody
    public String updateDocument(@RequestPart(REQUEST_PARAM_SITE) String site, @RequestPart(REQUEST_PARAM_ID) String
        id, @RequestPart(REQUEST_PARAM_DOCUMENT) MultipartFile document,
                                 @RequestPart(value = REQUEST_PARAM_DESCRIPTOR, required = false) MultipartFile
                                     descriptor) throws SearchException {

        try {
            String tempPath = System.getProperty("java.io.tmpdir");
            if (tempPath == null) {
                tempPath = "temp";
            }
            File documentFile = new File(tempPath + File.separator + "crafter" + document.getOriginalFilename());

            document.transferTo(documentFile);
            return searchService.updateDocument(site, id, documentFile);
        } catch (IOException e) {
            throw new SearchException(e);
        }
    }
}
