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
 * Holds the highlighted fragments for a field
 * @author joseross
 */
public class HighlightField {

    /**
     * Name of the field
     */
    protected String name;

    /**
     * List of fragments
     */
    protected List<String> fragments;

    public HighlightField() {
    }

    public HighlightField(final String name, final List<String> fragments) {
        this.name = name;
        this.fragments = fragments;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getFragments() {
        return fragments;
    }

    public void setFragments(final List<String> fragments) {
        this.fragments = fragments;
    }

    @Override
    public String toString() {
        return "HighlightField{" + "name='" + name + '\'' + ", fragments=" + fragments + '}';
    }

}
