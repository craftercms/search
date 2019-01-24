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

package org.craftercms.search.batch.impl;

import java.util.Collections;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;

/**
 * Implementation of {@link AbstractMetadataBatchIndexer} that handles file sizes
 * @author joseross
 */
public abstract class AbstractFileSizeBatchIndexer extends AbstractMetadataBatchIndexer {

    public static final String PROPERTY_NAME_LENGTH = "contentLength";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                              final Context context) {
        Content content = contentStoreService.getContent(context, item.getUrl());
        return Collections.singletonMap(PROPERTY_NAME_LENGTH, content.getLength());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCompatible(final Item item) {
        return !item.isFolder();
    }

}
