package org.craftercms.search.batch.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.commons.file.stores.RemoteFile;
import org.craftercms.commons.file.stores.RemoteFileResolver;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.craftercms.search.service.ResourceAwareSearchService;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.activation.FileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.craftercms.search.batch.utils.IndexingUtils.*;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that tries to match binary files with metadata files. A metadata
 * file can reference several binary files, and a binary file can be referenced by several metadata files. Also, this
 * indexer supports the concept of "child" binaries, where the parent is the metadata file and the binary file only
 * exists in the index as long as the metadata file exists and it references the binary.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(BinaryFileWithMetadataBatchIndexer.class);

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

    public BinaryFileWithMetadataBatchIndexer() {
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

    @Override
    public void updateIndex(SearchService searchService, String indexId, String siteName,
                            ContentStoreService contentStoreService, Context context, UpdateSet updateSet,
                            UpdateStatus updateStatus) throws BatchIndexingException {
        doUpdates(indexId, siteName, searchService, contentStoreService, context, updateSet.getUpdatePaths(),
                  updateStatus);
        doDeletes(indexId, siteName, searchService, contentStoreService, context, updateSet.getDeletePaths(),
                  updateStatus);
    }

    protected void doUpdates(String indexId, String siteName, SearchService searchService,
                             ContentStoreService contentStoreService, Context context, List<String> updatePaths,
                             UpdateStatus updateStatus) {
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
            List<String> previousBinaryPaths = searchBinaryPathsFromMetadataPath(searchService, indexId, siteName,
                                                                                 metadataPath);
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);

            if (metadataDoc != null) {
                newBinaryPaths = getBinaryFilePaths(metadataDoc);
            }

            // If there are previous binaries that are not associated to the metadata anymore, reindex them without
            // metadata or delete them if they're child binaries.
            if (CollectionUtils.isNotEmpty(previousBinaryPaths)) {
                for (String previousBinaryPath : previousBinaryPaths) {
                    if (CollectionUtils.isEmpty(newBinaryPaths) || !newBinaryPaths.contains(previousBinaryPath)) {
                        binaryUpdatePaths.remove(previousBinaryPath);

                        if (isChildBinary(previousBinaryPath)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Reference of child binary " + previousBinaryPath + " removed from " +
                                             "parent " + metadataPath + ". Deleting binary from index...");
                            }

                            doDelete(searchService, indexId, siteName, previousBinaryPath, updateStatus);
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Reference of binary " + previousBinaryPath + " removed from " +
                                             metadataPath + ". Reindexing without metadata...");
                            }

                            updateBinary(searchService, indexId, siteName, contentStoreService, context,
                                         previousBinaryPath, updateStatus);
                        }
                    }
                }
            }

            // Index the new associated binaries
            if (CollectionUtils.isNotEmpty(newBinaryPaths)) {
                MultiValueMap<String, String> metadata = extractMetadata(metadataPath, metadataDoc);

                for (String newBinaryPath : newBinaryPaths) {
                    binaryUpdatePaths.remove(newBinaryPath);

                    updateBinaryWithMetadata(searchService, indexId, siteName, contentStoreService, context,
                                             newBinaryPath, metadata, updateStatus);
                }
            }
        }

        for (String binaryPath : binaryUpdatePaths) {
            String metadataPath = searchMetadataPathFromBinaryPath(searchService, indexId, siteName, binaryPath);
            if (StringUtils.isNotEmpty(metadataPath)) {
                // If the binary file has an associated metadata, index the file with the metadata
                Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);
                if (metadataDoc != null) {
                    MultiValueMap<String, String> metadata = extractMetadata(metadataPath, metadataDoc);

                    updateBinaryWithMetadata(searchService, indexId, siteName, contentStoreService, context, binaryPath,
                                             metadata, updateStatus);
                }
            } else {
                // If not, index by itself
                updateBinary(searchService, indexId, siteName, contentStoreService, context, binaryPath, updateStatus);
            }
        }
    }

    protected void doDeletes(String indexId, String siteName, SearchService searchService,
                             ContentStoreService contentStoreService, Context context, List<String> deletePaths,
                             UpdateStatus updateStatus) {
        for (String path : deletePaths) {
            if (isMetadata(path)) {
                List<String> binaryPaths = searchBinaryPathsFromMetadataPath(searchService, indexId, siteName, path);
                for (String binaryPath : binaryPaths) {
                    if (isChildBinary(binaryPath)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Parent of binary " + binaryPath + " deleted. Deleting child binary too");
                        }

                        // If the binary is a child binary, when the metadata file is deleted, then delete it
                        doDelete(searchService, indexId, siteName, binaryPath, updateStatus);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Metadata with reference of binary " + binaryPath + " deleted. Reindexing " +
                                         "without metadata...");
                        }

                        // Else, update binary without metadata
                        updateBinary(searchService, indexId, siteName, contentStoreService, context, binaryPath,
                                     updateStatus);
                    }
                }
            } else if (isBinary(path)) {
                doDelete(searchService, indexId, siteName, path, updateStatus);
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

    @SuppressWarnings("unchecked")
    protected List<String> searchBinaryPathsFromMetadataPath(SearchService searchService, String indexId,
                                                             String siteName, String metadataPath) {
        SearchRequest request = searchService.createRequest();
        request.setMainQuery("crafterSite:\"" + siteName + "\" AND metadataPath:\"" + metadataPath + "\"");
        request.setFields(localIdFieldName);
        request.setIndexId(indexId);

        SearchResponse response = searchService.search(request);
        List<Map<String, Object>> documents = response.getItems();
        List<String> binaryPaths = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(documents)) {
            for (Map<String, Object> document : documents) {
                String binaryPath = (String) document.get(localIdFieldName);
                if (StringUtils.isNotEmpty(binaryPath)) {
                    binaryPaths.add(binaryPath);
                }
            }
        }

        return binaryPaths;
    }

    @SuppressWarnings("unchecked")
    protected String searchMetadataPathFromBinaryPath(SearchService searchService, String indexId,
                                                      String siteName, String binaryPath) {
        SearchRequest request = searchService.createRequest();
        request.setMainQuery("crafterSite:\"" + siteName + "\" AND localId:\"" + binaryPath + "\"");
        request.setFields(metadataPathFieldName);
        request.setIndexId(indexId);

        SearchResponse response = searchService.search(request);
        List<Map<String, Object>> documents = response.getItems();

        if (CollectionUtils.isNotEmpty(documents)) {
            return (String) documents.get(0).get(metadataPathFieldName);
        } else {
            return null;
        }
    }

    protected Document loadMetadata(ContentStoreService contentStoreService, Context context, String siteName,
                                    String metadataPath) {
        try {
            Document metadataDoc = contentStoreService.getItem(context, null, metadataPath,
                                                               itemProcessor).getDescriptorDom();
            if (metadataDoc != null) {
                return metadataDoc;
            } else {
                logger.error("File " + getSiteBasedPath(siteName, metadataPath) + " is not a metadata XML descriptor");
            }
        } catch (Exception e) {
            logger.error("Error retrieving metadata file @ " + getSiteBasedPath(siteName, metadataPath), e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected Collection<String> getBinaryFilePaths(Document document) {
        if (CollectionUtils.isNotEmpty(referenceXPaths)) {
            for (String refXpath : referenceXPaths) {
                List<Node> references = document.selectNodes(refXpath);
                if (CollectionUtils.isNotEmpty(references)) {
                    Set<String> binaryPaths = new LinkedHashSet<>();

                    for (Node reference : references) {
                        String referenceValue = reference.getText();
                        if (StringUtils.isNotBlank(referenceValue) &&
                            isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, referenceValue)) {
                            binaryPaths.add(referenceValue);
                        }
                    }

                    return binaryPaths;
                }
            }
        }

        return null;
    }

    protected void updateBinaryWithMetadata(SearchService searchService, String indexId, String siteName,
                                            ContentStoreService contentStoreService, Context context,
                                            String binaryPath, MultiValueMap<String, String> metadata,
                                            UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.info("Indexing remote file " + binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                doUpdateContent((ResourceAwareSearchService) searchService, indexId, siteName, binaryPath,
                                remoteFile.toResource(), metadata, updateStatus);
            } else {
                Content binaryContent = contentStoreService.findContent(context, binaryPath);
                if (binaryContent == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No binary file found @ " + getSiteBasedPath(siteName, binaryPath) +
                                     ". Empty content will be used for the update");
                    }

                    binaryContent = new EmptyContent();
                }

                doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, metadata, updateStatus);
            }
        } catch (Exception e) {
            logger.error("Error when trying to send index update with metadata for binary file " +
                         getSiteBasedPath(siteName, binaryPath), e);
        }
    }

    protected void updateBinary(SearchService searchService, String indexId, String siteName,
                                ContentStoreService contentStoreService, Context context, String binaryPath,
                                UpdateStatus updateStatus) {
        try {
            // Check if the binary file is stored remotely
            if (remoteFileResolver != null && isRemoteBinary(binaryPath)) {
                logger.info("Indexing remote file " + binaryPath);

                RemoteFile remoteFile = remoteFileResolver.resolve(binaryPath);

                doUpdateContent((ResourceAwareSearchService) searchService, indexId, siteName, binaryPath,
                                remoteFile.toResource(), updateStatus);
            } else {
                Content binaryContent = contentStoreService.findContent(context, binaryPath);
                if (binaryContent != null) {
                    doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, updateStatus);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No binary file found @ " + getSiteBasedPath(siteName, binaryPath) +
                                     ". Skipping update");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error when trying to send index update for binary file " +
                         getSiteBasedPath(siteName, binaryPath), e);
        }
    }

    protected MultiValueMap<String, String> extractMetadata(String path, Document document) {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, StringUtils.EMPTY, metadata);

        if (logger.isDebugEnabled()) {
            logger.debug("Extracted metadata: " + metadata);
        }

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
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Adding value [%s] for property [%s]", value, key));
                    }

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
