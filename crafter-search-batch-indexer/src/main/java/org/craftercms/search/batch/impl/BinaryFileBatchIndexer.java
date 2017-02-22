/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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

import java.io.File;
import java.util.List;
import javax.activation.FileTypeMap;

import org.apache.commons.collections.CollectionUtils;
import org.craftercms.core.exception.CrafterException;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.IndexingStatus;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

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
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                                      String path, boolean delete, IndexingStatus status) throws Exception {
        String mimeType = mimeTypesMap.getContentType(path);
        boolean doUpdate = false;

        if (CollectionUtils.isNotEmpty(supportedMimeTypes)) {
            if (supportedMimeTypes.contains(mimeType)) {
                doUpdate = true;
            }
        } else {
            doUpdate = true;
        }

        if (doUpdate) {
            if (delete) {
                doDelete(indexId, siteName, path, status);
            } else {
                Content binaryContent = contentStoreService.getContent(context, path);
                doUpdateContent(indexId, siteName, path, binaryContent, status);
            }
        }
    }

}
