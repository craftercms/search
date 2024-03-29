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

import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.impl.AbstractBinaryFileBatchIndexer;
import org.craftercms.search.opensearch.OpenSearchService;

import java.beans.ConstructorProperties;
import java.util.Map;

/**
 * Implementation of {@link AbstractBinaryFileBatchIndexer} for OpenSearch
 * @author joseross
 */
public class OpenSearchBinaryFileBatchIndexer extends AbstractBinaryFileBatchIndexer {

    /**
     * OpenSearch service
     */
    protected final OpenSearchService searchService;

    @ConstructorProperties({"searchService"})
    public OpenSearchBinaryFileBatchIndexer(final OpenSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        OpenSearchIndexingUtils.doDelete(searchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String path,
                                   final Content binaryContent, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus, Map<String, Object> metadata) {
        OpenSearchIndexingUtils.doUpdateBinary(searchService, indexId, siteName, path, metadata,
            binaryContent, updateDetail, updateStatus);
    }

}
