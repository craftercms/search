/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
import org.apache.commons.io.FilenameUtils;
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
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

import javax.activation.FileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.craftercms.search.batch.utils.IndexingUtils.isMimeTypeSupported;
import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;


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
    public static final String DEFAULT_INTERNAL_NAME_FIELD_NAME = "internalName";

    protected List<String> supportedMimeTypes;
    protected FileTypeMap mimeTypesMap;
    protected RemoteFileResolver remoteFileResolver;
    protected ItemProcessor itemProcessor;
    protected List<String> metadataPathPatterns;
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
    protected String internalNameFieldName;

    public AbstractBinaryFileWithMetadataBatchIndexer() {
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
        metadataPathFieldName = DEFAULT_METADATA_PATH_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
        internalNameFieldName = DEFAULT_INTERNAL_NAME_FIELD_NAME;
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

    public void setInternalNameFieldName(String internalNameFieldName) {
        this.internalNameFieldName = internalNameFieldName;
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

        for (String path : updatePaths) {
            if (isMetadata(path)) {
                metadataUpdatePaths.add(path);
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
                    Collections.emptySet(), context, contentStoreService, updateSet.getUpdateDetail(metadataPath), updateStatus);

            // Index the new associated binaries
            if (isNotEmpty(newBinaryPaths)) {
                Map<String, Object> metadata = extractMetadata(metadataPath, metadataDoc);

                for (String newBinaryPath : newBinaryPaths) {
                    Map<String, Object> additionalFields = collectMetadata(metadataPath, contentStoreService, context);
                    Map<String, Object> mergedMetadata = mergeMaps(metadata, additionalFields);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, newBinaryPath,
                            mergedMetadata, updateSet.getUpdateDetail(metadataPath), updateStatus);
                }
            }
        }
    }

    protected void updatePreviousBinaries(String indexId, String siteName, String metadataPath,
                                          List<String> previousBinaryPaths, Collection<String> newBinaryPaths,
                                          Set<String> binaryUpdatePaths, Context context,
                                          ContentStoreService contentStoreService,
                                          UpdateDetail updateDetail, UpdateStatus updateStatus) {
        if (!isNotEmpty(previousBinaryPaths)) {
            return;
        }
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

    protected abstract void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                                     final UpdateStatus updateStatus);

    protected void doDeletes(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             List<String> deletePaths, UpdateStatus updateStatus) {
        for (String path : deletePaths) {
            if (!isMetadata(path)) {
                continue;
            }

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
        }
    }

    protected boolean isMetadata(String path) {
        return RegexUtils.matchesAny(path, metadataPathPatterns);
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

    protected Collection<String> getBinaryFilePaths(Document document) {
        if (!isNotEmpty(referenceXPaths)) {
            return null;
        }
        Set<String> binaryPaths = new LinkedHashSet<>();
        for (String refXpath : referenceXPaths) {
            List<Node> references = document.selectNodes(refXpath);
            if (!isNotEmpty(references)) {
                continue;
            }
            for (Node reference : references) {
                String referenceValue = reference.getText();
                if (StringUtils.isNotBlank(referenceValue) &&
                    isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, referenceValue)) {
                    binaryPaths.add(referenceValue);
                }
            }
        }
        return binaryPaths;

    }

    protected void updateBinaryWithMetadata(String indexId, String siteName,
                                            ContentStoreService contentStoreService, Context context,
                                            String binaryPath, Map<String, Object> metadata,
                                            UpdateDetail updateDetail, UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.debug("Indexing remote file {}", binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                if (remoteFile.getContentLength() > maxFileSize) {
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
                                            final Resource resource, final Map<String, Object> metadata,
                                            final UpdateDetail updateDetail, final UpdateStatus updateStatus);

    protected abstract void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                            final Content content, final Map<String, Object> metadata,
                                            final UpdateDetail updateDetail, final UpdateStatus updateStatus);

    protected void updateBinary(String indexId, String siteName, ContentStoreService contentStoreService,
                                Context context, String binaryPath, UpdateDetail updateDetail,
                                UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.info("Indexing remote file {}", binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                if (remoteFile.getContentLength() > maxFileSize) {
                    logger.warn("Skipping large binary file @ {}", binaryPath);
                } else {
                    Map<String, Object> metadata = collectRemoteAssetMetadata(binaryPath);
                    doUpdateContent(indexId, siteName, binaryPath, remoteFile.toResource(), metadata, updateDetail, updateStatus);
                }
            } else {
                Content binaryContent = contentStoreService.findContent(context, binaryPath);
                if (binaryContent != null && binaryContent.getLength() > 0) {
                    if(binaryContent.getLength() > maxFileSize) {
                        logger.warn("Skipping large binary file @ {}", binaryPath);
                    } else {
                        Map<String, Object> metadata = collectMetadata(binaryPath, contentStoreService, context);
                        doUpdateContent(indexId, siteName, binaryPath, binaryContent, metadata, updateDetail,
                                        updateStatus);
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

    protected Map<String, Object> collectRemoteAssetMetadata(String binaryPath) {
        Map<String, Object> metadata = new HashMap<>();
        String internalName = FilenameUtils.getName(binaryPath);
        metadata.put(internalNameFieldName, internalName);
        return metadata;
    }

    protected Map<String, Object> extractMetadata(String path, Document document) {
        Map<String, Object> metadata = new TreeMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, EMPTY, metadata);

        logger.debug("Extracted metadata: {}", metadata);

        // Add extra metadata ID field
        metadata.put(metadataPathFieldName, path);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    protected void extractMetadataFromChildren(Element element, String path, Map<String, Object> metadata) {
        for (Iterator<Node> iter = element.nodeIterator(); iter.hasNext(); ) {
            Node node = iter.next();

            // Skip namespace nodes to avoid issues during XML merging
            if (node instanceof Namespace) {
                continue;
            }

            StringBuilder childKey = new StringBuilder(path);

            if (childKey.length() > 0) {
                childKey.append(".");
            }

            childKey.append(node.getName());

            if (node instanceof Element && isNotEmpty(((Element) node).elements())) {
                if (shouldIncludeProperty(childKey.toString()) &&
                        (CollectionUtils.isEmpty(excludeMetadataProperties) ||
                        !excludeMetadataProperties.contains(childKey.toString()))) {
                    var childMetadata = new TreeMap<String, Object>();
                    metadata.put(node.getName(), childMetadata);
                    extractMetadataFromChildren((Element) node, childKey.toString(), childMetadata);
                }
            } else {
                String value = trim(node.getText());
                if (StringUtils.isNotBlank(value) && shouldIncludeProperty(childKey.toString())) {
                    logger.debug("Adding value [{}] for property [{}]", value, childKey.toString());

                    metadata.compute(node.getName(), (k, existingValue) -> {
                        if (existingValue == null) {
                            return value;
                        } else {
                            if (existingValue instanceof List) {
                                ((List<Object>) existingValue).add(value);
                                return existingValue;
                            } else {
                                var list = new LinkedList<>();
                                list.add(existingValue);
                                list.add(value);
                                return list;
                            }
                        }
                    });
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
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

    }

}
