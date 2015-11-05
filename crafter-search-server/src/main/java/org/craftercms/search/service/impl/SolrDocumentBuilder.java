/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
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

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.utils.BooleanUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Required;

/**
 * <p/>
 * The purpose of this class is to convert a generic XML document:<br>
 * <pre>
 *     &lt;page&gt;
 *      &lt;file-name&gt;index.xml&lt;/file-name&gt;
 *      &lt;showAboutAuthor&gt;false&lt;/showAboutAuthor&gt;
 *      &lt;showSearchBox&gt;true&lt;/showSearchBox&gt;
 *     &lt;/page&gt;
 * </pre>
 * into a Solr Document:
 * <pre>
 *     &lt;add&gt;
 *      &lt;doc&gt;
 *          &lt;field name="file-name"&gt;index.xml&lt;/field&gt;
 *          &lt;field name="showAboutAuthor"&gt;false&lt;/field&gt;
 *          &lt;field name="showSearchBox"&gt;true&lt;/field&gt;
 *      &lt;/doc&gt;
 *     &lt;/add&gt;
 * </pre>
 * <p/>
 * The Solr document is represent in Java as a {@link SolrInputDocument} object, which then can be sent to the server
 * with
 * {@link org.apache.solr.client.solrj.SolrServer#add(org.apache.solr.common.SolrInputDocument)}
 * <p/>
 *
 * @author Michael Chen
 * @author Alfonso Vásquez
 */
public class SolrDocumentBuilder {

    private static final Log logger = LogFactory.getLog(SolrDocumentBuilder.class);

    protected String siteFieldName;
    protected String localIdFieldName;
    protected String dateTimeFieldPattern;
    protected String dateTimeFieldSuffix;
    protected String dateTimeMultivaluedFieldSuffix;
    protected String htmlFieldSuffix;
    protected String multivalueSeparator;

    /**
     * Sets the site field name, which is used to indicate to which site the document belongs to.
     */
    @Required
    public void setSiteFieldName(String siteFieldName) {
        this.siteFieldName = siteFieldName;
    }

    /**
     * Sets the local ID field name, which is the ID of the document within the site.
     */
    @Required
    public void setLocalIdFieldName(String localIdFieldName) {
        this.localIdFieldName = localIdFieldName;
    }

    /**
     * Sets the pattern string for datetime fields tha come in the input XML.
     */
    @Required
    public void setDateTimeFieldPattern(String dateTimeFieldPattern) {
        this.dateTimeFieldPattern = dateTimeFieldPattern;
    }

    /**
     * Sets the suffix of datetime fields (commonly _dt).
     */
    @Required
    public void setDateTimeFieldSuffix(String dateTimeFieldSuffix) {
        this.dateTimeFieldSuffix = dateTimeFieldSuffix;
    }

    /**
     * Sets the suffix of datetime multivalued fields (commonly _dts).
     */
    @Required
    public void setDateTimeMultivaluedFieldSuffix(String dateTimeMultivaluedFieldSuffix) {
        this.dateTimeMultivaluedFieldSuffix = dateTimeMultivaluedFieldSuffix;
    }

    /**
     * Sets the suffix of fields with HTML content (commonly _html).
     */
    @Required
    public void setHtmlFieldSuffix(String htmlFieldSuffix) {
        this.htmlFieldSuffix = htmlFieldSuffix;
    }

    public void setMultivalueSeparator(final String multivalueSeparator) {
        this.multivalueSeparator = multivalueSeparator;
    }

    @PostConstruct
    public void init() {
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    /**
     * Build the Solr document from the input XML.
     *
     * @param site                   the Crafter site name the content belongs to
     * @param id                     value for the "localId" field in the Solr document (final doc id is built as
     *                               site:localId)
     * @param xml                    the input XML
     * @param ignoreRootInFieldNames ignore the root element of the input XML in field names
     * @return the Solr document
     * @throws org.craftercms.search.exception.SolrDocumentBuildException
     *
     */
    public SolrInputDocument build(String site, String id, String xml, boolean ignoreRootInFieldNames) throws
        SolrDocumentBuildException {
        SAXReader reader = createSAXReader();
        SolrInputDocument solrDoc = new SolrInputDocument();
        String finalId = site + ":" + id;

        if (logger.isDebugEnabled()) {
            logger.debug("Building Solr doc for " + finalId);
        }

        solrDoc.addField("id", finalId);
        solrDoc.addField(siteFieldName, site);
        solrDoc.addField(localIdFieldName, id);

        Document document;
        try {
            document = reader.read(new StringReader(xml));
        } catch (DocumentException e) {
            throw new SolrDocumentBuildException("Unable to parse XML into Document object", e);
        }

        Element rootElement = document.getRootElement();
        // Start the recursive call to build the Solr Update Schema
        List<Element> children = rootElement.elements();
        for (Element child : children) {
            build(solrDoc, ignoreRootInFieldNames? null: rootElement.getName(), child);
        }

        return solrDoc;
    }

    protected void build(SolrInputDocument solrDoc, String branchName, Element element) {
        branchName = (StringUtils.isNotEmpty(branchName)? branchName + '.': "") + element.getName();

        // All fields are indexable unless excluded using the indexable attribute, e.g. <name indexable="false"/>.
        // If the element is a branch, skip the children too.
        if (BooleanUtils.toBoolean(element.attributeValue("indexable"), true)) {
            if (element.hasContent()) {
                if (element.isTextOnly()) {
                    // If a field name is not specified as attribute, use the branch name.
                    String fieldName = element.attributeValue("fieldName");
                    if (StringUtils.isEmpty(fieldName)) {
                        fieldName = branchName;
                    }

                    String fieldValue = element.getText();
                    // If fieldName ends with HTML prefix, strip all HTML markup from the field value.
                    if (fieldName.endsWith(htmlFieldSuffix)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Stripping HTML from field '" + fieldName + "'");
                        }

                        fieldValue = stripHtml(fieldName, fieldValue);
                    }
                    // If fieldName ends with datetime prefix, convert the field value to an ISO datetime string.
                    if (fieldName.endsWith(dateTimeFieldSuffix) || fieldName.endsWith(dateTimeMultivaluedFieldSuffix)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Converting '" + fieldValue + "' to ISO datetime");
                        }

                        fieldValue = convertToISODateTimeString(fieldValue);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding field '" + fieldName + "' to the Solr doc");
                    }

                    solrDoc.addField(fieldName, fieldValue);
                } else {
                    List<Element> children = element.elements();
                    for (Element child : children) {
                        build(solrDoc, branchName, child);
                    }
                }
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("Element '" + branchName + "' is not indexable: it won't be added to the Solr doc");
        }
    }

    protected SAXReader createSAXReader() {
        SAXReader reader = new SAXReader();
        reader.setEncoding(CharEncoding.UTF_8);
        reader.setMergeAdjacentText(true);

        return reader;
    }

    protected String stripHtml(String element, String value) throws SolrDocumentBuildException {
        StringReader reader = new StringReader(value);
        //HTMLStripCharFilter htmlStripper = new HTMLStripCharFilter(CharReader.get(reader));
        HTMLStripCharFilter htmlStripper = new HTMLStripCharFilter(reader);
        char[] buffer = new char[1024 * 10];
        StringBuilder strippedValue = new StringBuilder();

        try {
            int charsRead;
            do {
                charsRead = htmlStripper.read(buffer);
                if (charsRead > 0) {
                    strippedValue.append(buffer, 0, charsRead);
                }
            } while (charsRead >= 0);
        } catch (IOException e) {
            throw new SolrDocumentBuildException("Failed to strip the HTML from field '" + element + "'", e);
        }

        return strippedValue.toString();
    }

    protected String convertToISODateTimeString(String dateTimeStr) {
        DateTimeFormatter incomingFormatter = DateTimeFormat.forPattern(dateTimeFieldPattern);
        DateTimeFormatter outgoingFormatter = ISODateTimeFormat.dateTime();

        return outgoingFormatter.print(incomingFormatter.parseDateTime(dateTimeStr));
    }

    /**
     * Build the Solr document for partial update of the search engine's index data of a structured document.
     *
     * @param solrDoc                Existing solr document
     * @param additionalFields       Fields to add to solr document
     * @return the Solr document
     *
     */
    public SolrInputDocument buildPartialUpdateDocument(SolrInputDocument solrDoc, Map<String,
        String> additionalFields) {

        for (Map.Entry<String, String> additionalField : additionalFields.entrySet()) {
            String fieldName = additionalField.getKey();

            String fieldValue = additionalField.getValue();
            // If fieldName ends with HTML prefix, strip all HTML markup from the field value.
            if (fieldName.endsWith(htmlFieldSuffix)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Stripping HTML from field '" + fieldName + "'");
                }

                fieldValue = stripHtml(fieldName, fieldValue);
            }
            // If fieldName ends with datetime prefix, convert the field value to an ISO datetime string.
            if (fieldName.endsWith(dateTimeFieldSuffix) || fieldName.endsWith(dateTimeMultivaluedFieldSuffix)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Converting '" + fieldValue + "' to ISO datetime");
                }

                fieldValue = convertToISODateTimeString(fieldValue);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Adding field '" + fieldName + "' to the Solr doc");
            }
            if (fieldName.endsWith(htmlFieldSuffix) || fieldName.endsWith(dateTimeFieldSuffix)) {
                solrDoc.setField(fieldName, fieldValue);
            } else {
                String[] fieldValues = fieldValue.split(multivalueSeparator);
                if (fieldValues.length > 1) {
                    solrDoc.setField(fieldName, fieldValues);
                } else {
                    solrDoc.setField(fieldName, fieldValue);
                }
            }
        }
        return solrDoc;
    }

    /**
     * Build the Solr document for partial update of the search engine's index data of a structured document.
     *
     * @param request                Content Stream update request for document update
     * @param additionalFields       Fields to add to solr document
     * @return the Solr document
     *
     */
    public ContentStreamUpdateRequest buildPartialUpdateDocument(ContentStreamUpdateRequest request, Map<String,
        String> additionalFields) {

        for (Map.Entry<String, String> additionalField : additionalFields.entrySet()) {
            String fieldName = additionalField.getKey();

            String fieldValue = additionalField.getValue();
            // If fieldName ends with HTML prefix, strip all HTML markup from the field value.
            if (fieldName.endsWith(htmlFieldSuffix)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Stripping HTML from field '" + fieldName + "'");
                }

                fieldValue = stripHtml(fieldName, fieldValue);
            }
            // If fieldName ends with datetime prefix, convert the field value to an ISO datetime string.
            if (fieldName.endsWith(dateTimeFieldSuffix) || fieldName.endsWith(dateTimeMultivaluedFieldSuffix)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Converting '" + fieldValue + "' to ISO datetime");
                }

                fieldValue = convertToISODateTimeString(fieldValue);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Adding field '" + fieldName + "' to the Solr doc");
            }
            if (fieldName.endsWith(htmlFieldSuffix) || fieldName.endsWith(dateTimeFieldSuffix)) {
                request.setParam(ExtractingParams.LITERALS_PREFIX + fieldName, fieldValue);
            } else {
                String[] fieldValues = fieldValue.split(multivalueSeparator);
                if (fieldValues.length > 1) {
                    ModifiableSolrParams params = request.getParams();
                    params.add(ExtractingParams.LITERALS_PREFIX + fieldName, fieldValues);
                } else {
                    request.setParam(ExtractingParams.LITERALS_PREFIX + fieldName, fieldValue);
                }
            }


        }
        return request;
    }
}
