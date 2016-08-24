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
import org.dom4j.Element;

/**
 * {@link DocumentProcessor} that renames elements or fields based on mappings, where a mapping is of the form:
 * XPath of fields to rename -> new field name.
 *
 * @author avasquez
 */
public class FieldRenamingDocumentProcessor implements DocumentProcessor {

    private static final Log logger = LogFactory.getLog(FieldRenamingDocumentProcessor.class);

    private Map<String, String> fieldMappings;

    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Document process(Document document, File file, String rootFolder) {
        if (MapUtils.isNotEmpty(fieldMappings)) {
            for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                String xpath = entry.getKey();
                String newName = entry.getValue();

                if (logger.isDebugEnabled()) {
                    logger.debug("Renaming elements that match XPath " + xpath + " to '" + newName + "' for file " +
                                 file + "...");
                }

                List<Element> elements = document.selectNodes(xpath);
                if (CollectionUtils.isNotEmpty(elements)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Number of elements found to rename: " + elements.size());
                    }

                    for (Element element : elements) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Renaming element " + element.getUniquePath());
                        }

                        element.setName(newName);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Element renamed to " + element.getUniquePath());
                        }
                    }
                }
            }
        }

        return document;
    }

}
