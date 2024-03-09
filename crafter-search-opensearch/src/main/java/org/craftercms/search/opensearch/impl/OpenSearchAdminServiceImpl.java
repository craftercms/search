/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.impl;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.craftercms.commons.locale.LocaleUtils;
import org.craftercms.search.commons.exception.IndexNotFoundException;
import org.craftercms.search.opensearch.OpenSearchAdminService;
import org.craftercms.search.opensearch.exception.OpenSearchException;
import org.opensearch.action.admin.indices.alias.Alias;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.open.OpenIndexRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.client.GetAliasesResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CloseIndexRequest;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.ReindexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * Default implementation of {@link OpenSearchAdminService}
 *
 * @author joseross
 * @since 3.1.0
 */
public class OpenSearchAdminServiceImpl implements OpenSearchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchAdminServiceImpl.class);

    public static final String DEFAULT_INDEX_NAME_SUFFIX = "_v1";

    public static final String ES_STANDARD_ANALYZER = "standard";

    public static final String ES_KEY_DEFAULT_ANALYZER = "analysis.analyzer.default.type";

    /**
     * The suffix to add to all index names during creation
     */
    protected String indexNameSuffix = DEFAULT_INDEX_NAME_SUFFIX;

    /**
     * Index mapping file for authoring indices
     */
    protected final Resource authoringMapping;

    /**
     * Index mapping file for preview indices
     */
    protected final Resource previewMapping;

    /**
     * Regex used to determine if an index is for authoring
     */
    protected final String authoringNamePattern;

    /**
     * The map of locale codes to OpenSearch languages
     */
    protected final Map<String, String> localeMapping;

    /**
     * The OpenSearch client
     */
    protected final RestHighLevelClient openSearchClient;

    /**
     * The default settings used when creating indices
     */
    protected final Map<String, String> defaultSettings;

    /**
     * The settings to ignore on duplicate index
     */
    protected final Set<String> ignoredSettings;

    @ConstructorProperties({"authoringMapping", "previewMapping", "authoringNamePattern", "localeMapping",
            "defaultSettings", "ignoredSettings", "openSearchClient"})
    public OpenSearchAdminServiceImpl(final Resource authoringMapping, final Resource previewMapping,
                                      final String authoringNamePattern, final Map<String, String> localeMapping,
                                      final Map<String, String> defaultSettings, final Set<String> ignoredSettings,
                                      final RestHighLevelClient openSearchClient) {
        this.authoringMapping = authoringMapping;
        this.previewMapping = previewMapping;
        this.authoringNamePattern = authoringNamePattern;
        this.localeMapping = localeMapping;
        this.defaultSettings = defaultSettings;
        this.ignoredSettings = ignoredSettings;
        this.openSearchClient = openSearchClient;
    }

    public void setIndexNameSuffix(final String indexNameSuffix) {
        this.indexNameSuffix = indexNameSuffix;
    }

    @Override
    public boolean indexExists(String indexName) {
        return doIndexExist(openSearchClient, indexName);
    }

    protected boolean doIndexExist(RestHighLevelClient client, String indexName) {
        logger.debug("Checking if index {} exits", indexName);
        try {
            return openSearchClient.indices().exists(
                    new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new OpenSearchException(indexName, "Error consulting index", e);
        }
    }

    @Override
    public void createIndex(String aliasName) throws OpenSearchException {
        createIndex(aliasName, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String aliasName, Locale locale) throws OpenSearchException {
        doCreateIndex(openSearchClient, aliasName, indexNameSuffix, locale, true, defaultSettings);
    }

    /**
     * Performs the index creation using the given OpenSearch client
     */
    protected void doCreateIndex(RestHighLevelClient client, String aliasName, Locale locale) {
        doCreateIndex(client, aliasName, indexNameSuffix, locale, true, defaultSettings);
    }

    /**
     * Performs the index creation using the given OpenSearch client
     */
    protected void doCreateIndex(RestHighLevelClient client, String aliasName, String indexSuffix, Locale locale,
                                 boolean createAlias, Map<String, String> settings) {
        Resource mapping = aliasName.matches(authoringNamePattern) ? authoringMapping : previewMapping;
        String defaultAnalyzer = ES_STANDARD_ANALYZER;
        if (locale != null) {
            String localeValue = LocaleUtils.toString(locale);
            aliasName += "-" + LocaleUtils.toString(locale);
            defaultAnalyzer = localeMapping.entrySet().stream()
                    .filter(entry -> localeValue.matches(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(defaultAnalyzer);
        }

        Settings.Builder settingsBuilder = Settings.builder();
        settings.forEach(settingsBuilder::put);
        ignoredSettings.forEach(settingsBuilder::remove);

        String indexName = aliasName + indexSuffix;
        if (doIndexExist(client, createAlias ? aliasName : indexName)) {

            try {
                client.indices().close(new CloseIndexRequest(indexName), RequestOptions.DEFAULT);
                logger.info("Index '{}' already indexExists, updating settings", indexName);
                client.indices().putSettings(new UpdateSettingsRequest().indices(indexName).settings(settingsBuilder), RequestOptions.DEFAULT);

                client.indices().open(new OpenIndexRequest(indexName), RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new OpenSearchException(aliasName, format("Error updating settings for index '%s'", indexName), e);
            }
            return;
        }
        logger.info("Creating index {}", indexName);

        settingsBuilder.put(ES_KEY_DEFAULT_ANALYZER, defaultAnalyzer);
        try (InputStream is = mapping.getInputStream()) {
            CreateIndexRequest request = new CreateIndexRequest(indexName)
                    .settings(settingsBuilder.build())
                    .mapping(IOUtils.toString(is, UTF_8), XContentType.JSON);
            if (createAlias) {
                logger.info("Creating alias {}", aliasName);
                request.alias(new Alias(aliasName));
            }
            client.indices().create(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new OpenSearchException(aliasName, "Error creating index " + indexName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndexes(final String aliasName) throws OpenSearchException {
        doDeleteIndexes(openSearchClient, aliasName);
    }

    /**
     * Performs the index delete using the given OpenSearch client
     */
    protected void doDeleteIndexes(RestHighLevelClient client, String aliasName) {
        try {
            GetAliasesResponse indices = client.indices().getAlias(
                    new GetAliasesRequest(aliasName + "*"),
                    RequestOptions.DEFAULT);
            Set<String> actualIndices = indices.getAliases().keySet();
            logger.info("Deleting indices {}", actualIndices);
            client.indices().delete(
                    new DeleteIndexRequest(actualIndices.toArray(new String[]{})),
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new OpenSearchException(aliasName, "Error deleting index " + aliasName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recreateIndex(String aliasName) throws OpenSearchException {
        doRecreateIndex(openSearchClient, aliasName);
    }

    /**
     * Performs all operations for recreating an index using the given OpenSearch client
     */
    protected void doRecreateIndex(RestHighLevelClient client, String aliasName) {
        logger.info("Recreating index for alias {}", aliasName);
        try {
            List<String> existingIndexes = doGetIndexes(client, aliasName);
            for (String indexName : existingIndexes) {
                logger.info("Found index {} for alias {}", indexName, aliasName);

                // get the locale from the alias name
                Locale locale = getLocale(indexName);

                // get the version of the existing index
                String[] tokens = indexName.split("_v");
                if (tokens.length != 2) {
                    throw new IllegalStateException("Could not find current version for index: " + indexName);
                }
                int currentVersion = Integer.parseInt(tokens[1]);

                // create a new index
                String newVersion = "_v" + (currentVersion + 1);
                logger.debug("Using new version {} for index {}", newVersion, indexName);

                // copy the supported settings from the existing index
                Map<String, String> settings = doGetIndexSettings(client, aliasName);

                doCreateIndex(client, aliasName, newVersion, locale, false, settings);
                String newIndexName = locale == null ? aliasName + newVersion :
                        aliasName + "-" + LocaleUtils.toString(locale) + newVersion;

                // index all existing content into the new index
                doReindex(client, indexName, newIndexName);

                // swap indexes
                doSwap(client, aliasName, indexName, newIndexName);

                // delete the previous index
                doDeleteIndex(client, indexName);
            }
        } catch (Exception e) {
            throw new OpenSearchException(aliasName, "Error upgrading index " + aliasName, e);
        }
    }

    private static Locale getLocale(String indexName) {
        Locale locale = null;
        String localeValue = substringBeforeLast(substringAfterLast(indexName, "-"), "_");
        if (contains(localeValue, "_")) {
            locale = LocaleUtils.parseLocale(localeValue);
            if (locale != null) {
                logger.info("Found locale {} for index {}", locale, indexName);
            }
        }
        return locale;
    }

    protected List<String> doGetIndexes(RestHighLevelClient client, String aliasName) throws IOException {
        GetAliasesResponse indices =
                client.indices().getAlias(new GetAliasesRequest(aliasName + "*"), RequestOptions.DEFAULT);
        return IteratorUtils.toList(indices.getAliases().keySet().iterator());
    }

    protected Map<String, String> doGetIndexSettings(RestHighLevelClient client, String indexAlias) throws IOException {
        List<String> indices = doGetIndexes(client, indexAlias);
        if (indices.isEmpty()) {
            throw new IndexNotFoundException(indexAlias);
        }
        String indexName = indices.get(0);
        GetSettingsResponse response =
                client.indices().getSettings(new GetSettingsRequest().indices(indexAlias), RequestOptions.DEFAULT);
        Settings indexSettings = response.getIndexToSettings().get(indexName);
        Map<String, String> settings = new HashMap<>(defaultSettings);
        indexSettings.keySet().forEach(key -> settings.put(key, indexSettings.get(key)));
        return settings;
    }

    protected void doReindex(RestHighLevelClient client, String sourceIndex, String destinationIndex)
            throws IOException {
        logger.info("Reindexing all existing content from {} to {}", sourceIndex, destinationIndex);
        BulkByScrollResponse response = client.reindex(
                new ReindexRequest().setSourceIndices(sourceIndex).setDestIndex(destinationIndex).setRefresh(true),
                RequestOptions.DEFAULT);
        logger.info("Successfully indexed {} docs into {}", response.getTotal(), destinationIndex);
    }

    protected void doSwap(RestHighLevelClient client, String aliasName, String existingIndexName, String newIndexName)
            throws IOException {
        logger.info("Swapping index {} with {}", existingIndexName, newIndexName);
        client.indices().updateAliases(new IndicesAliasesRequest()
                .addAliasAction(
                        new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                                .index(newIndexName)
                                .alias(aliasName))
                .addAliasAction(
                        new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                                .index(existingIndexName)
                                .alias(aliasName)
                ), RequestOptions.DEFAULT);
    }

    protected void doDeleteIndex(RestHighLevelClient client, String indexName) throws IOException {
        logger.info("Deleting index {}", indexName);
        client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    @Override
    public void waitUntilReady() {
        doWaitUntilReady(openSearchClient);
    }

    @Override
    public void duplicateIndex(String srcAliasName, String destAliasName) throws OpenSearchException {
        doDuplicateIndex(openSearchClient, srcAliasName, destAliasName);
    }

    protected void doDuplicateIndex(RestHighLevelClient client, String srcAliasName, String destAliasName) throws OpenSearchException {
        try {
            doCreateIndex(client, destAliasName, indexNameSuffix,
                    getLocale(srcAliasName), true, doGetIndexSettings(client, srcAliasName));
            doReindex(client, srcAliasName, destAliasName);
        } catch (IOException e) {
            throw new OpenSearchException(srcAliasName, format("Error duplicating index '%s'", srcAliasName), e);
        }
    }

    protected void doWaitUntilReady(RestHighLevelClient client) {
        logger.info("Waiting for OpenSearch cluster to be ready");
        boolean ready = false;
        do {
            try {
                ready = client.ping(RequestOptions.DEFAULT);
            } catch (IOException e) {
                logger.debug("Error pinging OpenSearch cluster", e);
            }
            if (!ready) {
                logger.info("OpenSearch cluster not ready, will try again in 5 seconds");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("Error waiting for OpenSearch cluster to be ready", e);
                }
            }
        } while (!ready);
    }

    @Override
    public void close() throws Exception {
        openSearchClient.close();
    }

}
