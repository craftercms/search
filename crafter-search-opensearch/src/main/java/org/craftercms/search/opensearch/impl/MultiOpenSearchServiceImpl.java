/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.impl;

import org.craftercms.search.opensearch.DocumentParser;
import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.util.Map;

/**
 * Extension of {@link OpenSearchServiceImpl} that handles multiple OpenSearch clusters
 *
 * @author joseross
 * @since 3.1.5
 */
public class MultiOpenSearchServiceImpl extends OpenSearchServiceImpl {

    /**
     * OpenSearch clients used for write-related operations
     */
    protected final OpenSearchClient[] writeClients;

    public MultiOpenSearchServiceImpl(final OpenSearchDocumentBuilder documentBuilder,
                                      final DocumentParser documentParser, final OpenSearchClient readClient,
                                      final OpenSearchClient[] writeClients) {
        super(documentBuilder, documentParser, readClient);
        this.writeClients = writeClients;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String indexName, final String siteName, final String docId)
            throws OpenSearchException {
        for (OpenSearchClient client : writeClients) {
            doDelete(client, indexName, siteName, docId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final String indexName, final String siteName, final String docId, final Map<String, Object> doc)
            throws OpenSearchException {
        for (OpenSearchClient client : writeClients) {
            doIndex(client, indexName, siteName, docId, doc);
        }
    }

    @Override
    public void close() throws Exception {
        for (OpenSearchClient client : writeClients) {
            client._transport().close();
        }
        super.close();
    }

}
