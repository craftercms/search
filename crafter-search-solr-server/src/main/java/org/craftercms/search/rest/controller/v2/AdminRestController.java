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

import java.util.Map;
import javax.validation.Valid;

import org.craftercms.commons.rest.Result;
import org.craftercms.search.rest.v2.requests.CreateIndexRequest;
import org.craftercms.search.rest.v2.requests.DeleteIndexRequest;
import org.craftercms.search.service.AdminService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.craftercms.search.rest.v2.AdminRestApiConstants.PATH_VAR_ID;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_CREATE_INDEX;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_DELETE_INDEX;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_GET_INDEX_INFO;
import static org.craftercms.search.rest.v2.AdminRestApiConstants.URL_ROOT;

/**
 * REST controller for the admin service.
 *
 * @author Alfonso Vásquez
 */
@RestController
@RequestMapping(URL_ROOT)
public class AdminRestController {

    protected AdminService adminService;

    @Required
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @RequestMapping(value = URL_CREATE_INDEX, method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Result createIndex(@Valid @RequestBody CreateIndexRequest request) {
        adminService.createIndex(request.getId());

        return Result.OK;
    }

    @RequestMapping(value = URL_GET_INDEX_INFO, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getIndexInfo(@PathVariable(PATH_VAR_ID) String id) {
        return adminService.getIndexInfo(id);
    }

    @RequestMapping(value = URL_DELETE_INDEX, method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIndex(@PathVariable(PATH_VAR_ID) String id,
                            @Valid @RequestBody(required = false) DeleteIndexRequest request) {
        adminService.deleteIndex(id, request != null? request.getDeleteMode() : null);
    }

}
