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

package org.craftercms.search.elasticsearch.batch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractBinaryFileWithMetadataBatchIndexer;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * Implementation of {@link AbstractBinaryFileWithMetadataBatchIndexer} for Elasticsearch
 * @author joseross
 */
public class ElasticsearchBinaryFileWithMetadataBatchIndexer extends
        AbstractBinaryFileWithMetadataBatchIndexer<Map<String, Object>> {

    /**
     * Elasticsearch service
     */
    protected ElasticsearchService elasticsearchService;

    @Required
    public void setElasticsearchService(final ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String previousBinaryPath,
                            final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doDelete(elasticsearchService, indexId, siteName, previousBinaryPath, updateStatus);
    }

    @Override
    protected List<String> searchBinaryPathsFromMetadataPath(final String indexId, final String siteName,
                                                             final String metadataPath) {
        try {
            return elasticsearchService.searchField(indexId, localIdFieldName,
                    matchQuery(metadataPathFieldName, metadataPath));
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId, "Error executing search for " + metadataPath, e);
        }
    }

    @Override
    protected String searchMetadataPathFromBinaryPath(final String indexId, final String siteName,
                                                      final String binaryPath) {
        try {
            List<String> paths = elasticsearchService.searchField(indexId, metadataPathFieldName,
                    matchQuery(localIdFieldName, binaryPath));

            if(isNotEmpty(paths)) {
                return paths.get(0);
            } else {
                return null;
            }
        } catch (ElasticsearchException e) {
           throw new SearchException(indexId, "Error executing search for " + binaryPath, e);
        }
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doUpdateBinary(elasticsearchService, indexId, siteName, binaryPath, metadata,
                resource, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final Map<String, Object> metadata,
                                   final UpdateDetail updateDetail, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doUpdateBinary(elasticsearchService, indexId, siteName, binaryPath, metadata,
                content, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Resource resource, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus) {
        doUpdateContent(indexId, siteName, binaryPath, resource, null, updateDetail, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String binaryPath,
                                   final Content content, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus, final Map<String, String> metadata) {

        // This map transformation is required to bridge with the crafter-search API
        Map<String, Object> map = metadata.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        doUpdateContent(indexId, siteName, binaryPath, content, map, updateDetail, updateStatus);
    }

    @Override
    protected Map<String, Object> extractMetadata(String path, Document document) {
        Map<String, Object> metadata = new TreeMap<>();
        Element rootElem = document.getRootElement();

        extractMetadataFromChildren(rootElem, EMPTY, metadata);

        logger.debug("Extracted metadata: {}", metadata);

        // Add extra metadata ID field
        metadata.put(metadataPathFieldName, path);

        return metadata;
    }

    @Override
    @SuppressWarnings("unchecked, deprecation")
    protected void extractMetadataFromChildren(Element element, String key, Map<String, Object> metadata) {
        for (Iterator<Node> iter = element.nodeIterator(); iter.hasNext(); ) {
            Node node = iter.next();

            StringBuilder childKey = new StringBuilder(key);

            if (childKey.length() > 0) {
                childKey.append(".");
            }

            childKey.append(node.getName());

            if (node instanceof Element && isNotEmpty(((Element) node).elements())) {
                if (shouldIncludeProperty(childKey.toString()) &&
                        (CollectionUtils.isEmpty(excludeMetadataProperties) ||
                                !excludeMetadataProperties.contains(childKey.toString()))) {
                    Map<String, Object> childMetadata = new TreeMap<>();
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
                                List<Object> list = new LinkedList<>();
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

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> mergeMetadata(Map<String, Object> a, Object b) {
        if (b instanceof Map) {
            return mergeMaps(a, (Map<String, Object>) b);
        } else {
            return a;
        }
    }

}
