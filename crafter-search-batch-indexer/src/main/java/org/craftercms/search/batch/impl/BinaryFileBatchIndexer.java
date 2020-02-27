/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.batch.impl;

import java.util.Map;

import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.Content;
import org.craftercms.search.batch.utils.CrafterSearchIndexingUtils;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link AbstractBinaryFileBatchIndexer} that uses {@link SearchService}.
 * @author joseross
 */
public class BinaryFileBatchIndexer extends AbstractBinaryFileBatchIndexer {

    /**
     * Instance of {@link SearchService}
     */
    protected SearchService searchService;

    @Required
    public void setSearchService(final SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doDelete(final String indexId, final String siteName, final String path,
                            final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doDelete(searchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String path,
                                   final Content binaryContent, final UpdateDetail updateDetail,
                                   final UpdateStatus updateStatus, Map<String, String> metadata) {
        CrafterSearchIndexingUtils.doUpdateContent(searchService, indexId, siteName, path, binaryContent, updateStatus);
    }

}