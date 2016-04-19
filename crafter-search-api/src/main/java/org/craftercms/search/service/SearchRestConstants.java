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
package org.craftercms.search.service;

/**
 * Common REST constants used by both server and client.
 *
 * @author Alfonso Vásquez
 * @author Dejan Brkic
 */
public class SearchRestConstants {

    public static final String URL_ROOT = "/api/1/search";
    public static final String URL_SEARCH = "/search";
    public static final String URL_UPDATE = "/update";
    public static final String URL_UPDATE_FILE= "/update-file";
    @Deprecated public static final String URL_UPDATE_DOCUMENT = "/update-document";
    @Deprecated public static final String URL_PARTIAL_DOCUMENT_UPDATE = "/partial-document-update";
    public static final String URL_DELETE = "/delete";
    public static final String URL_COMMIT = "/commit";

    public static final String REQUEST_PARAM_INDEX_ID = "indexId";
    public static final String REQUEST_PARAM_SITE = "site";
    public static final String REQUEST_PARAM_ID = "id";
    public static final String REQUEST_PARAM_IGNORE_ROOT_IN_FIELD_NAMES = "stripRoot";
    public static final String REQUEST_PARAM_DOCUMENT = "document";
    public static final String REQUEST_PARAM_FILE = "document";

    public static final String SOLR_CONTENT_STREAM_UPDATE_URL = "/update/extract";

}
