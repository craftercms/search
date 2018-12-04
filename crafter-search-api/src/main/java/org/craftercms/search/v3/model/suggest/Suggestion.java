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

package org.craftercms.search.v3.model.suggest;

import java.util.List;

/**
 * Holds the suggestions generated for a search
 * @author joseross
 */
public class Suggestion {

    /**
     * Name of the suggester
     */
    protected String name;

    /**
     * List of suggested values
     */
    protected List<String> suggestions;

    public Suggestion() {
    }

    public Suggestion(final String name, final List<String> suggestions) {
        this.name = name;
        this.suggestions = suggestions;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(final List<String> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "Suggestion{" + "name='" + name + '\'' + ", suggestions=" + suggestions + '}';
    }

}
