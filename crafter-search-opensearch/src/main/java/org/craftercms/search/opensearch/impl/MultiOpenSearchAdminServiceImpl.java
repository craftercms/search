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

import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.core.io.Resource;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extension of {@link OpenSearchAdminServiceImpl} that handles multiple OpenSearch clusters
 *
 * @author joseross
 * @since 3.1.5
 */
public class MultiOpenSearchAdminServiceImpl extends OpenSearchAdminServiceImpl {

    /**
     * OpenSearch clients used for write-related operations
     */
    protected final RestHighLevelClient[] writeClients;

    public MultiOpenSearchAdminServiceImpl(Resource authoringMapping, Resource previewMapping,
                                           String authoringNamePattern, Map<String, String> localeMapping,
                                           RestHighLevelClient OpenSearchClient,
                                           Map<String, String> indexSettings,
                                           Set<String> ignoredSettings,
                                           RestHighLevelClient[] writeClients) {
        super(authoringMapping, previewMapping, authoringNamePattern, localeMapping, indexSettings, ignoredSettings,
                OpenSearchClient);
        this.writeClients = writeClients;
    }

    @Override
    public void createIndex(String aliasName) throws OpenSearchException {
        for (RestHighLevelClient client : writeClients) {
            doCreateIndex(client, aliasName, null);
        }
    }

    @Override
    public void duplicateIndex(String srcAliasName, String destAliasName) throws OpenSearchException {
        for (RestHighLevelClient client : writeClients) {
            doDuplicateIndex(client, srcAliasName, destAliasName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String aliasName, Locale locale) throws OpenSearchException {
        for (RestHighLevelClient client : writeClients) {
            doCreateIndex(client, aliasName, locale);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndexes(final String aliasName) throws OpenSearchException {
        for (RestHighLevelClient client : writeClients) {
            doDeleteIndexes(client, aliasName);
        }
    }

    @Override
    public void recreateIndex(String aliasName) throws OpenSearchException {
        for (RestHighLevelClient client : writeClients) {
            doRecreateIndex(client, aliasName);
        }
    }

    @Override
    public void waitUntilReady() {
        // wait for the read cluster to be ready
        super.waitUntilReady();

        // wait for the write clusters to be ready
        for (RestHighLevelClient client : writeClients) {
            doWaitUntilReady(client);
        }
    }

    @Override
    public void close() throws Exception {
        for (RestHighLevelClient client : writeClients) {
            client.close();
        }
        super.close();
    }

}
