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
package org.craftercms.search.service.impl;

import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.craftercms.search.service.Query;

/**
 * Search query for Solr.
 *
 * @author Alfonso Vásquez
 */
public class SolrQuery extends QueryParams {

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

    public SolrQuery(Map<String, String[]> params) {
        super(params);
    }

    @Override
    public Query setOffset(int offset) {
        return setStart(offset);
    }

    @Override
    public int getOffset() {
        return getStart();
    }

    @Override
    public Query setNumResults(int numResults) {
        return setRows(numResults);
    }

    @Override
    public int getNumResults() {
        return getRows();
    }

    @Override
    public SolrQuery setFieldsToReturn(String... fields) {
        addParam(FIELDS_TO_RETURN, fields);

        return this;
    }

    @Override
    public String[] getFieldsToReturn() {
        return getParam(FIELDS_TO_RETURN);
    }

    @Override
    public String getQuery() {
        return getSingleValue(QUERY_PARAM);
    }

    @Override
    public SolrQuery setQuery(String query) {
        setParam(QUERY_PARAM, query);

        return this;
    }

    public boolean isHighlight() {
        return BooleanUtils.toBoolean(getSingleValue(HIGHLIGHT_PARAM));
    }

    public SolrQuery setHighlight(boolean highlight) {
        setParam(HIGHLIGHT_PARAM, Boolean.toString(highlight));

        return this;
    }

    public String[] getHighlightFields() {
        return getParam(HIGHLIGHT_FIELDS_PARAM);
    }

    public SolrQuery setHighlightFields(String... fields) {
        setHighlight(true).addParam(HIGHLIGHT_FIELDS_PARAM, fields);

        return this;
    }

    public int getHighlightSnippets() {
        return NumberUtils.toInt(getSingleValue(HIGHLIGHT_SNIPPETS_PARAM));
    }

    public SolrQuery setHighlightSnippets(int snippets) {
        setHighlight(true).setParam(HIGHLIGHT_SNIPPETS_PARAM, Integer.toString(snippets));

        return this;
    }

    public int getHighlightSnippetSize() {
        return NumberUtils.toInt(getSingleValue(HIGHLIGHT_SNIPPET_SIZE_PARAM));
    }

    public SolrQuery setHighlightSnippetSize(int size) {
        setHighlight(true).setParam(HIGHLIGHT_SNIPPET_SIZE_PARAM, Integer.toString(size));

        return this;
    }

    public String[] getFilterQueries() {
        return getParam(FILTER_QUERY_PARAM);
    }

    public SolrQuery setFilterQueries(String... queries) {
        addParam(FILTER_QUERY_PARAM, queries);

        return this;
    }

    public SolrQuery addFilterQuery(String query) {
        addParam(FILTER_QUERY_PARAM, query);

        return this;
    }

    public int getStart() {
        return NumberUtils.toInt(getSingleValue(START_PARAM));
    }

    public SolrQuery setStart(int start) {
        setParam(START_PARAM, Integer.toString(start));

        return this;
    }

    public int getRows() {
        return NumberUtils.toInt(getSingleValue(ROWS_PARAM));
    }

    public SolrQuery setRows(int rows) {
        setParam(ROWS_PARAM, Integer.toString(rows));

        return this;
    }

}
