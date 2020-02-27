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
package org.craftercms.search.rest.v1;

/**
 * Search API REST constants used by both server and client (API v1).
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
public class SearchRestApiConstants {

    public static final String URL_ROOT = "/api/1/search";
    public static final String URL_SEARCH = "/search";
    public static final String URL_UPDATE = "/update";
    public static final String URL_UPDATE_DOCUMENT = "/update-document";
    public static final String URL_PARTIAL_DOCUMENT_UPDATE = "/partial-document-update";
    public static final String URL_UPDATE_FILE= "/update-file";
    public static final String URL_DELETE = "/delete";
    public static final String URL_COMMIT = "/commit";

    public static final String PARAM_INDEX_ID = "indexId";
    public static final String PARAM_SITE = "site";
    public static final String PARAM_ID = "id";
    public static final String PARAM_IGNORE_ROOT_IN_FIELD_NAMES = "stripRoot";
    public static final String PARAM_FILE = "document";

    private SearchRestApiConstants() {
    }

}
