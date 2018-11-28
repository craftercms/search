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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.craftercms.search.service.ElementParser;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.utils.BooleanUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of the {@link ElementParserService}. Parses the element using a list of
 * {@link ElementParser}s. If first parser returns false (the element was not handled), it calls the second one and
 * so on. It also handles elements tagged with the "indexable" attribute. If the attribute is present, and it's
 * false, the element is not parsed.
 *
 * @author avasquez
 */
public class ElementParserServiceImpl implements ElementParserService {

    private static final Logger logger = LoggerFactory.getLogger(ElementParserServiceImpl.class);

    public static final String DEFAULT_FIELD_NAME_SEPARATOR = ".";
    public static final String DEFAULT_INDEXABLE_ATTRIBUTE_NAME = "indexable";

    protected List<ElementParser> parsers;
    protected String fieldNameSeparator;
    protected String indexableAttributeName;

    public ElementParserServiceImpl() {
        fieldNameSeparator = DEFAULT_FIELD_NAME_SEPARATOR;
        indexableAttributeName = DEFAULT_INDEXABLE_ATTRIBUTE_NAME;
    }

    @Required
    public void setParsers(List<ElementParser> parsers) {
        this.parsers = parsers;
    }

    public void setFieldNameSeparator(String fieldNameSeparator) {
        this.fieldNameSeparator = fieldNameSeparator;
    }

    public String getIndexableAttributeName() {
        return indexableAttributeName;
    }

    @Override
    public void parse(Element element, String parentFieldName, SolrInputDocument solrDoc) {
        String fieldName = element.getName();

        if (StringUtils.isNotEmpty(parentFieldName)) {
            fieldName = parentFieldName + fieldNameSeparator + fieldName;
        }

        // All fields are indexable unless excluded using the indexable attribute, e.g. <name indexable="false"/>.
        if (BooleanUtils.toBoolean(element.attributeValue(indexableAttributeName), true)) {
            boolean parsed = false;

            for (Iterator<ElementParser> iter = parsers.iterator(); !parsed && iter.hasNext();) {
                parsed = iter.next().parse(element, fieldName, parentFieldName, solrDoc, this);
            }

            if (!parsed) {
                throw new IllegalArgumentException("Unable to find parser for element '" + fieldName + "'");
            }
        } else {
            logger.debug("Element '{}' is tagged as not indexable: it won't be added to the Solr doc", fieldName);
        }
    }

}
