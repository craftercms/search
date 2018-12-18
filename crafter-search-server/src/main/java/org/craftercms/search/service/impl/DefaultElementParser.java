/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.search.service.impl;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.craftercms.search.service.ElementParser;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.service.FieldValueConverter;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link ElementParser}. For text only fields it adds the field with the provided field
 * name. For elements with children is uses the {@link ElementParserService} to parse the children.
 *
 * @author avasquez
 */
public class DefaultElementParser implements ElementParser {

    private static final Logger logger = LoggerFactory.getLogger(DefaultElementParser.class);

    protected FieldValueConverter fieldValueConverter;

    @Required
    public void setFieldValueConverter(FieldValueConverter fieldValueConverter) {
        this.fieldValueConverter = fieldValueConverter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean parse(Element element, String fieldName, String parentFieldName, SolrInputDocument solrDoc,
                         ElementParserService parserService) {
        logger.debug("Parsing element '{}'", fieldName);

        if (element.hasContent()) {
            if (element.isTextOnly()) {
                logger.debug("Adding Solr field '{}'", fieldName);

                Object fieldValue = fieldValueConverter.convert(fieldName, element.getText());

                solrDoc.addField(fieldName, fieldValue);
            } else {
                List<Element> children = element.elements();
                for (Element child : children) {
                    parserService.parse(child, fieldName, solrDoc);
                }
            }
        } else {
            logger.debug("Element '{}' has no content. Ignoring it.");
        }

        return true;
    }

}
