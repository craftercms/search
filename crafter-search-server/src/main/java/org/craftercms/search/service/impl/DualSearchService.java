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
import java.io.InputStream;
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
    public void update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        if (writeService != null) {
            writeService.update(site, id, xml, ignoreRootInFieldNames);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void update(String indexId, String site, String id, String xml,
                         boolean ignoreRootInFieldNames) throws SearchException {
        if (writeService != null) {
            writeService.update(indexId, site, id, xml, ignoreRootInFieldNames);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void delete(String site, String id) throws SearchException {
        if (writeService != null) {
            writeService.delete(site, id);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void delete(String indexId, String site, String id) throws SearchException {
        if (writeService != null) {
            writeService.delete(indexId, site, id);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void commit() throws SearchException {
        if (writeService != null) {
            writeService.commit();
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void commit(String indexId) throws SearchException {
        if (writeService != null) {
            writeService.commit(indexId);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String site, String id, File file) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(site, id, file);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String indexId, String site, String id, File file) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(indexId, site, id, file);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String site, String id, File file,
                              Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(site, id, file, additionalFields);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String indexId, String site, String id, File file,
                              Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(indexId, site, id, file, additionalFields);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String site, String id, InputStream content) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(site, id, content);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String indexId, String site, String id, InputStream content) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(indexId, site, id, content);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String site, String id, InputStream content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(site, id, content, additionalFields);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    @Override
    public void updateContent(String indexId, String site, String id, InputStream content,
                              Map<String, List<String>> additionalFields) throws SearchException {
        if (writeService != null) {
            writeService.updateContent(indexId, site, id, content, additionalFields);
        } else {
            handleNoWriteServiceProvided();
        }
    }

    private String handleNoWriteServiceProvided() {
        logger.warn(NO_WRITE_SERVICE_PROVIDED_MSG);

        return NO_WRITE_SERVICE_PROVIDED_MSG;
    }

}
