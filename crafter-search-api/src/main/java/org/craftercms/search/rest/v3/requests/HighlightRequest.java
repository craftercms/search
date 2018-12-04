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

import java.util.Arrays;

/**
 * Holds the data needed to perform highlighting during a search operation
 * @author joseross
 */
public class HighlightRequest {

    /**
     * Query to use for highlighting
     */
    protected String query;

    /**
     * Maximum number of fragment to generate for each field
     */
    protected int maxFragments = 1;

    /**
     * Maximum size for each fragment
     */
    protected int fragmentSize = 100;

    /**
     * Opening tag used for the matched text
     */
    protected String prefix = "<em>";

    /**
     * Closing tag used for the matched text
     */
    protected String postfix = "</em>";

    /**
     * Fields to include during highlighting
     */
    protected String[] fields;

    public String getQuery() {
        return query;
    }

    public HighlightRequest setQuery(final String query) {
        this.query = query;
        return this;
    }

    public int getMaxFragments() {
        return maxFragments;
    }

    public HighlightRequest setMaxFragments(final int maxFragments) {
        this.maxFragments = maxFragments;
        return this;
    }

    public int getFragmentSize() {
        return fragmentSize;
    }

    public HighlightRequest setFragmentSize(final int fragmentSize) {
        this.fragmentSize = fragmentSize;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public HighlightRequest setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getPostfix() {
        return postfix;
    }

    public HighlightRequest setPostfix(final String postfix) {
        this.postfix = postfix;
        return this;
    }

    public String[] getFields() {
        return fields;
    }

    public HighlightRequest setFields(final String... fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        return "HighlightRequest{" + "query='" + query + '\'' + ", maxFragments=" + maxFragments + ", fragmentSize="
            + fragmentSize + ", prefix='" + prefix + '\'' + ", postfix='" + postfix + '\'' + ", fields="
            + Arrays.toString(fields) + '}';
    }

}
