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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes binary or structured document files (PDF,
 * Word, etc.) from a search index, only if their mime types match the supported mime types or if the supported mime
 * types map is empty.
 *
 * @author avasquez
 */
public class BinaryFileBatchIndexer extends AbstractBatchIndexer {

    private static final Log logger = LogFactory.getLog(BinaryFileBatchIndexer.class);

    protected List<String> supportedMimeTypes;
    protected FileTypeMap mimeTypesMap;

    public BinaryFileBatchIndexer() {
        mimeTypesMap = new ConfigurableMimeFileTypeMap();
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    protected boolean doSingleFileUpdate(String indexId, String siteName, String rootFolder, String fileName,
                                         boolean delete) throws BatchIndexingException {
        File file = new File(rootFolder, fileName);
        String mimeType = mimeTypesMap.getContentType(fileName);
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
                return doDelete(indexId, siteName, fileName);
            } else {
                return doUpdateFile(indexId, siteName, fileName, file);
            }
        }

        return false;
    }

}
