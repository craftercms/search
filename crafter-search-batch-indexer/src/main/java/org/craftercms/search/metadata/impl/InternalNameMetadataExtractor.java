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

package org.craftercms.search.metadata.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.craftercms.search.metadata.MetadataExtractor} for the internalName field
 *
 * @author joseross
 * @since 3.1.1
 */
public class InternalNameMetadataExtractor extends AbstractMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(InternalNameMetadataExtractor.class);

    public static final String DEFAULT_PROPERTY_NAME = "internalName";

    /**
     * The name of the metadata property to return
     */
    protected String propertyName = DEFAULT_PROPERTY_NAME;

    /**
     * The XPath selector used to extract the internal name from descriptors
     */
    protected String internalNameFieldXpath;

    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Required
    public void setInternalNameFieldXpath(final String internalNameFieldXpath) {
        this.internalNameFieldXpath = internalNameFieldXpath;
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
        Item item = contentStoreService.getItem(context, path);
        String internalName = item.queryDescriptorValue(internalNameFieldXpath);
        if(StringUtils.isEmpty(internalName)) {
            logger.debug("Internal name not found in descriptor, using filename as fallback");
            internalName = FilenameUtils.getName(item.getName());
        }
        return Collections.singletonMap(propertyName, internalName);
    }

}
