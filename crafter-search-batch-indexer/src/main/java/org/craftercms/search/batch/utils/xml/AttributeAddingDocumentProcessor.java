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
package org.craftercms.search.batch.utils.xml;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Created by alfonsovasquez on 23/8/16.
 */
public class AttributeAddingDocumentProcessor implements DocumentProcessor {

    private static final Log logger = LogFactory.getLog(AttributeAddingDocumentProcessor.class);

    public static final String DEFAULT_ATTRIBUTE_VALUE_SEPARATOR = ":";

    protected Map<String, Map<String, String>> attributeMappings;
    protected String attributeSeparator;

    public AttributeAddingDocumentProcessor() {
        attributeSeparator = DEFAULT_ATTRIBUTE_VALUE_SEPARATOR;
    }

    public void setAttributeMappings(Map<String, Map<String, String>> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }

    public void setAttributeSeparator(String attributeSeparator) {
        this.attributeSeparator = attributeSeparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        if (MapUtils.isNotEmpty(attributeMappings)) {
            for (Map.Entry<String, Map<String, String>> mapping : attributeMappings.entrySet()) {
                String xpath = mapping.getKey();
                Map<String, String> attributes = mapping.getValue();

                if (MapUtils.isNotEmpty(attributes)) {
                    List<Element> matchedElements = document.selectNodes(xpath);

                    if (CollectionUtils.isNotEmpty(matchedElements)) {
                        for (Element element : matchedElements) {
                            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                                String name = attribute.getKey();
                                String value = attribute.getValue();

                                element.add(DocumentHelper.createAttribute(element, name, value));

                                if (logger.isDebugEnabled()) {
                                    logger.debug("Added attribute " + name + "=" + value + " to element " +
                                                 element.getUniquePath());
                                }
                            }
                        }
                    }
                }
            }
        }

        return document;
    }



}
