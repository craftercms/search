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

import java.io.StringReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.CharEncoding;
import org.craftercms.search.commons.exception.DocumentBuildException;
import org.craftercms.search.commons.service.DocumentBuilder;
import org.craftercms.search.commons.service.DocumentPostProcessor;
import org.craftercms.search.commons.service.ElementParserService;
import org.craftercms.search.commons.service.FieldValueConverter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.xml.sax.SAXException;

/**
 * Base implementation of {@link DocumentBuilder} to add commons fields.
 * @param <T> the type of document for the search engine
 *
 * @author avasquez
 */
public abstract class AbstractDocumentBuilder<T> implements DocumentBuilder<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDocumentBuilder.class);

    public static final String DEFAULT_ID_FIELD_NAME = "id";
    public static final String DEFAULT_ROOT_ID_FIELD_NAME = "rootId";
    public static final String DEFAULT_SITE_FIELD_NAME = "crafterSite";
    public static final String DEFAULT_LOCAL_ID_FIELD_NAME = "localId";
    public static final String DEFAULT_PUBLISHING_DATE_FIELD_NAME = "publishingDate";
    public static final String DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME = "publishingDate_dt";

    protected String idFieldName;
    protected String rootIdFieldName;
    protected String siteFieldName;
    protected String localIdFieldName;
    protected String publishingDateFieldName;
    protected String publishingDateAltFieldName;
    protected ElementParserService<T> parserService;
    protected FieldValueConverter fieldValueConverter;
    protected List<DocumentPostProcessor<T>> postProcessors;
    protected Map<String, String> copyFields;

    public AbstractDocumentBuilder() {
        idFieldName = DEFAULT_ID_FIELD_NAME;
        rootIdFieldName = DEFAULT_ROOT_ID_FIELD_NAME;
        siteFieldName = DEFAULT_SITE_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
        publishingDateFieldName = DEFAULT_PUBLISHING_DATE_FIELD_NAME;
        publishingDateAltFieldName = DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public void setRootIdFieldName(String rootIdFieldName) {
        this.rootIdFieldName = rootIdFieldName;
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

    @Required
    public void setParserService(ElementParserService<T> parserService) {
        this.parserService = parserService;
    }

    @Required
    public void setFieldValueConverter(FieldValueConverter fieldValueConverter) {
        this.fieldValueConverter = fieldValueConverter;
    }

    public void setPostProcessors(List<DocumentPostProcessor<T>> postProcessors) {
        this.postProcessors = postProcessors;
    }

    public void setCopyFields(final Map<String, String> copyFields) {
        this.copyFields = copyFields;
    }

    protected abstract T createDoc();

    protected abstract void addField(T doc, String fieldName, Object fieldValue);

    @SuppressWarnings("unchecked")
    public T build(String site, String id, String xml, boolean ignoreRootInFieldNames) throws DocumentBuildException {
        SAXReader reader = createSAXReader();
        T doc = createDoc();
        String finalId = site + ":" + id;

        logger.debug("Building Solr doc for {}", finalId);

        String now = formatAsIso(Instant.now());

        addField(doc, idFieldName, finalId);
        addField(doc, rootIdFieldName, finalId);
        addField(doc, siteFieldName, site);
        addField(doc, localIdFieldName, id);
        addField(doc, publishingDateFieldName, now);
        addField(doc, publishingDateAltFieldName, now);

        Document document;
        try {
            document = reader.read(new StringReader(xml));
        } catch (DocumentException e) {
            throw new DocumentBuildException("Unable to parse XML into Document object", e);
        }

        Element rootElement = document.getRootElement();

        if(MapUtils.isNotEmpty(copyFields)) {
            addCopyFields(rootElement);
        }

        // Start the recursive call to build the document
        List<Element> children = rootElement.elements();
        for (Element child : children) {
            parserService.parse(child, ignoreRootInFieldNames? null : rootElement.getName(), doc);
        }

        postProcess(doc);

        return doc;
    }

    @SuppressWarnings("unchecked")
    protected void addCopyFields(Element element) {
        if(element.hasContent()) {
            if (element.isTextOnly()) {
                String elementName = element.getName();
                for (Map.Entry<String, String> entry : copyFields.entrySet()) {
                    if (elementName.matches(entry.getKey())) {
                        Element copy = element.createCopy(elementName + entry.getValue());
                        element.getParent().add(copy);
                    }
                }
            } else {
                List<Element> children = element.elements();
                children.forEach(this::addCopyFields);
            }
        }
    }

    public T build(String site, String id, Map<String, List<String>> fields) {
        T doc = createDoc();
        String finalId = site + ":" + id;

        logger.debug("Building Solr doc for {}", finalId);

        String now = formatAsIso(Instant.now());

        addField(doc, idFieldName, finalId);
        addField(doc, rootIdFieldName, finalId);
        addField(doc, siteFieldName, site);
        addField(doc, localIdFieldName, id);
        addField(doc, publishingDateFieldName, now);
        addField(doc, publishingDateAltFieldName, now);

        if (MapUtils.isNotEmpty(fields)) {
            for (Map.Entry<String, List<String>> field : fields.entrySet()) {
                String fieldName = field.getKey();
                List<String> fieldValues = field.getValue();

                for (String value : fieldValues) {
                    addField(doc, fieldName, fieldValueConverter.convert(fieldName, value));
                }
            }
        }

        return doc;
    }

    protected void postProcess(T doc) {
        if (CollectionUtils.isNotEmpty(postProcessors)) {
            for (DocumentPostProcessor<T> postProcessor : postProcessors) {
                postProcessor.postProcess(doc);
            }
        }
    }

    protected SAXReader createSAXReader() {
        SAXReader reader = new SAXReader();
        reader.setEncoding(CharEncoding.UTF_8);
        reader.setMergeAdjacentText(true);
        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        }catch (SAXException ex){
            logger.error("Unable to turn off external entity loading, This could be a security risk.", ex);
        }
        return reader;
    }

    protected String formatAsIso(Temporal temporal) {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(temporal);
    }

}
