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
package org.craftercms.search.service.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link SearchService} decorator that uses two different search services: one for reads and another one for writes.
 * The read service is required. If no write service is provided and any type of update or commit is attempted, the
 * service silently fails with a warning.
 *
 * @author avasquez
 */
public class DualSearchService implements SearchService {

    public static final String NO_WRITE_SERVICE_PROVIDED_MSG = "No write service provided. All updates and commits " +
                                                               "are ignored";

    private static final Log logger = LogFactory.getLog(SolrSearchService.class);

    private SearchService readService;
    private SearchService writeService;

    @Required
    public void setReadService(SearchService readService) {
        this.readService = readService;
    }

    public void setWriteService(SearchService writeService) {
        this.writeService = writeService;
    }

    @Override
    public Map<String, Object> search(Query query) throws SearchException {
        return readService.search(query);
    }

    @Override
    public Map<String, Object> search(String indexId, Query query) throws SearchException {
        return readService.search(indexId, query);
    }

    @Override
    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        if (writeService != null) {
            return writeService.update(site, id, xml, ignoreRootInFieldNames);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String update(String indexId, String site, String id, String xml,
                         boolean ignoreRootInFieldNames) throws SearchException {
        if (writeService != null) {
            return writeService.update(indexId, site, id, xml, ignoreRootInFieldNames);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String delete(String site, String id) throws SearchException {
        if (writeService != null) {
            return writeService.delete(site, id);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String delete(String indexId, String site, String id) throws SearchException {
        if (writeService != null) {
            return writeService.delete(indexId, site, id);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String commit() throws SearchException {
        if (writeService != null) {
            return writeService.commit();
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String commit(String indexId) throws SearchException {
        if (writeService != null) {
            return writeService.commit(indexId);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document) throws SearchException {
        if (writeService != null) {
            return writeService.updateDocument(site, id, document);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    @Deprecated
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        if (writeService != null) {
            return writeService.updateDocument(site, id, document, additionalFields);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateFile(String site, String id, File file) throws SearchException {
        if (writeService != null) {
            return writeService.updateFile(site, id, file);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file) throws SearchException {
        if (writeService != null) {
            return writeService.updateFile(indexId, site, id, file);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateFile(String site, String id, File file,
                             Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            return writeService.updateFile(site, id, file, additionalFields);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateFile(String indexId, String site, String id, File file,
                             Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            return writeService.updateFile(indexId, site, id, file, additionalFields);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    private String handleNoWriteServiceProvided() {
        logger.warn(NO_WRITE_SERVICE_PROVIDED_MSG);

        return NO_WRITE_SERVICE_PROVIDED_MSG;
    }

}
