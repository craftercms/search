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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.craftercms.search.service.ElementParser;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.utils.BooleanUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ElementParser} that parses elements marked with a "tokenized" attribute. This attribute
 * indicates that the field should be tokenized and analyzed by Solr, and by definition it isn't (like _s fields)
 * so a copy of the field is created with a field name that can actualy be tokenized (like those ending with _t).
 *
 * @author Dejan Brkic
 * @author Alfonso Vásqiuez
 */
public class TokenizedElementParser implements ElementParser {

    private static final Logger logger = LoggerFactory.getLogger(TokenizedElementParser.class);

    public static final String DEFAULT_TOKENIZED_ATTRIBUTE_NAME = "tokenized";

    protected String tokenizedAttributeName;
    protected Map<String, String> fieldSuffixMappings;

    public TokenizedElementParser() {
        tokenizedAttributeName = DEFAULT_TOKENIZED_ATTRIBUTE_NAME;
        fieldSuffixMappings = new HashMap<>(2);

        fieldSuffixMappings.put("_s", "_t");
        fieldSuffixMappings.put("_smv", "_tmv");
    }

    public void setTokenizedAttributeName(String tokenizedAttributeName) {
        this.tokenizedAttributeName = tokenizedAttributeName;
    }

    public void setFieldSuffixMappings(Map<String, String> fieldSuffixMappings) {
        this.fieldSuffixMappings = fieldSuffixMappings;
    }

    @Override
    public boolean parse(Element element, String fieldName, String parentFieldName, SolrInputDocument solrDoc,
                         ElementParserService parserService) {
        Attribute tokenizedAttribute = element.attribute(tokenizedAttributeName);
        if (tokenizedAttribute != null && BooleanUtils.toBoolean(tokenizedAttribute.getValue())) {
            logger.debug("Parsing element '{}' marked to tokenize", fieldName);

            // Remove the attribute so that at the end the element can be parsed as a normal attribute.
            element.remove(tokenizedAttribute);

            String elementName = element.getName();

            for (Map.Entry<String, String> mapping : fieldSuffixMappings.entrySet()) {
                if (elementName.endsWith(mapping.getKey())) {
                    String newElementName = StringUtils.substringBefore(elementName, mapping.getKey()) +
                                            mapping.getValue();

                    Element tokenizedElement = element.createCopy(newElementName);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Created new element for tokenized search: " + tokenizedElement.getName());
                    }

                    parserService.parse(tokenizedElement, parentFieldName, solrDoc);

                    break;
                }
            }

            parserService.parse(element, parentFieldName, solrDoc);

            return true;
        } else {
            return false;
        }
    }

}
