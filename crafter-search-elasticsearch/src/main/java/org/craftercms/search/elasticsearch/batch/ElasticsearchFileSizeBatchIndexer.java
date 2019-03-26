/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.batch;

import java.util.Map;

import org.craftercms.search.batch.impl.AbstractFileSizeBatchIndexer;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link AbstractFileSizeBatchIndexer} that uses Elasticsearch to index
 * @author joseross
 */
public class ElasticsearchFileSizeBatchIndexer extends AbstractFileSizeBatchIndexer {

    /**
     * The instance of elastic search service
     */
    protected ElasticsearchService elasticsearchService;

    @Required
    public void setElasticsearchService(final ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateIndex(final String indexName, final String siteName, final String path,
                               final Map<String, Object> data) {
        ElasticsearchIndexingUtils.doUpdate(elasticsearchService, indexName, siteName, path, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> getCurrentData(final String indexName, final String siteName, final String path) {
        return ElasticsearchIndexingUtils.doSearchById(elasticsearchService, indexName, path);
    }

}
