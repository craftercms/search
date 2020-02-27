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

package org.craftercms.search.rest.v2.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.craftercms.search.service.AdminService;

import static org.craftercms.search.rest.v2.AdminRestApiConstants.*;

/**
 * Created by alfonsovasquez on 2/6/17.
 */
public class DeleteIndexRequest {

    private AdminService.IndexDeleteMode deleteMode;

    @JsonProperty(PARAM_DELETE_MODE)
    public AdminService.IndexDeleteMode getDeleteMode() {
        return deleteMode;
    }

    @JsonProperty(PARAM_DELETE_MODE)
    public void setDeleteMode(AdminService.IndexDeleteMode deleteMode) {
        this.deleteMode = deleteMode;
    }

}
