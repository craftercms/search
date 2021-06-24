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

import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractBinaryFileBatchIndexer;
import org.craftercms.core.service.Content;
import org.springframework.beans.factory.annotation.Required;

import static java.util.stream.Collectors.toMap;

/**
 * Implementation of {@link AbstractBinaryFileBatchIndexer} for Elasticsearch
 * @author joseross
 */
public class ElasticsearchBinaryFileBatchIndexer extends AbstractBinaryFileBatchIndexer {

    /**
     * Elasticsearch service
     */
    protected ElasticsearchService elasticsearchService;

    @Required
    public void setElasticsearchService(final ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doDelete(elasticsearchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String path,
                                   final Content binaryContent, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus, Map<String, String> metadata) {

        // This map transformation is required to bridge with the crafter-search API
        Map<String, Object> map = metadata.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        ElasticsearchIndexingUtils.doUpdateBinary(elasticsearchService, indexId, siteName, path, map,
            binaryContent, updateDetail, updateStatus);
    }

}
