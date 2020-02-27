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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.commons.service.ElementParserService;
import org.craftercms.search.commons.service.impl.AbstractElementParser;
import org.craftercms.search.elasticsearch.jackson.MixedMultivaluedMap;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joseross
 */
public class ElasticsearchElementParserImpl extends AbstractElementParser<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchElementParserImpl.class);

    @Override
    @SuppressWarnings("unchecked")
    public boolean parse(final Element element, final String fieldName, final String parentFieldName,
                         final Map<String, Object> doc, final ElementParserService<Map<String, Object>> parserService) {
        logger.debug("Parsing element '{}'", fieldName);

        if (element.hasContent()) {
            if (element.isTextOnly()) {
                logger.debug("Adding Solr field '{}'", fieldName);

                Object fieldValue = fieldValueConverter.convert(fieldName, element.getText());

                addField(doc, fieldName, fieldValue);
            } else {
                Map<String, Object> map = new MixedMultivaluedMap();
                List<Element> children = element.elements();
                for (Element child : children) {
                    parserService.parse(child, StringUtils.EMPTY, map);
                }
                addField(doc, fieldName, map);
            }
        } else {
            logger.debug("Element '{}' has no content. Ignoring it.", fieldName);
        }

        return true;
    }

    @Override
    protected void addField(final Map<String, Object> doc, final String fieldName, final Object fieldValue) {
        doc.put(fieldName, fieldValue);
    }

}
