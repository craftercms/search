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
 * @author joseross
 */
public abstract class AbstractContentTypeMetadataBatchIndexer extends AbstractXmlMetadataBatchIndexer {

    public static final String NAME_PLACEHOLDER = "\\{name\\}";

    protected String configTemplate;

    protected String formDefinitionTemplate;

    public void setConfigTemplate(final String configTemplate) {
        this.configTemplate = configTemplate;
    }

    public void setFormDefinitionTemplate(final String formDefinitionTemplate) {
        this.formDefinitionTemplate = formDefinitionTemplate;
    }

    @Override
    protected Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                              final Context context) {

        Map<String, Object> metadata = new HashMap<>();
        String contentTypeName = item.queryDescriptorValue(fieldXpath);
        String configPath = StringUtils.replaceFirst(configTemplate, NAME_PLACEHOLDER, contentTypeName);
        String definitionPath = StringUtils.replaceFirst(formDefinitionTemplate, NAME_PLACEHOLDER, contentTypeName);

        Item config = contentStoreService.getItem(context, configPath);
        metadata.put("thumbnail", config.queryDescriptorValue("*/image-thumbnail"));

        return metadata;
    }

}
