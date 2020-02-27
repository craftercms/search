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

package org.craftercms.search.batch.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.file.stores.RemoteFile;
import org.craftercms.commons.file.stores.RemoteFileResolver;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.metadata.impl.AbstractMetadataCollector;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.activation.FileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.craftercms.search.batch.utils.IndexingUtils.isMimeTypeSupported;


/**
 * {@link BatchIndexer} that tries to match binary files with metadata files. A
 * metadata
 * file can reference several binary files, and a binary file can be referenced by several metadata files. Also, this
 * indexer supports the concept of "child" binaries, where the parent is the metadata file and the binary file only
 * exists in the index as long as the metadata file exists and it references the binary.
 *
 * @author avasquez
 */
public abstract class AbstractBinaryFileWithMetadataBatchIndexer
    extends AbstractMetadataCollector implements BatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBinaryFileWithMetadataBatchIndexer.class);

    public static final String DEFAULT_METADATA_PATH_FIELD_NAME = "metadataPath";
    public static final String DEFAULT_LOCAL_ID_FIELD_NAME = "localId";

    protected List<String> supportedMimeTypes;
    protected FileTypeMap mimeTypesMap;
    protected RemoteFileResolver remoteFileResolver;
    protected ItemProcessor itemProcessor;
    protected List<String> metadataPathPatterns;
    protected List<String> binaryPathPatterns;
    protected List<String> remoteBinaryPathPatterns;
    protected List<String> childBinaryPathPatterns;
    protected List<String> referenceXPaths;
    protected List<String> includePropertyPatterns;
    protected List<String> excludePropertyPatterns;
    @Deprecated
    protected List<String> excludeMetadataProperties;
    protected String metadataPathFieldName;
    protected String localIdFieldName;
    protected long maxFileSize;

    public AbstractBinaryFileWithMetadataBatchIndexer() {
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
        metadataPathFieldName = DEFAULT_METADATA_PATH_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    public void setRemoteFileResolver(RemoteFileResolver remoteFileResolver) {
        this.remoteFileResolver = remoteFileResolver;
    }

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

    public void setRemoteBinaryPathPatterns(List<String> remoteBinaryPathPatterns) {
        this.remoteBinaryPathPatterns = remoteBinaryPathPatterns;
    }

    public void setChildBinaryPathPatterns(List<String> childBinaryPathPatterns) {
        this.childBinaryPathPatterns = childBinaryPathPatterns;
    }

    public void setReferenceXPaths(List<String> referenceXPaths) {
        this.referenceXPaths = referenceXPaths;
    }

    public void setIncludePropertyPatterns(List<String> includePropertyPatterns) {
        this.includePropertyPatterns = includePropertyPatterns;
    }

    public void setExcludePropertyPatterns(List<String> excludePropertyPatterns) {
        this.excludePropertyPatterns = excludePropertyPatterns;
    }

    @Deprecated
    public void setExcludeMetadataProperties(List<String> excludeMetadataProperties) {
        this.excludeMetadataProperties = excludeMetadataProperties;
    }

    public void setMetadataPathFieldName(String metadataPathFieldName) {
        this.metadataPathFieldName = metadataPathFieldName;
    }

    public void setLocalIdFieldName(String localIdFieldName) {
        this.localIdFieldName = localIdFieldName;
    }

    @Required
    public void setMaxFileSize(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void updateIndex(String indexId, String siteName,
                            ContentStoreService contentStoreService, Context context, UpdateSet updateSet,
                            UpdateStatus updateStatus) throws BatchIndexingException {
        doUpdates(indexId, siteName, contentStoreService, context, updateSet, updateStatus);
        doDeletes(indexId, siteName, contentStoreService, context, updateSet.getDeletePaths(), updateStatus);
    }

    protected void doUpdates(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             UpdateSet updateSet, UpdateStatus updateStatus) {
        List<String> updatePaths = updateSet.getUpdatePaths();
        Set<String> metadataUpdatePaths = new LinkedHashSet<>();
        Set<String> binaryUpdatePaths = new LinkedHashSet<>();

        for (String path : updatePaths) {
            if (isMetadata(path)) {
                metadataUpdatePaths.add(path);
            } else if (isBinary(path)) {
                binaryUpdatePaths.add(path);
            }
        }

        for (String metadataPath : metadataUpdatePaths) {
            Collection<String> newBinaryPaths = Collections.emptyList();
            List<String> previousBinaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, metadataPath);
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);

            if (metadataDoc != null) {
                newBinaryPaths = getBinaryFilePaths(metadataDoc);
            }

            // If there are previous binaries that are not associated to the metadata anymore, reindex them without
            // metadata or delete them if they're child binaries.
            updatePreviousBinaries(indexId, siteName, metadataPath, previousBinaryPaths, newBinaryPaths,
                binaryUpdatePaths, context, contentStoreService, updateSet.getUpdateDetail(metadataPath), updateStatus);

            // Index the new associated binaries
            if (CollectionUtils.isNotEmpty(newBinaryPaths)) {
                MultiValueMap<String, String> metadata = extractMetadata(metadataPath, metadataDoc);

                for (String newBinaryPath : newBinaryPaths) {
                    binaryUpdatePaths.remove(newBinaryPath);

                    Map<String, String> additionalFields = collectMetadata(metadataPath, contentStoreService, context);
                    metadata.setAll(additionalFields);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, newBinaryPath, metadata,
                        updateSet.getUpdateDetail(metadataPath), updateStatus);
                }
            }
        }

        for (String binaryPath : binaryUpdatePaths) {
            String metadataPath = searchMetadataPathFromBinaryPath(indexId, siteName, binaryPath);
            if (StringUtils.isNotEmpty(metadataPath)) {
                // If the binary file has an associated metadata, index the file with the metadata
                Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);
                if (metadataDoc != null) {
                    MultiValueMap<String, String> metadata = extractMetadata(metadataPath, metadataDoc);

                    Map<String, String> additionalFields = collectMetadata(metadataPath, contentStoreService, context);
                    metadata.setAll(additionalFields);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, binaryPath,
                                             metadata, updateSet.getUpdateDetail(metadataPath), updateStatus);
                }
            } else {
                // If not, index by itself
                updateBinary(indexId, siteName, contentStoreService, context, binaryPath,
                    updateSet.getUpdateDetail(binaryPath), updateStatus);
            }
        }
    }

    protected void updatePreviousBinaries(String indexId, String siteName, String metadataPath,
                                          List<String> previousBinaryPaths, Collection<String> newBinaryPaths,
                                          Set<String> binaryUpdatePaths, Context context,
                                          ContentStoreService contentStoreService,
                                          UpdateDetail updateDetail, UpdateStatus updateStatus) {
        if (CollectionUtils.isNotEmpty(previousBinaryPaths)) {
            for (String previousBinaryPath : previousBinaryPaths) {
                if (CollectionUtils.isEmpty(newBinaryPaths) || !newBinaryPaths.contains(previousBinaryPath)) {
                    binaryUpdatePaths.remove(previousBinaryPath);

                    if (isChildBinary(previousBinaryPath)) {
                        logger.debug(
                                "Reference of child binary {} removed from  parent {}. Deleting binary from index...",
                                previousBinaryPath, metadataPath);

                        doDelete(indexId, siteName, previousBinaryPath, updateStatus);
                    } else {
                        logger.debug("Reference of binary {} removed from {}. Reindexing without metadata...",
                                     previousBinaryPath, metadataPath);

                        updateBinary(indexId, siteName, contentStoreService, context,
                            previousBinaryPath, updateDetail, updateStatus);
                    }
                }
            }
        }
    }

    protected abstract void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                                     final UpdateStatus updateStatus);

    protected void doDeletes(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             List<String> deletePaths, UpdateStatus updateStatus) {
        for (String path : deletePaths) {
            if (isMetadata(path)) {
                List<String> binaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, path);
                for (String binaryPath : binaryPaths) {
                    if (isChildBinary(binaryPath)) {
                        logger.debug("Parent of binary {} deleted. Deleting child binary too", binaryPath);

                        // If the binary is a child binary, when the metadata file is deleted, then delete it
                        doDelete(indexId, siteName, binaryPath, updateStatus);
                    } else {
                        logger.debug("Metadata with reference of binary {} deleted. Reindexing without metadata...",
                                     binaryPath);

                        // Else, update binary without metadata
                        updateBinary(indexId, siteName, contentStoreService, context, binaryPath,null, updateStatus);
                    }
                }
            } else if (isBinary(path)) {
                doDelete(indexId, siteName, path, updateStatus);
            }
        }
    }

    protected boolean isMetadata(String path) {
        return RegexUtils.matchesAny(path, metadataPathPatterns);
    }

    protected boolean isBinary(String path) {
        return RegexUtils.matchesAny(path, binaryPathPatterns) &&
               isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, path);
    }

    protected boolean isRemoteBinary(String path) {
        return RegexUtils.matchesAny(path, remoteBinaryPathPatterns);
    }

    protected boolean isChildBinary(String path) {
        return RegexUtils.matchesAny(path, childBinaryPathPatterns);
    }


    protected abstract List<String> searchBinaryPathsFromMetadataPath(String indexId, String siteName,
                                                             String metadataPath);

    protected abstract String searchMetadataPathFromBinaryPath(String indexId, String siteName, String binaryPath);

    protected Document loadMetadata(ContentStoreService contentStoreService, Context context, String siteName,
                                    String metadataPath) {
        try {
            Document metadataDoc = contentStoreService.getItem(context, null, metadataPath,
                                                               itemProcessor).getDescriptorDom();
            if (metadataDoc != null) {
                return metadataDoc;
            } else {
                logger.error("File {}:{} is not a metadata XML descriptor", siteName, metadataPath);
            }
        } catch (Exception e) {
            logger.error("Error retrieving metadata file @ {}:{}", siteName, metadataPath, e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected Collection<String> getBinaryFilePaths(Document document) {
        if (CollectionUtils.isNotEmpty(referenceXPaths)) {
            Set<String> binaryPaths = new LinkedHashSet<>();
            for (String refXpath : referenceXPaths) {
                List<Node> references = document.selectNodes(refXpath);
                if (CollectionUtils.isNotEmpty(references)) {
                    for (Node reference : references) {
                        String referenceValue = reference.getText();
                        if (StringUtils.isNotBlank(referenceValue) &&
                            isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, referenceValue)) {
                            binaryPaths.add(referenceValue);
                        }
                    }
                }
            }
            return binaryPaths;
        }

        return null;
    }

    protected void updateBinaryWithMetadata(String indexId, String siteName,
                                            ContentStoreService contentStoreService, Context context,
                                            String binaryPath, MultiValueMap<String, String> metadata,
                                            UpdateDetail updateDetail, UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.debug("Indexing remote file {}", binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                if(remoteFile.getContentLength() > maxFileSize) {
                    logger.warn("Skipping large binary file @ {}", binaryPath);
                } else {
                    doUpdateContent(indexId, siteName, binaryPath, remoteFile.toResource(), metadata, updateDetail,
                                    updateStatus);
                }
            } else {
                Content binaryContent = contentStoreService.findContent(context, binaryPath);
                if (binaryContent == null) {
                    logger.debug("No binary file found @ {}:{}. Empty content will be used for the update", siteName,
                                 binaryPath);

                    binaryContent = new EmptyContent();
                }

                if(binaryContent.getLength() > maxFileSize) {
                    logger.warn("Skipping large binary file @ {}", binaryPath);
                } else {
                    doUpdateContent(indexId, siteName, binaryPath, binaryContent, metadata, updateDetail, updateStatus);
                }
            }
        } catch (Exception e) {
            logger.error("Error when trying to send index update with metadata for binary file {}:{}", siteName,
                         binaryPath, e);
        }
    }

    protected abstract void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                            final Resource resource, final MultiValueMap<String, String> metadata,
                                            final UpdateDetail updateDetail, final UpdateStatus updateStatus);

    protected abstract void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                            final Content content, final MultiValueMap<String, String> metadata,
                                            final UpdateDetail updateDetail, final UpdateStatus updateStatus);

    protected void updateBinary(String indexId, String siteName, ContentStoreService contentStoreService,
                                Context context, String binaryPath, UpdateDetail updateDetail,
                                UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.info("Indexing remote file {}", binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                if(remoteFile.getContentLength() > maxFileSize) {
                    logger.warn("Skipping large binary file @ {}", binaryPath);
                } else {
                    doUpdateContent(indexId, siteName, binaryPath, remoteFile.toResource(), updateDetail, updateStatus);
                }
            } else {
                Content binaryContent = contentStoreService.findContent(context, binaryPath);
                if (binaryContent != null && binaryContent.getLength() > 0) {
                    if(binaryContent.getLength() > maxFileSize) {
                        logger.warn("Skipping large binary file @ {}", binaryPath);
                    } else {
                        Map<String, String> metadata = collectMetadata(binaryPath, contentStoreService, context);
                        doUpdateContent(indexId, siteName, binaryPath, binaryContent, updateDetail, updateStatus,
                                        metadata);
                    }
                } else {
                    logger.debug("No binary file found @ {}:{}. Skipping update", siteName, binaryPath);
                }
            }
        } catch (Exception e) {
            logger.error("Error when trying to send index update for binary file {}:{}", siteName, binaryPath, e);
        }
    }

    protected abstract void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                            final Resource toResource, final UpdateDetail updateDetail,
                                            final UpdateStatus updateStatus);

    protected abstract void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                            final Content content, final UpdateDetail updateDetail,
                                            final UpdateStatus updateStatus, final Map<String, String> metadata);

    protected MultiValueMap<String, String> extractMetadata(String path, Document document) {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, StringUtils.EMPTY, metadata);

        logger.debug("Extracted metadata: {}", metadata);

        // Add extra metadata ID field
        metadata.set(metadataPathFieldName, path);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    protected void extractMetadataFromChildren(Element element, String key, MultiValueMap<String, String> metadata) {
        for (Iterator<Node> iter = element.nodeIterator(); iter.hasNext(); ) {
            Node node = iter.next();

            if (node instanceof Element) {
                StringBuilder childKey = new StringBuilder(key);

                if (childKey.length() > 0) {
                    childKey.append(".");
                }

                childKey.append(node.getName());

                if (CollectionUtils.isEmpty(excludeMetadataProperties) ||
                    !excludeMetadataProperties.contains(childKey.toString())) {
                    extractMetadataFromChildren((Element) node, childKey.toString(), metadata);
                }
            } else {
                String value = node.getText();
                if (StringUtils.isNotBlank(value) && shouldIncludeProperty(key)) {
                    logger.debug("Adding value [{}] for property [{}]", value, key);

                    metadata.add(key, StringUtils.trim(value));
                }
            }
        }
    }

    protected boolean shouldIncludeProperty(String name) {
        return (CollectionUtils.isEmpty(includePropertyPatterns) ||
                RegexUtils.matchesAny(name, includePropertyPatterns)) &&
               (CollectionUtils.isEmpty(excludePropertyPatterns) ||
                !RegexUtils.matchesAny(name, excludePropertyPatterns));
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
