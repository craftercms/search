/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.metadata;

import java.util.Map;

import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;

/**
 * Performs any metadata processing needed for files in the site repository.
 *
 * @author joseross
 * @since 3.1.1
 */
public interface MetadataExtractor {

    /**
     * Performs the metadata extraction on the given file
     * @param path the path of the file to process
     * @param contentStoreService the content store service
     * @param context the current context
     * @return the extracted metadata
     */
    Map<String, String> extract(String path, ContentStoreService contentStoreService, Context context);

}
