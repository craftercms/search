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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.utils.BooleanUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
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
 * {@link org.apache.solr.client.solrj.SolrServer#add(SolrInputDocument)}
 * <p/>
 *
 * @author Michael Chen
 * @author Alfonso Vásquez
 */
public class SolrDocumentBuilder {

    public static final String ID_FIELD_NAME = "id";

    private static final Log logger = LogFactory.getLog(SolrDocumentBuilder.class);

    protected String siteFieldName;
    protected String localIdFieldName;
    protected FieldValueConverter fieldValueConverter;
    protected Map<String, String> singleToMultiValueSuffixMappings;

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
     * Sets the default {@link FieldValueConverter} to convert Crafter CMS fields to Solr supported fields.
     */
    @Required
    public void setFieldValueConverter(FieldValueConverter fieldValueConverter) {
        this.fieldValueConverter = fieldValueConverter;
    }

    /**
     * Sets the single field suffix to multi value suffix mappings, to remap fields that are defined as single
     * value but actually have multiple values.
     */
    @Required
    public void setSingleToMultiValueSuffixMappings(Map<String, String> singleToMultiValueSuffixMappings) {
        this.singleToMultiValueSuffixMappings = singleToMultiValueSuffixMappings;
    }

    /**
     * Builds a Solr document from the input XML.
     *
     * @param site                   the Crafter site name the content belongs to
     * @param id                     value for the "localId" field in the Solr document (final doc id is built as
     *                               site:localId)
     * @param xml                    the input XML
     * @param ignoreRootInFieldNames ignore the root element of the input XML in field names
     * @return the Solr document
     * @throws SolrDocumentBuildException
     *
     */
    public SolrInputDocument build(String site, String id, String xml,
                                   boolean ignoreRootInFieldNames) throws SolrDocumentBuildException {
        SAXReader reader = createSAXReader();
        SolrInputDocument solrDoc = new SolrInputDocument();
        String finalId = site + ":" + id;

        if (logger.isDebugEnabled()) {
            logger.debug("Building Solr doc for " + finalId);
        }

        solrDoc.addField(ID_FIELD_NAME, finalId);
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

        List<SolrInputField> renamedFields = new ArrayList<>();

        // Remap single value fields to multi value fields if they have more than one value
        for (Iterator<SolrInputField> iter = solrDoc.iterator(); iter.hasNext();) {
            SolrInputField field = iter.next();
            SolrInputField renamedField = renameFieldIfMultiValue(finalId, solrDoc, field);

            if (renamedField != null) {
                renamedFields.add(renamedField);

                iter.remove();
            }
        }

        for (SolrInputField renamedField : renamedFields) {
            solrDoc.put(renamedField.getName(), renamedField);
        }

        return solrDoc;
    }

    /**
     * Builds a Solr document from the provided multi value map of fields
     *
     * @param site      the Crafter site name the content belongs to
     * @param id        value for the "localId" field in the Solr document (final doc id is built as site:localId)
     * @param fields    fields to add to solr document.
     *
     * @return the Solr document
     */
    public SolrInputDocument build(String site, String id, Map<String, List<String>> fields) {
        SolrInputDocument solrDoc = new SolrInputDocument();
        String finalId = site + ":" + id;

        if (logger.isDebugEnabled()) {
            logger.debug("Building Solr doc for " + finalId);
        }

        solrDoc.addField(ID_FIELD_NAME, finalId);
        solrDoc.addField(siteFieldName, site);
        solrDoc.addField(localIdFieldName, id);

        if (MapUtils.isNotEmpty(fields)) {
            for (Map.Entry<String, List<String>> field : fields.entrySet()) {
                String fieldName = field.getKey();
                List<String> fieldValues = field.getValue();

                for (String value : fieldValues) {
                    solrDoc.addField(fieldName, fieldValueConverter.convert(fieldName, value));
                }
            }
        }

        return solrDoc;
    }

    /**
     * Builds a set of SolrParams for an update request from the provided multi value map of fields
     *
     * @param site      the Crafter site name the content belongs to
     * @param prefix    the common prefix for the fields (null or empty for no prefix)
     * @param suffix    the common suffix for the fields (null or empty for no suffix)
     * @param id        value for the "localId" field in the Solr document (final doc id is built as site:localId)
     * @param fields    fields to add to solr document.
     *
     * @return the Solr document
     */
    public ModifiableSolrParams buildParams(String site, String id, String prefix, String suffix,
                                            Map<String, List<String>> fields) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        String finalId = site + ":" + id;

        prefix = prefix != null? prefix : "";
        suffix = suffix != null? suffix : "";

        if (logger.isDebugEnabled()) {
            logger.debug("Building params for update request for " + finalId);
        }

        params.set(prefix + ID_FIELD_NAME + suffix, finalId);
        params.set(prefix + siteFieldName + suffix, site);
        params.set(prefix + localIdFieldName + suffix, id);

        if (MapUtils.isNotEmpty(fields)) {
            for (Map.Entry<String, List<String>> field : fields.entrySet()) {
                String fieldName = field.getKey();
                List<String> fieldValues = field.getValue();
                String[] values = new String[fieldValues.size()];

                for (int i = 0; i < fieldValues.size(); i++) {
                    values[i] = fieldValueConverter.convert(fieldName, fieldValues.get(i)).toString();
                }

                params.set(prefix + fieldName + suffix, values);
            }
        }

        return params;
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
                    solrDoc.addField(fieldName, fieldValueConverter.convert(fieldName, fieldValue));
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

    protected SolrInputField renameFieldIfMultiValue(String docId, SolrInputDocument doc, SolrInputField field) {
        if (field.getValue() instanceof Collection) {
            String fieldName = field.getName();

            for (Map.Entry<String, String> entry : singleToMultiValueSuffixMappings.entrySet()) {
                String singleValueSuffix = entry.getKey();
                if (fieldName.endsWith(singleValueSuffix)) {
                    String multiValueSuffix = entry.getValue();
                    String fieldWithoutSuffix = StringUtils.substringBefore(fieldName, singleValueSuffix);
                    String newFieldName = fieldWithoutSuffix + multiValueSuffix;

                    logger.warn("Field '" + fieldName + "' is single value, but multiple values where provided in " +
                                docId + ". Renaming to multi value field '" + newFieldName + "'");

                    SolrInputField newField = new SolrInputField(newFieldName);
                    newField.setValue(field.getValue(), field.getBoost());

                    return newField;
                }
            }
        }

        return null;
    }

}
