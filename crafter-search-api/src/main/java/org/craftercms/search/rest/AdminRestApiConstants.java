/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
package org.craftercms.search.rest;

/**
 * Created by alfonsovasquez on 2/6/17.
 */
public class AdminRestApiConstants {

    private AdminRestApiConstants() {
    }

    public static final String PATH_VAR_ID = "id";
    public static final String PARAM_DELETE_MODE = "delete_mode";

    public static final String URL_ROOT = "/api/2/admin";
    public static final String URL_CREATE_INDEX = "/index/create";
    public static final String URL_GET_INDEX_INFO = "/index/info/{" + PATH_VAR_ID + "}";
    public static final String URL_DELETE_INDEX = "/index/delete/{" + PATH_VAR_ID + "}";

}
