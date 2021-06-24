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

import java.util.Map;

import org.craftercms.search.batch.utils.IndexingUtils;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.Content;
import org.craftercms.search.exception.SearchException;
import org.springframework.core.io.Resource;

import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;

/**
 * Utility class to perform Elasticsearch operations
 * @author joseross
 */
public abstract class ElasticsearchIndexingUtils extends IndexingUtils {

    public static Map<String, Object> doSearchById(final ElasticsearchService elasticsearch, final String indexName,
                                                   final String path) {
        return elasticsearch.searchId(indexName, path);
    }

    public static void doDelete(final ElasticsearchService elasticsearch, final String indexName,
                                final String siteName, final String path, final UpdateStatus updateStatus) {
        try {
            elasticsearch.delete(indexName, siteName, path);
            updateStatus.addSuccessfulDelete(path);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexName, "Error deleting document " + path, e);
        }
    }

    public static void doUpdate(final ElasticsearchService elasticsearch, final String indexName,
                                final String siteName, final String path, final Map<String, Object> doc) {
        elasticsearch.index(indexName, siteName, path, doc);
    }

    public static void doUpdate(final ElasticsearchService elasticsearch, final String indexName,
                                final String siteName, final String path, final String xml,
                                final UpdateDetail updateDetail, final UpdateStatus updateStatus,
                                Map<String, Object> metadata) {
        try {
            elasticsearch.index(indexName, siteName, path, xml,
                mergeMaps(metadata, getAdditionalFields(updateDetail)));
            updateStatus.addSuccessfulUpdate(path);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexName, "Error indexing document " + path, e);
        }
    }

    public static void doUpdateBinary(final ElasticsearchService elasticsearch, final String indexName,
                                      final String siteName, final String path,
                                      final Map<String, Object> additionalFields,
                                      final Content content, final UpdateDetail updateDetail,
                                      final UpdateStatus updateStatus) {
        try {
            elasticsearch.indexBinary(indexName, siteName, path,
                mergeMaps(additionalFields,  getAdditionalFields(updateDetail)), content);
            updateStatus.addSuccessfulUpdate(path);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexName, "Error indexing binary document " + path, e);
        }

    }

    public static void doUpdateBinary(final ElasticsearchService elasticsearch, final String indexName,
                                      final String siteName, final String path,
                                      final Map<String, Object> additionalFields,
                                      final Resource resource, final UpdateDetail updateDetail,
                                      final UpdateStatus updateStatus) {
        try {
            elasticsearch.indexBinary(indexName, siteName, path,
                mergeMaps(additionalFields,  getAdditionalFields(updateDetail)), resource);
            updateStatus.addSuccessfulUpdate(path);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexName, "Error indexing binary document " + path, e);
        }

    }

}
