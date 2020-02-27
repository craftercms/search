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
package org.craftercms.search.utils;

/**
 * Utility methods for Solr queries.
 *
 * @author Alfonso Vásquez
 */
public class SolrQueryUtils {

    public static final String USER_SEARCH_QUERY_ILLEGAL_CHARS = "():[]{}\\";

    private SolrQueryUtils() {
    }

    /**
     * Escapes characters in a user search query (the search terms the user entered). User search queries shouldn't
     * contain the following characters, which can break the search, without escaping: ( ) : [ ] { } \
     *
     * @param q
     *          the query to escape
     * @return the escaped query
     */
    public static String escapeUserSearchQuery(String q) {
        return escapeChars(q, USER_SEARCH_QUERY_ILLEGAL_CHARS);
    }

    /**
     * Escapes the characters from {@code chars} in the {@code str} parameter.
     *
     * @param str
     *          the string to escape
     * @param chars
     *          the characters to escape
     * @return the escaped string
     */
    public static String escapeChars(String str, String chars) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (chars.indexOf(c) >= 0) {
                sb.append('\\');
            }

            sb.append(c);
        }

        return sb.toString();
    }

}
