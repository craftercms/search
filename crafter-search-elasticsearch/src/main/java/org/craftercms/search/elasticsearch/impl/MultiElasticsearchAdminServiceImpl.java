/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.impl;

import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.core.io.Resource;

/**
 * Extension of {@link ElasticsearchAdminServiceImpl} that handles multiple Elasticsearch clusters
 *
 * @author joseross
 * @since 3.1.5
 */
public class MultiElasticsearchAdminServiceImpl extends ElasticsearchAdminServiceImpl {

    /**
     * Elasticsearch clients used for write-related operations
     */
    protected RestHighLevelClient[] writeClients;

    public MultiElasticsearchAdminServiceImpl(final Resource authoringIndexSettings,
                                              final Resource previewIndexSettings,
                                              final RestHighLevelClient readClient,
                                              final RestHighLevelClient[] writeClients) {
        super(authoringIndexSettings, previewIndexSettings, readClient);
        this.writeClients = writeClients;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String indexName, final boolean isAuthoring) throws ElasticsearchException {
        for(RestHighLevelClient client : writeClients) {
            doCreateIndex(client, indexName, isAuthoring);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndex(final String indexName) throws ElasticsearchException {
        for(RestHighLevelClient client : writeClients) {
            doDeleteIndex(client, indexName);
        }
    }

    @Override
    public void close() throws Exception {
        for(RestHighLevelClient client : writeClients) {
            client.close();
        }
        super.close();
    }

}
