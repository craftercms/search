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

package org.craftercms.search.rest.v3.requests;

/**
 * Holds the data needed to include suggestions during a a search operation
 * @author joseross
 */
public class SuggestRequest {

    /**
     * Name of the suggester to use
     */
    protected String name;

    /**
     * Name of the field used to generate suggestions
     */
    protected String field;

    /**
     * Query used to generate suggestions
     */
    protected String query;

    /**
     * Maximum number of suggestions to generate
     */
    protected int max = 10;

    public String getName() {
        return name;
    }

    public SuggestRequest setName(final String name) {
        this.name = name;
        return this;
    }

    public String getField() {
        return field;
    }

    public SuggestRequest setField(final String field) {
        this.field = field;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public SuggestRequest setQuery(final String query) {
        this.query = query;
        return this;
    }

    public int getMax() {
        return max;
    }

    public SuggestRequest setMax(final int max) {
        this.max = max;
        return this;
    }

    @Override
    public String toString() {
        return "SuggestRequest{" + "name='" + name + '\'' + ", field='" + field + '\'' + ", query='" + query + '\'' +
            ", max=" + max + '}';
    }

}
