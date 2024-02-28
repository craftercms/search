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
 * Implementation of {@link AbstractXmlFileBatchIndexer} for OpenSearch
 * @author joseross
 */
public class OpenSearchXmlFileBatchIndexer extends AbstractXmlFileBatchIndexer {

    protected final OpenSearchAdminService searchAdminService;

    protected final LocaleExtractor localeExtractor;

    protected final boolean enableTranslation;

    /**
     * OpenSearch service
     */
    protected final OpenSearchService searchService;

    @ConstructorProperties({"searchAdminService", "localeExtractor", "searchService",
            "enableTranslation"})
    public OpenSearchXmlFileBatchIndexer(final OpenSearchAdminService searchAdminService,
                                         final LocaleExtractor localeExtractor,
                                         final OpenSearchService searchService,
                                         final boolean enableTranslation) {
        this.searchAdminService = searchAdminService;
        this.localeExtractor = localeExtractor;
        this.searchService = searchService;
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
                    // check if locale specific index indexExists
                    searchAdminService.createIndex(indexId, locale);
                    // update the index name
                    indexId += "-" + LocaleUtils.toString(locale);
                }
            }
            doUpdate(indexId, siteName, path, xml, updateDetail, updateStatus, metadata);
        }
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doDelete(searchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdate(final String indexId, final String siteName, final String path, final String xml,
                            final UpdateDetail updateDetail, final UpdateStatus updateStatus,
                            Map<String, Object> metadata) {
        OpenSearchIndexingUtils.doUpdate(searchService, indexId, siteName, path, xml, updateDetail,
            updateStatus, metadata);
    }

}
