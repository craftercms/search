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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.craftercms.search.metadata.MetadataExtractor} for content-type metadata
 *
 * @author joseross
 * @since 3.1.1
 */
public class ContentTypeMetadadataExtractor extends AbstractMetadataExtractor {

    public static final String NAME_PLACEHOLDER = "\\{name\\}";
    public static final String FILE_PLACEHOLDER = "\\{file\\}";

    public static final String DEFAULT_CONFIG_TEMPLATE = "/config/studio/content-types{name}/config.xml";
    public static final String DEFAULT_DEFINITION_TEMPLATE = "/config/studio/content-types{name}/form-definition.xml";
    public static final String DEFAULT_THUMBNAIL_TEMPLATE = "/config/studio/content-types{name}/{file}";
    public static final String DEFAULT_THUMBNAIL_XPATH = "*/image-thumbnail";

    public static final String DEFAULT_PROPERTY_NAME_THUMBNAIL = "thumbnail";

    /**
     * The XPath of the field to check
     */
    protected String fieldXpath;

    /**
     * The expected value of the field to check (optional)
     */
    protected String fieldValue;

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

    /**
     * The name of the metadata property for the thumbnail
     */
    protected String propertyNameThumbnail = DEFAULT_PROPERTY_NAME_THUMBNAIL;

    @Required
    public void setFieldXpath(final String fieldXpath) {
        this.fieldXpath = fieldXpath;
    }

    public void setFieldValue(final String fieldValue) {
        this.fieldValue = fieldValue;
    }

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

    public void setPropertyNameThumbnail(final String propertyNameThumbnail) {
        this.propertyNameThumbnail = propertyNameThumbnail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCompatible(final String path, final ContentStoreService contentStoreService,
                                   final Context context) {
        Item item = contentStoreService.getItem(context, path);
        String value = item.queryDescriptorValue(fieldXpath);
        return StringUtils.isEmpty(fieldValue)? StringUtils.isNotEmpty(value) : StringUtils.equals(fieldValue, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> doExtract(final String path, final ContentStoreService contentStoreService,
                                            final Context context) {
        Map<String, String> metadata = new HashMap<>();

        Item item = contentStoreService.getItem(context, path);
        String contentTypeName = item.queryDescriptorValue(fieldXpath);

        getConfigMetadata(contentTypeName, contentStoreService, context, metadata);
        getDefinitionMetadata(contentTypeName, contentStoreService, context, metadata);

        return metadata;
    }

    /**
     * Extracts metadata from the form-definition file
     */
    protected void getDefinitionMetadata(final String contentTypeName, final ContentStoreService contentStoreService,
                                         final Context context, final Map<String, String> metadata) {
        String definitionPath = StringUtils.replaceFirst(definitionTemplate, NAME_PLACEHOLDER, contentTypeName);
        Item definition = contentStoreService.getItem(context, definitionPath);

        //TODO: Define the metadata to extract
    }

    /**
     * Extracts metadata from the configuratiion file
     */
    protected void getConfigMetadata(final String contentTypeName, final ContentStoreService contentStoreService,
                                     final Context context, final Map<String, String> metadata) {
        String configPath = StringUtils.replaceFirst(configTemplate, NAME_PLACEHOLDER, contentTypeName);
        Item config = contentStoreService.getItem(context, configPath);

        String thumbnailFile = config.queryDescriptorValue(thumbnailXpath);
        String thumbnailValue = StringUtils.replaceFirst(thumbnailTemplate, NAME_PLACEHOLDER, contentTypeName);
        thumbnailValue = StringUtils.replaceFirst(thumbnailValue, FILE_PLACEHOLDER, thumbnailFile);
        metadata.put(propertyNameThumbnail, thumbnailValue);
    }

}
