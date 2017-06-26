/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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

import java.util.List;
import javax.activation.FileTypeMap;

import org.apache.commons.collections.CollectionUtils;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.SearchService;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

import static org.craftercms.search.batch.utils.IndexingUtils.*;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes binary or structured document files (PDF,
 * Word, etc.) from a search index, only if their mime types match the supported mime types or if the supported mime
 * types map is empty.
 *
 * @author avasquez
 */
public class BinaryFileBatchIndexer extends AbstractBatchIndexer {

    protected List<String> supportedMimeTypes;
    protected FileTypeMap mimeTypesMap;

    public BinaryFileBatchIndexer() {
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    protected void doSingleFileUpdate(SearchService searchService,String indexId, String siteName,
                                      ContentStoreService contentStoreService, Context context,
                                      String path, boolean delete, UpdateStatus updateStatus) throws Exception {
        if (delete) {
            doDelete(searchService, indexId, siteName, path, updateStatus);
        } else {
            Content binaryContent = contentStoreService.getContent(context, path);
            doUpdateContent(searchService, indexId, siteName, path, binaryContent, updateStatus);
        }
    }

    @Override
    protected boolean include(String path) {
        if (super.include(path)) {
            if (CollectionUtils.isNotEmpty(supportedMimeTypes)) {
                String mimeType = mimeTypesMap.getContentType(path);
                if (supportedMimeTypes.contains(mimeType)) {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

}
