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
package org.craftercms.search.utils;

/**
 * Extension of Apache Common's {@link org.apache.commons.lang.BooleanUtils} thats adds some new methods.
 *
 * @author Alfonso Vásquez
 */
public class BooleanUtils extends org.apache.commons.lang.BooleanUtils {

    /**
     * Just as {@link #toBoolean(String)}, except that true will be returned if str = null when {@code trueIfNull} is true,
     * false otherwise.
     */
    public static final boolean toBoolean(String str, boolean trueIfNull) {
        if (str == null && trueIfNull) {
            return true;
        }

        return toBoolean(str);
    }

}
