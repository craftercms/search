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
package org.craftercms.search.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * Thrown when an error occurs in the {@link org.craftercms.search.service.SearchService}.
 *
 * @author Alfonso Vásquez
 */
public class SearchException extends RuntimeException {

    protected String indexId;

    public SearchException(String msg) {
        super(msg);
    }

    public SearchException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SearchException(String indexId, String msg) {
        super(StringUtils.isNotEmpty(indexId)? "[" + indexId + "] " + msg : msg);

        this.indexId = indexId;
    }

    public SearchException(String indexId, String msg, Throwable throwable) {
        super(StringUtils.isNotEmpty(indexId)? "[" + indexId + "] " + msg : msg, throwable);

        this.indexId = indexId;
    }

    public String getIndexId() {
        return indexId;
    }

}
