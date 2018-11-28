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

import java.util.List;

/**
 * Holds the values for a given facet
 * @author joseross
 */
public class Facet {

    /**
     * Id of the facet
     */
    protected String id;

    /**
     * Values for the facet
     */
    protected List<FacetValue> values;

    public Facet() {
    }

    public Facet(final String id, final List<FacetValue> values) {
        this.id = id;
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public List<FacetValue> getValues() {
        return values;
    }

    public void setValues(final List<FacetValue> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Facet{" + "id='" + id + '\'' + ", values=" + values + '}';
    }

}
