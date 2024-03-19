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
package org.craftercms.search.batch.impl;

import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

import jakarta.activation.FileTypeMap;
import java.util.List;
import java.util.Map;

import static org.craftercms.search.batch.utils.IndexingUtils.isMimeTypeSupported;


/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes binary or structured document files
 * (PDF,
 * Word, etc.) from a search index, only if their mime types match the supported mime types or if the supported mime
 * types map is empty.
 *
 * @author avasquez
 */
public abstract class AbstractBinaryFileBatchIndexer extends AbstractBatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBinaryFileBatchIndexer.class);

    protected List<String> supportedMimeTypes;
    protected FileTypeMap mimeTypesMap;

    public AbstractBinaryFileBatchIndexer() {
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService,
                                      Context context, String path, boolean delete, UpdateDetail updateDetail,
                                      UpdateStatus updateStatus, Map<String, Object> metadata) {
        if (delete) {
            doDelete(indexId, siteName, path, updateStatus);
        } else {
            Content binaryContent = contentStoreService.findContent(context, path);
            if (binaryContent != null && binaryContent.getLength() > 0) {
                doUpdateContent(indexId, siteName, path, binaryContent, updateDetail, updateStatus, metadata);
            } else {
                logger.debug("No binary file found at '{}':'{}'. Skipping update", siteName, path);
            }
        }
    }

    protected abstract void doDelete(String indexId, String siteName, String path, UpdateStatus updateStatus);

    protected abstract void doUpdateContent(String indexId, String siteName, String path, Content binaryContent,
                                            UpdateDetail updateDetail, UpdateStatus updateStatus,
                                            Map<String, Object> metadata);

    @Override
    protected boolean include(String path) {
        if (super.include(path)) {
            return isMimeTypeSupported(mimeTypesMap, supportedMimeTypes, path);
        }

        return false;
    }

}
