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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.batch.IndexingStatus;
import org.dom4j.Document;
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

    protected ItemProcessor itemProcessor;
    protected List<String> metadataPathPatterns;
    protected List<String> binaryPathPatterns;
    protected List<String> referenceXPaths;
    protected List<String> excludeMetadataProperties;

    public void setItemProcessor(ItemProcessor itemProcessor) {
        this.itemProcessor = itemProcessor;
    }

    public void setItemProcessors(List<ItemProcessor> itemProcessors) {
        this.itemProcessor = new ItemProcessorPipeline(itemProcessors);
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
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                                      String path, boolean delete, IndexingStatus status) throws Exception {
        boolean doUpdate = false;
        String binaryPath = path;
        Content binaryContent = null;
        Map<String, List<String>> metadata = null;

        if (isMetadataFile(path)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Match for metadata file found @ " + getSiteBasedPath(siteName, path));
            }

            Item metadataItem = contentStoreService.getItem(context, null, path, itemProcessor);
            Document metadataDoc = metadataItem.getDescriptorDom();

            if (metadataDoc != null) {
                binaryPath = getBinaryFilePath(metadataDoc);

                if (StringUtils.isNoneEmpty(binaryPath)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Binary file found for metadata file " + getSiteBasedPath(siteName, path) + ": " +
                                     getSiteBasedPath(siteName, binaryPath));
                    }

                    if (!delete) {
                        metadata = extractMetadata(metadataDoc);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Extracted metadata: " + metadata);
                        }

                        binaryContent = contentStoreService.findContent(context, binaryPath);
                        if (binaryContent == null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Binary file " + getSiteBasedPath(siteName, path) + " doesn't exist. Empty content will " +
                                             "be used for the update");
                            }

                            binaryContent = new EmptyContent();
                        }
                    }

                    doUpdate = true;
                }
            } else {
                logger.error("File " + getSiteBasedPath(siteName, path) + " identified as metadata but is not an actual XML descriptor");
            }
        } else if (isBinaryFile(path)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Match for binary file found @ " + getSiteBasedPath(siteName, path));
            }

            if (!delete) {
                binaryContent = contentStoreService.getContent(context, binaryPath);
            }

            doUpdate = true;
        }

        if (doUpdate) {
            if (delete) {
                doDelete(indexId, siteName, binaryPath, status);
            } else {
                doUpdateContent(indexId, siteName, binaryPath, binaryContent, metadata, status);
            }
        }
    }

    protected boolean isMetadataFile(String path) {
        return RegexUtils.matchesAny(path, metadataPathPatterns);
    }

    protected boolean isBinaryFile(String path) {
        return RegexUtils.matchesAny(path, binaryPathPatterns);
    }

    protected String getBinaryFilePath(Document document) {
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
                        logger.debug(String.format("Adding value [%s] for property [%s]", value, key));
                    }

                    metadata.add(key, StringUtils.trim(value));
                }
            }
        }
    }

    public static class EmptyContent implements Content {

        @Override
        public long getLastModified() {
            return System.currentTimeMillis();
        }

        @Override
        public long getLength() {
            return 0;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
        }

    }

}
