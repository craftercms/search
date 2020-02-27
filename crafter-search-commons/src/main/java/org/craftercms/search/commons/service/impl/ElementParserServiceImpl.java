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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.commons.service.ElementParser;
import org.craftercms.search.commons.service.ElementParserService;
import org.craftercms.search.commons.utils.BooleanUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of the {@link ElementParserService}. Parses the element using a list of
 * {@link ElementParser}s. If first parser returns false (the element was not handled), it calls the second one and
 * so on. It also handles elements tagged with the "indexable" attribute. If the attribute is present, and it's
 * false, the element is not parsed.
 * @param <T> the type of document for the search engine
 *
 * @author avasquez
 */
public class ElementParserServiceImpl<T> implements ElementParserService<T> {

    private static final Logger logger = LoggerFactory.getLogger(ElementParserServiceImpl.class);

    public static final String DEFAULT_FIELD_NAME_SEPARATOR = ".";
    public static final String DEFAULT_INDEXABLE_ATTRIBUTE_NAME = "indexable";

    protected List<ElementParser<T>> parsers;
    protected String fieldNameSeparator;
    protected String indexableAttributeName;

    public ElementParserServiceImpl() {
        fieldNameSeparator = DEFAULT_FIELD_NAME_SEPARATOR;
        indexableAttributeName = DEFAULT_INDEXABLE_ATTRIBUTE_NAME;
    }

    @Required
    public void setParsers(List<ElementParser<T>> parsers) {
        this.parsers = parsers;
    }

    public void setFieldNameSeparator(String fieldNameSeparator) {
        this.fieldNameSeparator = fieldNameSeparator;
    }

    public String getIndexableAttributeName() {
        return indexableAttributeName;
    }

    @Override
    public void parse(Element element, String parentFieldName, T doc) {
        String fieldName = element.getName();

        if (StringUtils.isNotEmpty(parentFieldName)) {
            fieldName = parentFieldName + fieldNameSeparator + fieldName;
        }

        // All fields are indexable unless excluded using the indexable attribute, e.g. <name indexable="false"/>.
        if (BooleanUtils.toBoolean(element.attributeValue(indexableAttributeName), true)) {
            boolean parsed = false;

            for (Iterator<ElementParser<T>> iter = parsers.iterator(); !parsed && iter.hasNext();) {
                parsed = iter.next().parse(element, fieldName, parentFieldName, doc, this);
            }

            if (!parsed) {
                throw new IllegalArgumentException("Unable to find parser for element '" + fieldName + "'");
            }
        } else {
            logger.debug("Element '{}' is tagged as not indexable: it won't be added to the Solr doc", fieldName);
        }
    }

}
