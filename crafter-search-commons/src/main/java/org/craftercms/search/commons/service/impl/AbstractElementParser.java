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
package org.craftercms.search.commons.service.impl;

import java.util.List;

import org.craftercms.search.commons.service.ElementParser;
import org.craftercms.search.commons.service.ElementParserService;
import org.craftercms.search.commons.service.FieldValueConverter;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Base implementation of {@link ElementParser}. For text only fields it adds the field with the provided field
 * name. For elements with children is uses the {@link ElementParserService} to parse the children.
 * @param <T> the type of document for the search engine
 *
 * @author avasquez
 */
public abstract class AbstractElementParser<T> implements ElementParser<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractElementParser.class);

    protected FieldValueConverter fieldValueConverter;

    @Required
    public void setFieldValueConverter(FieldValueConverter fieldValueConverter) {
        this.fieldValueConverter = fieldValueConverter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean parse(Element element, String fieldName, String parentFieldName, T doc,
                         ElementParserService<T> parserService) {
        logger.debug("Parsing element '{}'", fieldName);

        if (element.hasContent()) {
            if (element.isTextOnly()) {
                logger.debug("Adding Solr field '{}'", fieldName);

                Object fieldValue = fieldValueConverter.convert(fieldName, element.getText());

                addField(doc, fieldName, fieldValue);
            } else {
                List<Element> children = element.elements();
                for (Element child : children) {
                    parserService.parse(child, fieldName, doc);
                }
            }
        } else {
            logger.debug("Element '{}' has no content. Ignoring it.");
        }

        return true;
    }

    protected abstract void addField(T doc, String fieldName, Object fieldValue);

}
