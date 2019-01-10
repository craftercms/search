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

package org.craftercms.search.batch.impl;

import org.craftercms.commons.search.batch.UpdateStatus;
import org.craftercms.commons.search.batch.impl.AbstractBinaryFileBatchIndexer;
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
    protected void doDelete(final String indexId, final String siteName, final String path, final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doDelete(searchService, indexId, siteName, path, updateStatus);
    }

    @Override
    protected void doUpdateContent(final String indexId, final String siteName, final String path, final Content binaryContent, final UpdateStatus updateStatus) {
        CrafterSearchIndexingUtils.doUpdateContent(searchService, indexId, siteName, path, binaryContent, updateStatus);
    }
    
}
