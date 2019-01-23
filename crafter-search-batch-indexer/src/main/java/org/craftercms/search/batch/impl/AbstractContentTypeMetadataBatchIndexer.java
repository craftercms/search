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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;

/**
 * Implementation of {@link AbstractXmlMetadataBatchIndexer} that handles Crafter's content-type metadata
 * @author joseross
 */
public abstract class AbstractContentTypeMetadataBatchIndexer extends AbstractXmlMetadataBatchIndexer {

    public static final String NAME_PLACEHOLDER = "\\{name\\}";
    public static final String FILE_PLACEHOLDER = "\\{file\\}";

    public static final String DEFAULT_CONFIG_TEMPLATE = "/config/studio/content-types{name}/config.xml";
    public static final String DEFAULT_DEFINITION_TEMPLATE = "/config/studio/content-types{name}/form-definition.xml";
    public static final String DEFAULT_THUMBNAIL_TEMPLATE = "/config/studio/content-types{name}/{fle}";
    public static final String DEFAULT_THUMBNAIL_XPATH = "*/image-thumbnail";

    public static final String PROPERTY_NAME_THUMBNAIL = "thumbnail";

    /**
     * The pattern for the configuration file
     */
    protected String configTemplate = DEFAULT_CONFIG_TEMPLATE;

    /**
     * The pattern for the form definition file
     */
    protected String definitionTemplate = DEFAULT_DEFINITION_TEMPLATE;

    /**
     * The pattern for the thumbnail file
     */
    protected String thumbnailTemplate = DEFAULT_THUMBNAIL_TEMPLATE;

    /**
     * The XPath for the thumbnail field
     */
    protected String thumbnailXpath = DEFAULT_THUMBNAIL_XPATH;

    public void setConfigTemplate(final String configTemplate) {
        this.configTemplate = configTemplate;
    }

    public void setDefinitionTemplate(final String definitionTemplate) {
        this.definitionTemplate = definitionTemplate;
    }

    public void setThumbnailTemplate(final String thumbnailTemplate) {
        this.thumbnailTemplate = thumbnailTemplate;
    }

    public void setThumbnailXpath(final String thumbnailXpath) {
        this.thumbnailXpath = thumbnailXpath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                              final Context context) {

        Map<String, Object> metadata = new HashMap<>();

        String contentTypeName = item.queryDescriptorValue(fieldXpath);

        getConfigMetadata(contentTypeName, contentStoreService, context, metadata);
        getDefinitionMetadata(contentTypeName, contentStoreService, context, metadata);

        return metadata;
    }

    protected void getDefinitionMetadata(final String contentTypeName, final ContentStoreService contentStoreService,
                                         final Context context, final Map<String, Object> metadata) {
        String definitionPath = StringUtils.replaceFirst(definitionTemplate, NAME_PLACEHOLDER, contentTypeName);
        Item definition = getItem(contentStoreService, context, definitionPath);

        //TODO: Extract metadata from the form definition
    }

    protected void getConfigMetadata(final String contentTypeName, final ContentStoreService contentStoreService,
                                     final Context context, final Map<String, Object> metadata) {
        String configPath = StringUtils.replaceFirst(configTemplate, NAME_PLACEHOLDER, contentTypeName);
        Item config = getItem(contentStoreService, context, configPath);

        String thumbnailFile = config.queryDescriptorValue(thumbnailXpath);
        metadata.put(PROPERTY_NAME_THUMBNAIL,
            StringUtils.replaceFirst(thumbnailTemplate, FILE_PLACEHOLDER, thumbnailFile));
    }

}
