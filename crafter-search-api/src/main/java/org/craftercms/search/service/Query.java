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
     * Sets the number of results to return.
     */
    Query setNumResults(int numResults);

    /**
     * Sets the fields that should be returned.
     */
    Query setFieldsToReturn(String... fieldsToReturn);

    /**
     * Sets the actual query.
     */
    Query setQuery(String query);

    /**
     * Indicates if additional filters should be added.
     */
    void setUseAddtionalFilters(boolean useAddtionalFilters);

    boolean getUseAdditionalFilters();

    /**
     * Converts this query to an URL query string.
     */
    String toUrlQueryString();

}