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
package org.craftercms.search.service;

/**
 * Base query that is passed to {@link SearchService#search(Query)}.
 *
 * @author Alfonso Vásquez
 */
public interface Query {

    /**
     * Sets the offset of the results.
     */
    Query setOffset(int offset);

    /**
     * Returns the offset of the results.
     */
    int getOffset();

    /**
     * Sets the number of results to return.
     */
    Query setNumResults(int numResults);

    /**
     * Returns the number of results to return.
     */
    int getNumResults();

    /**
     * Sets the fields that should be returned.
     */
    Query setFieldsToReturn(String... fieldsToReturn);

    /**
     * Returns the fields that should be returned.
     */
    String[] getFieldsToReturn();

    /**
     * Sets the actual query.
     */
    Query setQuery(String query);

    /**
     * Returns the actual query.
     */
    String getQuery();

    /**
     * Sets if the additional Crafter Search filters should be disabled on query execution.
     */
    Query setDisableAdditionalFilters(boolean disableAdditionalFilters);

    /**
     * Returns true if the additional Crafter Search filters should be disabled on query execution.
     */
    boolean isDisableAdditionalFilters();

    /**
     * Converts this query to an URL query string.
     */
    String toUrlQueryString();

}