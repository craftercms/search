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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.batch.impl.AbstractMetadataBatchIndexer;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.craftercms.search.batch.BatchIndexer} that adds an internal name field to all documents.
 * For XML descriptors an XPath selector is used to query the value, for binary files the full file name is used.
 * @author joseross
 */
public class ElasticsearchInternalNameBatchIndexer extends AbstractMetadataBatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchInternalNameBatchIndexer.class);

    public static final String PROPERTY_NAME_INTERNAL_NAME = "internalName";

    /**
     * The Elasticsearch service
     */
    protected ElasticsearchService elasticsearchService;

    /**
     * The XPath selector used to extract the internal name from descriptors
     */
    protected String internalNameFieldXpath;

    @Required
    public void setElasticsearchService(final ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Required
    public void setInternalNameFieldXpath(final String internalNameFieldXpath) {
        this.internalNameFieldXpath = internalNameFieldXpath;
    }

    @Override
    protected void updateIndex(final String indexName, final String siteName, final String path,
                               final Map<String, Object> data) {
        ElasticsearchIndexingUtils.doUpdate(elasticsearchService, indexName, siteName, path, data);
    }

    @Override
    protected Map<String, Object> getCurrentData(final String indexName, final String siteName, final String path) {
        return ElasticsearchIndexingUtils.doSearchById(elasticsearchService, indexName, path);
    }

    @Override
    protected Map<String, Object> getMetadata(final Item item, final ContentStoreService contentStoreService,
                                              final Context context) {
        String internalName = item.queryDescriptorValue(internalNameFieldXpath);
        if(StringUtils.isEmpty(internalName)) {
            internalName = FilenameUtils.getName(item.getName());
        }
        if(StringUtils.isNotEmpty(internalName)) {
            logger.debug("Fund internal name {} for file {}", internalName, item.getUrl());
            return Collections.singletonMap(PROPERTY_NAME_INTERNAL_NAME, internalName);
        } else {
            logger.debug("Could not found internal name for file {}", item.getUrl());
            return null;
        }
    }

    @Override
    protected boolean isCompatible(final Item item) {
        return !item.isFolder();
    }

}
