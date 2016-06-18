package org.craftercms.search.batch.exception;

import org.craftercms.search.exception.SearchException;

/**
 * Created by alfonsovasquez on 2/6/16.
 */
public class BatchIndexingException extends SearchException {

    public BatchIndexingException(String msg) {
        super(msg);
    }

    public BatchIndexingException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
