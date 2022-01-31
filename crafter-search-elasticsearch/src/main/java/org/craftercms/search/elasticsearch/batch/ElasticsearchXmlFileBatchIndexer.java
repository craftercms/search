/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

import java.beans.ConstructorProperties;
import java.util.Locale;
import java.util.Map;

import org.craftercms.commons.locale.LocaleUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractXmlFileBatchIndexer;
import org.craftercms.search.locale.LocaleExtractor;

/**
 * Implementation of {@link AbstractXmlFileBatchIndexer} for Elasticsearch
 * @author joseross
 */
public class ElasticsearchXmlFileBatchIndexer extends AbstractXmlFileBatchIndexer {

    protected ElasticsearchAdminService elasticsearchAdminService;

    protected LocaleExtractor localeExtractor;

    protected final boolean enbleTranslation;

    /**
     * Elasticsearch service
     */
    protected ElasticsearchService elasticsearchService;

    @ConstructorProperties({"elasticsearchAdminService", "localeExtractor", "elasticsearchService",
            "enableTranslation"})
    public ElasticsearchXmlFileBatchIndexer(ElasticsearchAdminService elasticsearchAdminService,
                                            LocaleExtractor localeExtractor,
                                            ElasticsearchService elasticsearchService,
                                            boolean enableTranslation) {
        this.elasticsearchAdminService = elasticsearchAdminService;
        this.localeExtractor = localeExtractor;
        this.elasticsearchService = elasticsearchService;
        this.enbleTranslation = enableTranslation;
    }

    @Override
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService,
                                      Context context, String path, boolean delete, UpdateDetail updateDetail,
                                      UpdateStatus updateStatus, Map<String, Object> metadata) {
        if (delete) {
            doDelete(indexId, siteName, path, updateStatus);
        } else {
            String xml = processXml(siteName, contentStoreService, context, path);

            if (enbleTranslation) {
                // get the locale for the item
                Locale locale = localeExtractor.extract(context, path);
                if (locale != null) {
                    // check if locale specific index exists
                    elasticsearchAdminService.createIndex(indexId, locale);
                    // update the index name
                    indexId += "-" + LocaleUtils.toString(locale);
                }
            }
            doUpdate(indexId, siteName, path, xml, updateDetail, updateStatus, metadata);
        }
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        ElasticsearchIndexingUtils.doDelete(elasticsearchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdate(final String indexId, final String siteName, final String path, final String xml,
                            final UpdateDetail updateDetail, final UpdateStatus updateStatus,
                            Map<String, Object> metadata) {
        ElasticsearchIndexingUtils.doUpdate(elasticsearchService, indexId, siteName, path, xml, updateDetail,
            updateStatus, metadata);
    }

}
