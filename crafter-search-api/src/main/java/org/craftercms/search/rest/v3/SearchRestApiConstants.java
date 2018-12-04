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

package org.craftercms.search.rest.v3;

/**
 * Search API REST constants used by both server and client.
 *
 * @author joseross
 */
public interface SearchRestApiConstants {

    String PARAM_INDEX_ID = "index_id";

    String PARAM_SITE = "site";
    String PARAM_ID = "id";
    String PARAM_IGNORE_ROOT_IN_FIELD_NAMES = "strip_root";
    String PARAM_CONTENT = "content";
    String PARAM_PARSE = "parse_content";

    String URL_ROOT = "/api/3/search";
    String URL_SEARCH = "/search";
    String URL_SEARCH_NATIVE = "/search/native";
    String URL_UPDATE = "/update";
    String URL_UPDATE_CONTENT = "/update-content";
    String URL_DELETE = "/delete";
    String URL_COMMIT = "/commit";

}
