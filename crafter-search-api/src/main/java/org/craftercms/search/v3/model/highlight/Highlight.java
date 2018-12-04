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

package org.craftercms.search.v3.model.highlight;

import java.util.List;

/**
 * Holds the highlighted fields for a document
 * @author joseross
 */
public class Highlight {

    /**
     * Id of the document
     */
    protected String id;

    /**
     * List of fields
     */
    protected List<HighlightField> fields;

    public Highlight() {
    }

    public Highlight(final String id, final List<HighlightField> fields) {
        this.id = id;
        this.fields = fields;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public List<HighlightField> getFields() {
        return fields;
    }

    public void setFields(final List<HighlightField> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "Highlight{" + "id='" + id + '\'' + ", fields=" + fields + '}';
    }

}
