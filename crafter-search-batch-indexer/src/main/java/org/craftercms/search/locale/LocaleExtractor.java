/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.locale;

import org.craftercms.core.service.Context;

import java.util.Locale;

/**
 * Obtains the locale from a given file
 *
 * @author joseross
 * @since 4.0.0
 */
public interface LocaleExtractor {

    /**
     * Extract the locale for the given file
     *
     * @param context the current context
     * @param path the path of the file
     * @return the locale
     */
    Locale extract(Context context, String path);

}
