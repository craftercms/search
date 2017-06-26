package org.craftercms.search.batch.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.SearchResultUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.craftercms.search.batch.utils.IndexingUtils.*;

/**
 * Created by alfonso on 6/22/17.
 */
public class BinaryFileWithMetadataBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(BinaryFileWithMetadataBatchIndexer.class);

    public static final String DEFAULT_METADATA_PATH_FIELD_NAME = "metadataPath";

    protected ItemProcessor itemProcessor;
    protected List<String> metadataPathPatterns;
    protected List<String> binaryPathPatterns;
    protected List<String> referenceXPaths;
    protected List<String> excludeMetadataProperties;
    protected String metadataPathFieldName;

    public BinaryFileWithMetadataBatchIndexer() {
        metadataPathFieldName = DEFAULT_METADATA_PATH_FIELD_NAME;
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

    public void setReferenceXPaths(List<String> referenceXPaths) {
        this.referenceXPaths = referenceXPaths;
    }

    public void setExcludeMetadataProperties(List<String> excludeMetadataProperties) {
        this.excludeMetadataProperties = excludeMetadataProperties;
    }

    public void setMetadataPathFieldName(String metadataPathFieldName) {
        this.metadataPathFieldName = metadataPathFieldName;
    }

    @Override
    public void updateIndex(SearchService searchService, String indexId, String siteName, ContentStoreService contentStoreService,
                            Context context, UpdateSet updateSet, UpdateStatus updateStatus) throws BatchIndexingException {
        doUpdates(indexId, siteName, searchService, contentStoreService, context, updateSet.getUpdatePaths(), updateStatus);
        doDeletes(indexId, siteName, searchService, contentStoreService, context, updateSet.getDeletePaths(), updateStatus);
    }

    protected void doUpdates(String indexId, String siteName, SearchService searchService, ContentStoreService contentStoreService,
                             Context context, List<String> updatePaths, UpdateStatus updateStatus) {
        Set<String> metadataPaths = new LinkedHashSet<>();
        Set<String> binaryPaths = new LinkedHashSet<>();

        for (String path : updatePaths) {
            if (isMetadata(path)) {
                metadataPaths.add(path);
            } else if (isBinary(path)) {
                binaryPaths.add(path);
            }
        }

        // Look for the associated binary in each of the metadata files. If one is found, index the binary with the metadata,
        // and remove the binary from the list to prevent it from being indexed again
        for (String metadataPath : metadataPaths) {
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);
            if (metadataDoc != null) {
                String binaryPath = getBinaryPathFromMetadata(metadataDoc);
                if (StringUtils.isNotEmpty(binaryPath)) {
                    binaryPaths.remove(binaryPath);

                    updateBinaryWithMetadata(searchService, indexId, siteName, contentStoreService, context,
                                             binaryPath, metadataPath, metadataDoc, updateStatus);
                }
            }
        }

        // Search the index for the metadata file associated to the binary. If one is found, index the binary with the
        // metadata, if not, index the binary by itself
        for (String binaryPath : binaryPaths) {
            String metadataPath = searchMetadataPathFromBinaryPath(searchService, indexId, siteName, binaryPath);
            if (StringUtils.isNotEmpty(metadataPath)) {
                Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);
                if (metadataDoc != null) {
                    updateBinaryWithMetadata(searchService, indexId, siteName, contentStoreService, context,
                                             binaryPath, metadataPath, metadataDoc, updateStatus);
                }
            } else {
                updateBinary(searchService, indexId, siteName, contentStoreService, context, binaryPath, updateStatus);
            }
        }
    }

    protected void doDeletes(String indexId, String siteName, SearchService searchService, ContentStoreService contentStoreService,
                             Context context, List<String> deletePaths, UpdateStatus updateStatus) {
        for (String path : deletePaths) {
            if (isMetadata(path)) {
                Document metadataDoc = loadMetadata(contentStoreService, context, siteName, path);
                if (metadataDoc != null) {
                    String binaryPath = getBinaryPathFromMetadata(metadataDoc);
                    if (StringUtils.isNotEmpty(binaryPath)) {
                        doDelete(searchService, indexId, siteName, binaryPath, updateStatus);
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
        return RegexUtils.matchesAny(path, binaryPathPatterns);
    }

    @SuppressWarnings("unchecked")
    protected String searchMetadataPathFromBinaryPath(SearchService searchService, String indexId, String siteName, String binaryPath) {
        Query query = searchService.createQuery();
        query.setQuery("id:\"" + getSiteBasedPath(siteName, binaryPath) + "\"");
        query.setFieldsToReturn(metadataPathFieldName);

        Map<String, Object> result = searchService.search(indexId, query);
        List<Map<String, Object>> documents = SearchResultUtils.getDocuments(result);

        if (CollectionUtils.isNotEmpty(documents)) {
            return (String)documents.get(0).get(metadataPathFieldName);
        } else {
            return null;
        }
    }

    protected Document loadMetadata(ContentStoreService contentStoreService, Context context, String siteName, String metadataPath) {
        try {
            Document metadataDoc = contentStoreService.getItem(context, null, metadataPath, itemProcessor).getDescriptorDom();
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

    protected String getBinaryPathFromMetadata(Document metadataDoc) {
        if (CollectionUtils.isNotEmpty(referenceXPaths)) {
            for (String refXpath : referenceXPaths) {
                Node reference = metadataDoc.selectSingleNode(refXpath);
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

    protected void updateBinaryWithMetadata(SearchService searchService, String indexId, String siteName,
                                            ContentStoreService contentStoreService, Context context,
                                            String binaryPath, String metadataPath, Document metadataDoc,
                                            UpdateStatus updateStatus) {
        try {
            MultiValueMap<String, String> metadata = extractMetadata(metadataDoc);

            if (logger.isDebugEnabled()) {
                logger.debug("Extracted metadata: " + metadata);
            }

            // Add extra metadata ID field
            metadata.set(metadataPathFieldName, metadataPath);

            Content binaryContent = contentStoreService.findContent(context, binaryPath);
            if (binaryContent == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Binary file " + getSiteBasedPath(siteName, binaryPath) + " doesn't exist. Empty content will " +
                                 "be used for the update");
                }

                binaryContent = new EmptyContent();
            }

            doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, metadata, updateStatus);
        } catch (Exception e) {
            logger.error("Error when trying to send index update with metadata for binary file " + getSiteBasedPath(siteName, binaryPath));
        }
    }

    protected void updateBinary(SearchService searchService, String indexId, String siteName, ContentStoreService contentStoreService,
                                Context context, String binaryPath, UpdateStatus updateStatus) {
        try {
            Content binaryContent = contentStoreService.getContent(context, binaryPath);

            doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, updateStatus);
        } catch (Exception e) {
            logger.error("Error when trying to send index update for binary file " + getSiteBasedPath(siteName, binaryPath));
        }
    }

    protected MultiValueMap<String, String> extractMetadata(Document document) {
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
