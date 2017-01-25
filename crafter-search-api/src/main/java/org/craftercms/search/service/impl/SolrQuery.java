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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Search query for Solr.
 *
 * @author Alfonso Vásquez
 */
public class SolrQuery extends QueryParams {

    public static final String MULTIVALUE_SEPARATOR = ",";
    public static final String FIELDS_TO_RETURN = "fl";
    public static final String HIGHLIGHT_PARAM = "hl";
    public static final String HIGHLIGHT_FIELDS_PARAM = "hl.fl";
    public static final String HIGHLIGHT_SNIPPETS_PARAM = "hl.snippets";
    public static final String HIGHLIGHT_SNIPPET_SIZE_PARAM = "hl.fragsize";
    public static final String QUERY_PARAM = "q";
    public static final String FILTER_QUERY_PARAM = "fq";
    public static final String START_PARAM = "start";
    public static final String ROWS_PARAM = "rows";

    public SolrQuery() {
    }

    public SolrQuery(QueryParams queryParams) {
        super(queryParams.getParams());
    }

    public String[] getFieldsToReturn() {
        return getParam(FIELDS_TO_RETURN);
    }

    public SolrQuery setFieldsToReturn(String... fields) {
        addParam(FIELDS_TO_RETURN, StringUtils.join(fields, MULTIVALUE_SEPARATOR));

        return this;
    }

    public boolean isHighlight() {
        return BooleanUtils.toBoolean(getSingleValue(HIGHLIGHT_PARAM));
    }

    public SolrQuery setHighlight(boolean highlight) {
        addParam(HIGHLIGHT_PARAM, Boolean.toString(highlight));

        return this;
    }

    public String[] getHighlightFields() {
        return StringUtils.split(getSingleValue(HIGHLIGHT_FIELDS_PARAM), MULTIVALUE_SEPARATOR);
    }

    public SolrQuery setHighlightFields(String... fields) {
        if (!hasParam(HIGHLIGHT_PARAM)) {
            addParam(HIGHLIGHT_PARAM, "true");
        }

        addParam(HIGHLIGHT_FIELDS_PARAM, StringUtils.join(fields, MULTIVALUE_SEPARATOR));

        return this;
    }

    public String[] getHighlightSnippets() {
        return StringUtils.split(getSingleValue(HIGHLIGHT_SNIPPETS_PARAM), MULTIVALUE_SEPARATOR);
    }

    public SolrQuery setHighlightSnippets(int snippets) {
        if (!hasParam(HIGHLIGHT_PARAM)) {
            addParam(HIGHLIGHT_PARAM, "true");
        }

        addParam(HIGHLIGHT_SNIPPETS_PARAM, Integer.toString(snippets));

        return this;
    }

    public int getHighlightSnippetSize() {
        return NumberUtils.toInt(getSingleValue(HIGHLIGHT_SNIPPET_SIZE_PARAM));
    }

    public SolrQuery setHighlightSnippetSize(int size) {
        if (!hasParam(HIGHLIGHT_PARAM)) {
            addParam(HIGHLIGHT_PARAM, "true");
        }

        addParam(HIGHLIGHT_SNIPPET_SIZE_PARAM, Integer.toString(size));

        return this;
    }

    public String getQuery() {
        return getSingleValue(QUERY_PARAM);
    }

    public SolrQuery setQuery(String query) {
        addParam(QUERY_PARAM, query);

        return this;
    }

    public String[] getFilterQueries() {
        return getParam(FILTER_QUERY_PARAM);
    }

    public SolrQuery addFilterQuery(String query) {
        addParam(FILTER_QUERY_PARAM, query);

        return this;
    }

    public int getStart() {
        return NumberUtils.toInt(getSingleValue(START_PARAM));
    }

    public SolrQuery setStart(int start) {
        addParam(START_PARAM, Integer.toString(start));

        return this;
    }

    public int getRows() {
        return NumberUtils.toInt(getSingleValue(ROWS_PARAM));
    }

    public SolrQuery setRows(int rows) {
        addParam(ROWS_PARAM, Integer.toString(rows));

        return this;
    }

    protected String getSingleValue(String param) {
        if (hasParam(param)) {
            String[] values = getParam(param);
            if (ArrayUtils.isNotEmpty(values)) {
                return values[0];
            }
        }

        return null;
    }

}
