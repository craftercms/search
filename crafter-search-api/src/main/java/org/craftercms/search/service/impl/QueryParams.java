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
package org.craftercms.search.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.search.service.Query;

/**
 * Simple {@link Query} represented as a map of query parameters.
 *
 * @author Alfonso Vásquez
 */
public class QueryParams implements Query {

    private Map<String, String[]> params;

    public QueryParams() {
        params = new LinkedHashMap<>();
    }

    public QueryParams(Map<String, String[]> params) {
        this.params = new LinkedHashMap<>(params);
    }

    public boolean hasParam(String name) {
        return params.containsKey(name);
    }

    public String[] getParam(String name) {
        return params.get(name);
    }

    public QueryParams addParam(String name, String value) {
        String[] oldValues = params.get(name);
        String[] values;

        if (ArrayUtils.isNotEmpty(oldValues)) {
            values = ArrayUtils.add(oldValues, value);
        } else {
            values = new String[1];
            values[0] = value;
        }

        params.put(name, values);

        return this;
    }

    public QueryParams addParam(String name, String... values) {
        String[] oldValues = params.get(name);
        if (ArrayUtils.isNotEmpty(oldValues)) {
            values = ArrayUtils.addAll(oldValues, values);
        }

        params.put(name, values);

        return this;
    }

    public Map<String, String[]> getParams() {
        return params;
    }


    public String toQueryString() {
        StringBuilder queryStr = new StringBuilder();

        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String name = param.getKey();

            for (String value : param.getValue()) {
                if (queryStr.length() > 0) {
                    queryStr.append('&');
                }

                try {
                    queryStr.append(name).append('=').append(URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Shouldn't happen, UTF-8 is a valid encoding
                    throw new RuntimeException();
                }
            }
        }

        return queryStr.toString();
    }

    @Override
    public String toString() {
        return toQueryString();
    }

}
