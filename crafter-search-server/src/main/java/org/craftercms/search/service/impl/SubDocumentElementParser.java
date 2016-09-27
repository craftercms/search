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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.craftercms.search.service.ElementParser;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.utils.BooleanUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_LOCAL_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_PUBLISHING_DATE_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_SITE_FIELD_NAME;

/**
 * {@link ElementParser} implementation that parses elements that ara tagged with an attribute that indicates
 * that it's sub-elements should be treated as sub-documents or child documents.
 *
 * @author avasquez
 */
public class SubDocumentElementParser implements ElementParser {

    private static final Logger logger = LoggerFactory.getLogger(SubDocumentElementParser.class);

    public static final String DEFAULT_CONTENT_TYPE_FIELD_NAME = "content-type";
    public static final String DEFAULT_PARENT_ID_FIELD_NAME = "parentId";

    public static final String DEFAULT_CONTAINS_SUB_DOCUMENTS_ATTRIBUTE_NAME = "sub-docs";
    public static final String DEFAULT_SUB_DOCUMENT_ELEMENT_NAME = "item";

    protected String idFieldName;
    protected String siteFieldName;
    protected String localIdFieldName;
    protected String publishingDateFieldName;
    protected String publishingDateAltFieldName;
    protected String parentIdFieldName;
    protected String contentTypeFieldName;
    protected String containsSubDocumentsAttributeName;
    protected String subDocumentElementName;

    public SubDocumentElementParser() {
        idFieldName = DEFAULT_ID_FIELD_NAME;
        siteFieldName = DEFAULT_SITE_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
        publishingDateFieldName = DEFAULT_PUBLISHING_DATE_FIELD_NAME;
        publishingDateAltFieldName = DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME;
        parentIdFieldName = DEFAULT_PARENT_ID_FIELD_NAME;
        contentTypeFieldName = DEFAULT_CONTENT_TYPE_FIELD_NAME;
        containsSubDocumentsAttributeName = DEFAULT_CONTAINS_SUB_DOCUMENTS_ATTRIBUTE_NAME;
        subDocumentElementName = DEFAULT_SUB_DOCUMENT_ELEMENT_NAME;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public void setSiteFieldName(String siteFieldName) {
        this.siteFieldName = siteFieldName;
    }

    public void setLocalIdFieldName(String localIdFieldName) {
        this.localIdFieldName = localIdFieldName;
    }

    public void setPublishingDateFieldName(String publishingDateFieldName) {
        this.publishingDateFieldName = publishingDateFieldName;
    }

    public void setPublishingDateAltFieldName(String publishingDateAltFieldName) {
        this.publishingDateAltFieldName = publishingDateAltFieldName;
    }

    public void setContentTypeFieldName(String contentTypeFieldName) {
        this.contentTypeFieldName = contentTypeFieldName;
    }

    public void setParentIdFieldName(String parentIdFieldName) {
        this.parentIdFieldName = parentIdFieldName;
    }

    public void setContainsSubDocumentsAttributeName(String containsSubDocumentsAttributeName) {
        this.containsSubDocumentsAttributeName = containsSubDocumentsAttributeName;
    }

    public void setSubDocumentElementName(String subDocumentElementName) {
        this.subDocumentElementName = subDocumentElementName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean parse(Element element, String fieldName, String parentFieldName, SolrInputDocument solrDoc,
                         ElementParserService parserService) {
        if (BooleanUtils.toBoolean(element.attributeValue(containsSubDocumentsAttributeName), false)) {
            logger.debug("Parsing element '{}' with sub-doc elements", fieldName);

            String site = (String)solrDoc.getFieldValue(siteFieldName);
            String parentId = (String)solrDoc.getFieldValue(idFieldName);
            String parentLocalId = (String)solrDoc.getFieldValue(localIdFieldName);
            String parentPublishingDate = (String)solrDoc.getFieldValue(publishingDateFieldName);
            String parentContentType = (String)solrDoc.getFieldValue(contentTypeFieldName);

            List<Element> subDocElements = element.elements(subDocumentElementName);
            if (CollectionUtils.isNotEmpty(subDocElements)) {
                int idx = 0;

                for (Element subDocElement : subDocElements) {
                    String suffix = "_" + fieldName + "_" + idx;
                    String id = parentId + suffix;
                    String localId = parentLocalId + suffix;

                    logger.debug("Building Solr sub-doc for {}", id);

                    SolrInputDocument subSolrDoc = new SolrInputDocument();
                    subSolrDoc.addField(siteFieldName, site);
                    subSolrDoc.addField(idFieldName, id);
                    subSolrDoc.addField(localIdFieldName, localId);
                    subSolrDoc.addField(publishingDateFieldName, parentPublishingDate);
                    subSolrDoc.addField(publishingDateAltFieldName, parentPublishingDate);
                    subSolrDoc.addField(parentIdFieldName, parentId);

                    if (StringUtils.isNotEmpty(parentContentType)) {
                        subSolrDoc.addField(contentTypeFieldName, parentContentType + "_" + fieldName);
                    }

                    parserService.parse(subDocElement, fieldName, subSolrDoc);

                    solrDoc.addChildDocument(subSolrDoc);

                    idx++;
                }
            } else {
                logger.warn("Element '{}' doesn't contain any sub-doc element '{}'. Ignoring it.", fieldName,
                            subDocumentElementName);
            }

            return true;
        } else {
            return false;
        }
    }

}
