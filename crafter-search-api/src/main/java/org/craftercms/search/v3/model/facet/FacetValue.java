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

package org.craftercms.search.v3.model.facet;

/**
 * Holds a value - count pair for a facet
 * @author joseross
 */
public class FacetValue {

    /**
     * Value for the facet
     */
    protected String value;

    /**
     * Count for the value
     */
    protected long count;

    public FacetValue() {
    }

    public FacetValue(final String value, final long count) {
        this.value = value;
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public long getCount() {
        return count;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "FacetValue{" + "value='" + value + '\'' + ", count=" + count + '}';
    }

}
