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
 * Created by alfonsovasquez on 23/8/16.
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
