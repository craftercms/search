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

import java.util.LinkedList;
import java.util.List;

import org.craftercms.search.v3.model.sort.FieldSort;
import org.craftercms.search.v3.model.sort.Order;

/**
 * Holds the data needed to sort during a search operation
 * @author joseross
 */
public class SortRequest {

    /**
     * List of fields to sort during the search
     */
    protected List<FieldSort> fields = new LinkedList<>();

    public List<FieldSort> getFields() {
        return fields;
    }

    public SortRequest setFields(final List<FieldSort> fields) {
        this.fields = fields;
        return this;
    }

    public SortRequest addField(String name, String order) {
        this.fields.add(new FieldSort(name, Order.valueOf(order)));
        return this;
    }

    @Override
    public String toString() {
        return "SortRequest{" + "fields=" + fields + '}';
    }

}
