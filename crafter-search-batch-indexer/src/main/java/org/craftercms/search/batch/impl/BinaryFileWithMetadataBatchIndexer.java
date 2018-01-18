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
import java.util.ArrayList;
import java.util.Collections;
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
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.QueryFactory;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.SearchResultUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.craftercms.search.batch.utils.IndexingUtils.*;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that tries to match binary files with metadata files. Right now, a metadata file
 * can reference several binary files. Also, this indexer supports the concept of "child" binaries, where the parent is the metadata
 * file and the binary file only exists in the index as long as the metadata file exists and it references the binary.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(BinaryFileWithMetadataBatchIndexer.class);

    public static final String DEFAULT_METADATA_PATH_FIELD_NAME = "metadataPath";
    public static final String DEFAULT_LOCAL_ID_FIELD_NAME = "localId";

    protected ItemProcessor itemProcessor;
    protected List<String> metadataPathPatterns;
    protected List<String> binaryPathPatterns;
    protected List<String> childBinaryPathPatterns;
    protected List<String> referenceXPaths;
    protected List<String> includePropertyPatterns;
    protected List<String> excludePropertyPatterns;
    @Deprecated
    protected List<String> excludeMetadataProperties;
    protected String metadataPathFieldName;
    protected String localIdFieldName;
    protected SearchService searchService;
    protected QueryFactory<Query> queryFactory;

    public BinaryFileWithMetadataBatchIndexer() {
        metadataPathFieldName = DEFAULT_METADATA_PATH_FIELD_NAME;
        localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;
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

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;

        if (searchService instanceof QueryFactory) {
            this.queryFactory = (QueryFactory<Query>)searchService;
        }
    }

    public void setQueryFactory(QueryFactory<Query> queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public void updateIndex(String indexId, String siteName, ContentStoreService contentStoreService, Context context, List<String> paths,
                            boolean delete, IndexingStatus status) throws BatchIndexingException {
        if (delete) {
            doDeletes(indexId, siteName, contentStoreService, context, paths, status);
        } else {
            doUpdates(indexId, siteName, contentStoreService, context, paths, status);
        }
    }

    protected void doUpdates(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             List<String> updatePaths, IndexingStatus status) {
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
            List<String> newBinaryPaths = Collections.emptyList();
            List<String> previousBinaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, metadataPath);
            Document metadataDoc = loadMetadata(contentStoreService, context, siteName, metadataPath);

            if (metadataDoc != null) {
                newBinaryPaths = getBinaryFilePaths(metadataDoc);
            }

            // If there are previous binaries that are not associated to the metadata anymore, reindex them without metadata or delete
            // them if they're child binaries.
            if (CollectionUtils.isNotEmpty(previousBinaryPaths)) {
                for (String previousBinaryPath : previousBinaryPaths) {
                    if (CollectionUtils.isNotEmpty(newBinaryPaths) && !newBinaryPaths.contains(previousBinaryPath)) {
                        binaryUpdatePaths.remove(previousBinaryPath);

                        if (isChildBinary(previousBinaryPath)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Reference of child binary " + previousBinaryPath + " removed from parent " + metadataPath +
                                             ". Deleting binary from index...");
                            }

                            doDelete(searchService, indexId, siteName, previousBinaryPath, status);
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Reference of binary " + previousBinaryPath + " removed from " + metadataPath +
                                             ". Reindexing without metadata...");
                            }

                            updateBinary(indexId, siteName, contentStoreService, context, previousBinaryPath, status);
                        }
                    }
                }
            }

            // Index the new associated binaries
            if (CollectionUtils.isNotEmpty(newBinaryPaths)) {
                MultiValueMap<String, String> metadata = extractMetadata(metadataPath, metadataDoc);

                for (String newBinaryPath : newBinaryPaths) {
                    binaryUpdatePaths.remove(newBinaryPath);

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, newBinaryPath, metadata, status);
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

                    updateBinaryWithMetadata(indexId, siteName, contentStoreService, context, binaryPath, metadata, status);
                }
            } else {
                // If not, index by itself
                updateBinary(indexId, siteName, contentStoreService, context, binaryPath, status);
            }
        }
    }

    protected void doDeletes(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                             List<String> deletePaths, IndexingStatus status) {
        for (String path : deletePaths) {
            if (isMetadata(path)) {
                List<String> binaryPaths = searchBinaryPathsFromMetadataPath(indexId, siteName, path);
                for (String binaryPath : binaryPaths) {
                    if (isChildBinary(binaryPath)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Parent of binary " + binaryPath + " deleted. Deleting child binary too");
                        }

                        // If the binary is a child binary, when the metadata file is deleted, then delete it
                        doDelete(searchService, indexId, siteName, binaryPath, status);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Metadata with reference of binary " + binaryPath + " deleted. Reindexing without metadata...");
                        }

                        // Else, update binary without metadata
                        updateBinary(indexId, siteName, contentStoreService, context, binaryPath, status);
                    }
                }
            } else if (isBinary(path)) {
                doDelete(searchService, indexId, siteName, path, status);
            }
        }
    }

    protected boolean isMetadata(String path) {
        return RegexUtils.matchesAny(path, metadataPathPatterns);
    }

    protected boolean isBinary(String path) {
        return RegexUtils.matchesAny(path, binaryPathPatterns);
    }

    protected boolean isChildBinary(String path) {
        return RegexUtils.matchesAny(path, childBinaryPathPatterns);
    }

    @SuppressWarnings("unchecked")
    protected List<String> searchBinaryPathsFromMetadataPath(String indexId, String siteName, String metadataPath) {
        if (queryFactory == null) {
            throw new IllegalStateException("No QueryFactory provided");
        }

        Query query = queryFactory.createQuery();
        query.setQuery("crafterSite:\"" + siteName + "\" AND metadataPath:\"" + metadataPath + "\"");
        query.setFieldsToReturn(localIdFieldName);

        Map<String, Object> result = searchService.search(indexId, query);
        List<Map<String, Object>> documents = SearchResultUtils.getDocuments(result);
        List<String> binaryPaths = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(documents)) {
            for (Map<String, Object> document : documents) {
                String binaryPath = (String)document.get(localIdFieldName);
                if (StringUtils.isNotEmpty(binaryPath)) {
                    binaryPaths.add(binaryPath);
                }
            }
        }

        return binaryPaths;
    }

    @SuppressWarnings("unchecked")
    protected String searchMetadataPathFromBinaryPath(String indexId, String siteName, String binaryPath) {
        if (queryFactory == null) {
            throw new IllegalStateException("No QueryFactory provided");
        }

        Query query = queryFactory.createQuery();
        query.setQuery("crafterSite:\"" + siteName + "\" AND localId:\"" + binaryPath + "\"");
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

    @SuppressWarnings("unchecked")
    protected List<String> getBinaryFilePaths(Document document) {
        if (CollectionUtils.isNotEmpty(referenceXPaths)) {
            for (String refXpath : referenceXPaths) {
                List<Node> references = document.selectNodes(refXpath);
                if (CollectionUtils.isNotEmpty(references)) {
                    List<String> binaryPaths = new ArrayList<>();

                    for (Node reference : references) {
                        String referenceValue = reference.getText();
                        if (StringUtils.isNotBlank(referenceValue)) {
                            binaryPaths.add(referenceValue);
                        }
                    }

                    return binaryPaths;
                }
            }
        }

        return null;
    }

    protected void updateBinaryWithMetadata(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                                            String binaryPath, MultiValueMap<String, String> metadata, IndexingStatus status) {
        try {
            Content binaryContent = contentStoreService.findContent(context, binaryPath);
            if (binaryContent == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No binary file found @ " + getSiteBasedPath(siteName, binaryPath) + ". Empty content will be used for " +
                                 "the update");
                }

                binaryContent = new EmptyContent();
            }

            doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, metadata, status);
        } catch (Exception e) {
            logger.error("Error when trying to send index update with metadata for binary file " + getSiteBasedPath(siteName, binaryPath), e);
        }
    }

    protected void updateBinary(String indexId, String siteName, ContentStoreService contentStoreService,
                                Context context, String binaryPath, IndexingStatus status) {
        try {
            Content binaryContent = contentStoreService.findContent(context, binaryPath);
            if (binaryContent != null) {
                doUpdateContent(searchService, indexId, siteName, binaryPath, binaryContent, status);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No binary file found @ " + getSiteBasedPath(siteName, binaryPath) + ". Skipping update");
                }
            }
        } catch (Exception e) {
            logger.error("Error when trying to send index update for binary file " + getSiteBasedPath(siteName, binaryPath), e);
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
        for (Iterator<Node> iter = element.nodeIterator(); iter.hasNext();) {
            Node node = iter.next();

            if (node instanceof Element) {
                StringBuilder childKey = new StringBuilder(key);

                if (childKey.length() > 0) {
                    childKey.append(".");
                }

                childKey.append(node.getName());

                if (CollectionUtils.isEmpty(excludeMetadataProperties) || !excludeMetadataProperties.contains(childKey.toString())) {
                    extractMetadataFromChildren((Element)node, childKey.toString(), metadata);
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
        return (CollectionUtils.isEmpty(includePropertyPatterns) || RegexUtils.matchesAny(name, includePropertyPatterns)) &&
               (CollectionUtils.isEmpty(excludePropertyPatterns) || !RegexUtils.matchesAny(name, excludePropertyPatterns));
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
