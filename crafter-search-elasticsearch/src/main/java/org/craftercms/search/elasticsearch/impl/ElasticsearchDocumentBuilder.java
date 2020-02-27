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

package org.craftercms.search.elasticsearch.impl;

import java.util.Map;

import org.craftercms.search.commons.service.DocumentBuilder;
import org.craftercms.search.commons.service.impl.AbstractDocumentBuilder;
import org.craftercms.search.elasticsearch.jackson.MixedMultivaluedMap;

/**
 * Implementation of {@link DocumentBuilder} for Elasticsearch
 * @author joseross
 */
public class ElasticsearchDocumentBuilder extends AbstractDocumentBuilder<Map<String, Object>> {

    @Override
    protected Map<String, Object> createDoc() {
        return new MixedMultivaluedMap();
    }

    @Override
    protected void addField(final Map<String, Object> doc, final String fieldName, final Object fieldValue) {
        doc.put(fieldName, fieldValue);
    }

}