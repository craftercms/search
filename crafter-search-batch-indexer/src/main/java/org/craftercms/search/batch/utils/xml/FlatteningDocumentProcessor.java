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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.batch.utils.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * {@link DocumentProcessor} that parses the document for {@code &gt;include&lt} elements and replaces them for the
 * actual referenced XML components.
 *
 * @author avasquez
 */
public class FlatteningDocumentProcessor implements DocumentProcessor {

    public static final String DEFAULT_CHAR_ENCODING = CharEncoding.UTF_8;
    public static final String DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY = "//include";
    public static final String DEFAULT_DISABLE_FLATTENING_ELEMENT  = "disabledFlattening";

    private static final Log logger = LogFactory.getLog(FlatteningDocumentProcessor.class);

    protected String charEncoding;
    protected String includeElementXPathQuery;
    protected String disableFlatteningElement;

    public FlatteningDocumentProcessor() {
        charEncoding = DEFAULT_CHAR_ENCODING;
        includeElementXPathQuery = DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY;
        disableFlatteningElement = DEFAULT_DISABLE_FLATTENING_ELEMENT;
    }

    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        this.includeElementXPathQuery = includeElementXPathQuery;
    }

    public void setDisableFlatteningElement(String disableFlatteningElement) {
        this.disableFlatteningElement = disableFlatteningElement;
    }

    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        flattenXml(document, file, rootFolder, new ArrayList<File>());

        return document;
    }

    protected Document flattenXml(Document document, File file, String rootFolder,
                                  List<File> flattenedFiles) throws DocumentException {
        if (logger.isDebugEnabled()) {
            logger.debug("Flattening XML file " + file + "...");
        }

        flattenedFiles.add(file);

        List<Element> includeElements = document.selectNodes(includeElementXPathQuery);

        if (CollectionUtils.isEmpty(includeElements)) {
            return document;
        }

        for (Element includeElement : includeElements) {
            boolean flatteningDisabled = false;
            Element parent = includeElement.getParent();
            Element disableFlatteningNode = parent.element(disableFlatteningElement);

            if (disableFlatteningNode != null) {
                String disableFlattening = disableFlatteningNode.getText();
                flatteningDisabled = Boolean.parseBoolean(disableFlattening);
            }

            if (!flatteningDisabled) {
                String includeSrcPath = rootFolder + File.separatorChar + includeElement.getTextTrim();
                if (StringUtils.isEmpty(includeSrcPath)) {
                    continue;
                }

                File includeFile = new File(includeSrcPath);
                if (includeFile.exists()) {
                    if (!flattenedFiles.contains(includeFile)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Include found in " + file + ": " + includeSrcPath);
                        }

                        Document includeDocument = flattenXml(XmlUtils.readXml(includeFile, charEncoding),
                                                              includeFile, rootFolder, flattenedFiles);
                        doInclude(includeElement, includeDocument);
                    } else {
                        logger.warn("Circular inclusion detected. File " + includeFile + " already included");
                    }
                } else {
                    logger.warn("No file found for include at " + includeFile);
                }
            }
        }

        return document;
    }

    protected void doInclude(Element includeElement, Document includeSrc) {
        List<Node> includeElementParentChildren = includeElement.getParent().content();
        int includeElementIdx = includeElementParentChildren.indexOf(includeElement);
        Element includeSrcRootElement = includeSrc.getRootElement().createCopy();

        // Remove the <include> element
        includeElementParentChildren.remove(includeElementIdx);

        // Add the src root element
        includeElementParentChildren.add(includeElementIdx, includeSrcRootElement);
    }

}
