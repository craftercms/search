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

import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.Item;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author joseross
 */
public abstract class AbstractXmlMetadataBatchIndexer extends AbstractMetadataBatchIndexer {


    protected String fieldXpath;
    protected String fieldValue;

    @Required
    public void setFieldXpath(final String fieldXpath) {
        this.fieldXpath = fieldXpath;
    }

    public void setFieldValue(final String fieldValue) {
        this.fieldValue = fieldValue;
    }

    @Override
    protected boolean isCompatible(final Item item) {
        String value = item.queryDescriptorValue(fieldXpath);
        return StringUtils.isEmpty(fieldValue)? StringUtils.isNotEmpty(value) : StringUtils.equals(fieldValue, value);
    }

}
