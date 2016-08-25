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
package org.craftercms.search.batch.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.batch.utils.XmlUtils;
import org.craftercms.search.batch.utils.xml.DocumentProcessor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that tries to match the update/deleted files against a series of
 * metadata/binary path patterns. If a file is matched as metadata, it's is parsed for a reference to it's binary. If
 * one is found, then an update will be issued for the binary file along with the metadata that is extracted from the
 * metadata file, or a delete for the referenced binary file. If the file is matched as binary, then an update/delete
 * is issued without any metadata.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexer extends AbstractBatchIndexer {

    private static final Log logger = LogFactory.getLog(BinaryFileWithMetadataBatchIndexer.class);

    protected List<DocumentProcessor> documentProcessors;
    protected String charEncoding;
    protected List<String> metadataPathPatterns;
    protected List<String> binaryPathPatterns;
    protected List<String> referenceXPaths;
    protected List<String> excludeMetadataProperties;

    public BinaryFileWithMetadataBatchIndexer() {
        charEncoding = "UTF-8";
    }

    public void setDocumentProcessors(List<DocumentProcessor> documentProcessors) {
        this.documentProcessors = documentProcessors;
    }

    public void setMetadataPathPatterns(List<String> metadataPathPatterns) {
        this.metadataPathPatterns = metadataPathPatterns;
    }

    public void setBinaryPathPatterns(List<String> binaryPathPatterns) {
        this.binaryPathPatterns = binaryPathPatterns;
    }

    public void setReferenceXPaths(List<String> referenceXPaths) {
        this.referenceXPaths = referenceXPaths;
    }

    public void setExcludeMetadataProperties(List<String> excludeMetadataProperties) {
        this.excludeMetadataProperties = excludeMetadataProperties;
    }

    @Override
    protected boolean doSingleFileUpdate(String indexId, String siteName, String rootFolder, String fileName,
                                         boolean delete) throws BatchIndexingException {
        boolean doUpdate = false;
        File file = new File(rootFolder, fileName);
        File updateFile = file;
        String updateFileName = fileName;
        Map<String, List<String>> metadata = null;

        if (isMetadataFile(fileName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Metadata file found: " + file + ". Processing started...");
            }

            try {
                Document metadataDoc = XmlUtils.readXml(file, charEncoding);
                updateFileName = getBinaryFileName(metadataDoc);

                if (StringUtils.isNotBlank(updateFileName)) {
                    updateFile = new File(rootFolder, updateFileName);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Binary file for metadata file " + file + ": " + updateFile);
                    }

                    metadataDoc = processDocument(metadataDoc, file, rootFolder);
                    metadata = extractMetadata(metadataDoc);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Extracted metadata: " + metadata);
                    }

                    if (!updateFile.exists()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Binary file " + updateFile + " doesn't exist. Creating it...");
                        }

                        FileUtils.forceMkdir(updateFile.getParentFile());

                        boolean created = updateFile.createNewFile();
                        if (!created) {
                            throw new IOException("Unable to create binary file " + updateFile);
                        }
                    }

                    doUpdate = true;
                }
            } catch (DocumentException e) {
                logger.warn("Cannot process XML file " + file + ". Continuing index update...", e);
            } catch (IOException e) {
                throw new BatchIndexingException(e.getMessage(), e);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Processing of metadata file " + file + " finished");
            }
        } else if (isBinaryFile(fileName)) {
            doUpdate = true;
        }

        if (doUpdate) {
            if (!delete) {
                return doUpdateFile(indexId, siteName, updateFileName, updateFile, metadata);
            } else {
                return doDelete(indexId, siteName, updateFileName);
            }
        }

        return false;
    }

    protected boolean isMetadataFile(String filePath) {
        return RegexUtils.matchesAny(filePath, metadataPathPatterns);
    }

    protected boolean isBinaryFile(String filePath) {
        return RegexUtils.matchesAny(filePath, binaryPathPatterns);
    }

    protected String getBinaryFileName(Document document) {
        if (CollectionUtils.isNotEmpty(referenceXPaths)) {
            for (String refXpath : referenceXPaths) {
                Node reference = document.selectSingleNode(refXpath);
                if (reference != null) {
                    String referenceValue = reference.getText();
                    if (StringUtils.isNotBlank(referenceValue)) {
                        return referenceValue;
                    }
                }
            }
        }

        return null;
    }

    protected Document processDocument(Document document, File file, String root) throws DocumentException {
        if (CollectionUtils.isNotEmpty(documentProcessors)) {
            for (DocumentProcessor processor : documentProcessors) {
                document = processor.process(document, file, root);
            }
        }

        return document;
    }

    protected Map<String, List<String>> extractMetadata(Document document) {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, StringUtils.EMPTY, metadata);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    protected void extractMetadataFromChildren(Element element, String key, MultiValueMap<String, String> metadata) {
        for (Iterator<Node> iter = element.nodeIterator(); iter.hasNext();) {
            Node node = iter.next();

            if (node instanceof Element) {
                StringBuilder childKey = new StringBuilder(key);

                if (childKey.length() > 0) {
                    childKey.append(".");
                }

                childKey.append(node.getName());

                if (CollectionUtils.isNotEmpty(excludeMetadataProperties) &&
                    !excludeMetadataProperties.contains(childKey.toString())) {
                    extractMetadataFromChildren((Element)node, childKey.toString(), metadata);
                }
            } else {
                String value = node.getText();
                if (StringUtils.isNotBlank(value)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Adding value [%s] for property [%s].", value, key));
                    }

                    metadata.add(key, StringUtils.trim(value));
                }
            }
        }
    }

}
