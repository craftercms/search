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

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

/**
 * Utility methods for handling search results
 */
public class SearchResultUtils {

    public static final String RESPONSE_KEY = "response";
    public static final String DOCUMENTS_KEY = "documents";

    /**
     * Extract the documents from the specified result.
     *
     * @param result the result containing the documents
     *
     * @return the documents
     */
    @SuppressWarnings("unchecked")
    public static final List<Map<String, Object>> getDocuments(Map<String, Object> result) {
        if (MapUtils.isNotEmpty(result) && result.containsKey(RESPONSE_KEY)) {
            Map<String, Object> response = (Map<String, Object>)result.get(RESPONSE_KEY);
            if (MapUtils.isNotEmpty(response) && response.containsKey(DOCUMENTS_KEY)) {
                return  (List<Map<String, Object>>)response.get(DOCUMENTS_KEY);
            }
        }

        return null;
    }

}
