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

package org.craftercms.search.opensearch.batch;

import java.beans.ConstructorProperties;
import java.util.Locale;
import java.util.Map;

import org.craftercms.commons.locale.LocaleUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.opensearch.OpenSearchAdminService;
import org.craftercms.search.opensearch.OpenSearchService;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractXmlFileBatchIndexer;
import org.craftercms.search.locale.LocaleExtractor;

/**
 * Implementation of {@link AbstractXmlFileBatchIndexer} for Elasticsearch
 * @author joseross
 */
public class OpenSearchXmlFileBatchIndexer extends AbstractXmlFileBatchIndexer {

    protected final OpenSearchAdminService openSearchAdminService;

    protected final LocaleExtractor localeExtractor;

    protected final boolean enableTranslation;

    /**
     * Elasticsearch service
     */
    protected final OpenSearchService openSearchService;

    @ConstructorProperties({"openSearchAdminService", "localeExtractor", "openSearchService",
            "enableTranslation"})
    public OpenSearchXmlFileBatchIndexer(final OpenSearchAdminService openSearchAdminService,
                                         final LocaleExtractor localeExtractor,
                                         final OpenSearchService openSearchService,
                                         final boolean enableTranslation) {
        this.openSearchAdminService = openSearchAdminService;
        this.localeExtractor = localeExtractor;
        this.openSearchService = openSearchService;
        this.enableTranslation = enableTranslation;
    }

    @Override
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService,
                                      Context context, String path, boolean delete, UpdateDetail updateDetail,
                                      UpdateStatus updateStatus, Map<String, Object> metadata) {
        if (delete) {
            doDelete(indexId, siteName, path, updateStatus);
        } else {
            String xml = processXml(siteName, contentStoreService, context, path);

            if (enableTranslation) {
                // get the locale for the item
                Locale locale = localeExtractor.extract(context, path);
                if (locale != null) {
                    // check if locale specific index exists
                    openSearchAdminService.createIndex(indexId, locale);
                    // update the index name
                    indexId += "-" + LocaleUtils.toString(locale);
                }
            }
            doUpdate(indexId, siteName, path, xml, updateDetail, updateStatus, metadata);
        }
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doDelete(openSearchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdate(final String indexId, final String siteName, final String path, final String xml,
                            final UpdateDetail updateDetail, final UpdateStatus updateStatus,
                            Map<String, Object> metadata) {
        OpenSearchIndexingUtils.doUpdate(openSearchService, indexId, siteName, path, xml, updateDetail,
            updateStatus, metadata);
    }

}
