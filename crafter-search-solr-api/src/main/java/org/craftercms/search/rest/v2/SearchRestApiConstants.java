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
package org.craftercms.search.rest.v2;

/**
 * Search API REST constants used by both server and client.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
public class SearchRestApiConstants {

    public static final String PARAM_INDEX_ID = "index_id";

    public static final String PARAM_SITE = "site";
    public static final String PARAM_ID = "id";
    public static final String PARAM_IGNORE_ROOT_IN_FIELD_NAMES = "strip_root";
    public static final String PARAM_CONTENT = "content";

    public static final String URL_ROOT = "/api/2/search";
    public static final String URL_SEARCH = "/search";
    public static final String URL_UPDATE = "/update";
    public static final String URL_UPDATE_CONTENT = "/update-content";
    public static final String URL_DELETE = "/delete";
    public static final String URL_COMMIT = "/commit";

    private SearchRestApiConstants() {
    }

}
