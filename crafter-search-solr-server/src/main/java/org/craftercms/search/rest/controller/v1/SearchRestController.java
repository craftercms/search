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
package org.craftercms.search.rest.controller.v1;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import static org.craftercms.search.rest.v1.SearchRestApiConstants.*;

/**
 * REST controller for the search service.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
@Controller
@RequestMapping(URL_ROOT)
public class SearchRestController {

    private static final String[] NON_ADDITIONAL_FIELD_NAMES =
        {PARAM_INDEX_ID, PARAM_SITE, PARAM_ID, PARAM_FILE};

    protected SearchService searchService;
    protected String multiValueSeparator;
    protected String multiValueIgnorePattern;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setMultiValueSeparator(String multiValueSeparator) {
        this.multiValueSeparator = multiValueSeparator;
    }

    @Required
    public void setMultiValueIgnorePattern(String multiValueIgnorePattern) {
        this.multiValueIgnorePattern = multiValueIgnorePattern;
    }

    @RequestMapping(value = URL_SEARCH, method = RequestMethod.GET)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public Map<String, Object> search(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                                      HttpServletRequest request) throws SearchException {
        Query query = searchService.createQuery(request.getParameterMap());

        return searchService.search(indexId, query);
    }

    @RequestMapping(value = URL_UPDATE, method = RequestMethod.POST)
    @ResponseBody
    public String update(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(PARAM_ID) String id,
                         @RequestParam(PARAM_IGNORE_ROOT_IN_FIELD_NAMES) boolean ignoreRootInFieldNames,
                         @RequestBody String xml) throws SearchException {
        searchService.update(indexId, site, id, xml, ignoreRootInFieldNames);

        return getSuccessMessage("Update of %s:%s for index %s successful", indexId, site, id);
    }

    @RequestMapping(value = URL_DELETE, method = RequestMethod.POST)
    @ResponseBody
    public String delete(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                         @RequestParam(PARAM_SITE) String site,
                         @RequestParam(value = PARAM_ID) String id) throws SearchException {
        searchService.delete(indexId, site, id);

        return getSuccessMessage("Delete of %s:%s for index %s successful", indexId, site, id);
    }

    @RequestMapping(value = URL_COMMIT, method = RequestMethod.POST)
    @ResponseBody
    public String commit(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId) throws SearchException {
        searchService.commit(indexId);

        return getSuccessMessage("Commit for index %s successful", indexId, null, null);
    }

    @RequestMapping(value = {URL_UPDATE_DOCUMENT, URL_PARTIAL_DOCUMENT_UPDATE}, method = RequestMethod.POST)
    @ResponseBody
    @Deprecated
    public String updateDocument(@RequestPart(PARAM_SITE) String site,
                                 @RequestPart(PARAM_ID) String id,
                                 @RequestPart(PARAM_FILE) MultipartFile document,
                                 HttpServletRequest request) throws SearchException {
         return updateContent(null, site, id, document, request, true);
    }

    @RequestMapping(value = URL_UPDATE_FILE, method = RequestMethod.POST)
    @ResponseBody
    public String updateFile(@RequestParam(value = PARAM_INDEX_ID, required = false) String indexId,
                             @RequestPart(PARAM_SITE) String site,
                             @RequestPart(PARAM_ID) String id,
                             @RequestPart(PARAM_FILE) MultipartFile file,
                             HttpServletRequest request) throws SearchException {
        return updateContent(indexId, site, id, file, request, false);
    }

    @SuppressWarnings("unchecked")
    protected String updateContent(String indexId, String site, String id, MultipartFile file, HttpServletRequest request,
                                   boolean splitParams) {
        try {
            File tmpFile = File.createTempFile("crafter-" + file.getOriginalFilename(), "");
            MultiValueMap<String, String> additionalFields = getAdditionalFields(request, splitParams);

            file.transferTo(tmpFile);

            try {
                searchService.updateContent(indexId, site, id, tmpFile, additionalFields);

                return getSuccessMessage("Update of %s:%s for index %s successful", indexId, site, id);
            } finally {
                FileUtils.forceDelete(tmpFile);
            }
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    protected MultiValueMap<String, String> getAdditionalFields(HttpServletRequest request, boolean splitParams) {
        MultiValueMap<String, String> additionalFields = new LinkedMultiValueMap<>();

        for (Enumeration i = request.getParameterNames(); i.hasMoreElements(); ) {
            String paramName = (String)i.nextElement();
            if (!ArrayUtils.contains(NON_ADDITIONAL_FIELD_NAMES, paramName)) {
                if (splitParams) {
                    String paramValue = request.getParameter(paramName);

                    if (!paramName.matches(multiValueIgnorePattern)) {
                        additionalFields.put(paramName, Arrays.asList(paramValue.split(multiValueSeparator)));
                    } else {
                        additionalFields.add(paramName, paramValue);
                    }
                } else {
                    additionalFields.put(paramName, Arrays.asList(request.getParameterValues(paramName)));
                }
            }
        }

        return additionalFields;
    }

    protected String getSuccessMessage(String message, String indexId, String site, String path) {
        String indexStr;

        if (StringUtils.isNotEmpty(indexId)) {
            indexStr = "'" + indexId + "'";
        } else {
            indexStr = "default";
        }

        if (StringUtils.isNotEmpty(site) && StringUtils.isNotEmpty(path)) {
            return String.format(message, site, path, indexStr);
        } else {
            return String.format(message, indexStr);
        }
    }

}
