/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.metadata.impl;

import java.util.Collections;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;

/**
 * Implementation of {@link org.craftercms.search.metadata.MetadataExtractor} for the file size field
 *
 * @author joseross
 * @since 3.1.1
 */
public class FileSizeMetadataExtractor extends AbstractMetadataExtractor {

    public static final String DEFAULT_PROPERTY_NAME = "contentLength";

    /**
     * The name of the metadata property to return
     */
    protected String propertyName = DEFAULT_PROPERTY_NAME;

    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCompatible(final String path, final ContentStoreService contentStoreService,
                                   final Context context) {
        return !contentStoreService.getItem(context, path).isFolder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> doExtract(final String path, final ContentStoreService contentStoreService,
                                       final Context context) {
        Content content = contentStoreService.getContent(context, path);
        return Collections.singletonMap(propertyName, Long.toString(content.getLength()));
    }

}
