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
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.service.FieldValueConverter;
import org.craftercms.search.service.SolrDocumentBuilder;
import org.craftercms.search.service.SolrDocumentPostProcessor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link SolrDocumentBuilder}.
 *
 * @author avasquez
 */
public class SolrDocumentBuilderImpl implements SolrDocumentBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SolrDocumentBuilderImpl.class);

    public static final String DEFAULT_ID_FIELD_NAME = "id";
    public static final String DEFAULT_SITE_FIELD_NAME = "crafterSite";
    public static final String DEFAULT_LOCAL_ID_FIELD_NAME = "localId";

    protected String idFieldName;
    protected String siteFieldName;
    protected String localIdFieldName;
    protected ElementParserService parserService;
    protected FieldValueConverter fieldValueConverter;
    protected List<SolrDocumentPostProcessor> postProcessors;

    public SolrDocumentBuilderImpl() {
        idFieldName = DEFAULT_ID_FIELD_NAME;
        siteFieldName = DEFAULT_SITE_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
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

    @Required
    public void setParserService(ElementParserService parserService) {
        this.parserService = parserService;
    }

    @Required
    public void setFieldValueConverter(FieldValueConverter fieldValueConverter) {
        this.fieldValueConverter = fieldValueConverter;
    }

    public void setPostProcessors(List<SolrDocumentPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    @SuppressWarnings("unchecked")
    public SolrInputDocument build(String site, String id, String xml,
                                   boolean ignoreRootInFieldNames) throws SolrDocumentBuildException {
        SAXReader reader = createSAXReader();
        SolrInputDocument solrDoc = new SolrInputDocument();
        String finalId = site + ":" + id;

        logger.debug("Building Solr doc for {}", finalId);

        solrDoc.addField(idFieldName, finalId);
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
            parserService.parse(child, ignoreRootInFieldNames? null : rootElement.getName(), solrDoc);
        }

        postProcess(solrDoc);

        return solrDoc;
    }

    public SolrInputDocument build(String site, String id, Map<String, List<String>> fields) {
        SolrInputDocument solrDoc = new SolrInputDocument();
        String finalId = site + ":" + id;

        logger.debug("Building Solr doc for {}", finalId);

        solrDoc.addField(idFieldName, finalId);
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

    public ModifiableSolrParams buildParams(String site, String id, String prefix, String suffix,
                                            Map<String, List<String>> fields) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        String finalId = site + ":" + id;

        prefix = prefix != null? prefix : "";
        suffix = suffix != null? suffix : "";

        logger.debug("Building params for update request for {}", finalId);

        params.set(prefix + idFieldName + suffix, finalId);
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

    protected void postProcess(SolrInputDocument solrDoc) {
        if (CollectionUtils.isNotEmpty(postProcessors)) {
            for (SolrDocumentPostProcessor postProcessor : postProcessors) {
                postProcessor.postProcess(solrDoc);
            }
        }
    }

    protected SAXReader createSAXReader() {
        SAXReader reader = new SAXReader();
        reader.setEncoding(CharEncoding.UTF_8);
        reader.setMergeAdjacentText(true);

        return reader;
    }

}
