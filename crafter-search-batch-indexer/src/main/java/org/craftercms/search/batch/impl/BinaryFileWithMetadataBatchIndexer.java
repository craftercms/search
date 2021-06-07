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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.Content;
import org.craftercms.search.batch.utils.CrafterSearchIndexingUtils;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.ResourceAwareSearchService;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.SearchResultUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Implementation of {@link AbstractBinaryFileWithMetadataBatchIndexer} that uses {@link SearchService}.
 * @author joseross
 */
@SuppressWarnings("rawtypes")
public class BinaryFileWithMetadataBatchIndexer extends
        AbstractBinaryFileWithMetadataBatchIndexer<MultiValueMap<String, String>> {

    /**
     * Instance of {@link SearchService}
     */
    protected SearchService searchService;

    @Required
    public void setSearchService(final SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                            final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doDelete(searchService, indexId, siteName, previousBinaryPath, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final MultiValueMap<String, String> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doUpdateContent((ResourceAwareSearchService) searchService, indexId, siteName,
            binaryPath, resource, metadata, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final MultiValueMap<String, String> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doUpdateContent(searchService, indexId, siteName, binaryPath, content, metadata,
            updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource toResource, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doUpdateContent((ResourceAwareSearchService) searchService, indexId, siteName,
            binaryPath, toResource, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus, final Map<String, String> metadata) {
        CrafterSearchIndexingUtils.doUpdateContent(searchService, indexId, siteName, binaryPath, content, updateStatus);
    }

    @SuppressWarnings("unchecked")
    protected List<String> searchBinaryPathsFromMetadataPath(String indexId, String siteName,
                                                             String metadataPath) {
        Query query = searchService.createQuery();
        query.setQuery("crafterSite:\"" + siteName + "\" AND metadataPath:\"" + metadataPath + "\"");
        query.setFieldsToReturn(localIdFieldName);

        Map<String, Object> result = searchService.search(indexId, query);
        List<Map<String, Object>> documents = SearchResultUtils.getDocuments(result);
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
    protected String searchMetadataPathFromBinaryPath(String indexId, String siteName, String binaryPath) {
        Query query = searchService.createQuery();
        query.setQuery("crafterSite:\"" + siteName + "\" AND localId:\"" + binaryPath + "\"");
        query.setFieldsToReturn(metadataPathFieldName);

        Map<String, Object> result = searchService.search(indexId, query);
        List<Map<String, Object>> documents = SearchResultUtils.getDocuments(result);

        if (CollectionUtils.isNotEmpty(documents)) {
            return (String) documents.get(0).get(metadataPathFieldName);
        } else {
            return null;
        }
    }

    protected MultiValueMap<String, String> extractMetadata(String path, Document document) {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, StringUtils.EMPTY, metadata);

        logger.debug("Extracted metadata: {}", metadata);

        // Add extra metadata ID field
        metadata.set(metadataPathFieldName, path);

        return metadata;
    }

    @SuppressWarnings("unchecked, deprecation")
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

    @Override
    @SuppressWarnings("unchecked")
    protected MultiValueMap<String, String> mergeMetadata(MultiValueMap<String, String> a, Object b) {
        if (b instanceof Map) {
            a.setAll((Map<String, String>) b);
        }
        return a;
    }

}