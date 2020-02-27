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

import javax.validation.constraints.NotNull;

import static org.craftercms.search.rest.v2.AdminRestApiConstants.PATH_VAR_ID;

/**
 * Created by alfonsovasquez on 2/6/17.
 */
public class CreateIndexRequest {

    @NotNull
    private String id;

    public CreateIndexRequest() {
    }

    public CreateIndexRequest(String id) {
        this.id = id;
    }

    @JsonProperty(PATH_VAR_ID)
    public String getId() {
        return id;
    }

    @JsonProperty(PATH_VAR_ID)
    public void setId(String id) {
        this.id = id;
    }
    
}
