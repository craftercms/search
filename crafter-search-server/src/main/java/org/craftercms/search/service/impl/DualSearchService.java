package org.craftercms.search.service.impl;

import java.io.File;
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
    public String update(String site, String id, String xml, boolean ignoreRootInFieldNames) throws SearchException {
        if (writeService != null) {
            return writeService.update(site, id, xml, ignoreRootInFieldNames);
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
    public String commit() throws SearchException {
        if (writeService != null) {
            return writeService.commit();
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateDocument(String site, String id, File document) throws SearchException {
        if (writeService != null) {
            return writeService.updateDocument(site, id, document);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    @Override
    public String updateDocument(String site, String id, File document,
                                 Map<String, String> additionalFields) throws SearchException {
        if (writeService != null) {
            return writeService.updateDocument(site, id, document, additionalFields);
        } else {
            return handleNoWriteServiceProvided();
        }
    }

    private String handleNoWriteServiceProvided() {
        logger.warn(NO_WRITE_SERVICE_PROVIDED_MSG);

        return NO_WRITE_SERVICE_PROVIDED_MSG;
    }

}
