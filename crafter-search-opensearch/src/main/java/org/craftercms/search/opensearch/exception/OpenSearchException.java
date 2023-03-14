/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.exception;

import org.craftercms.search.commons.exception.SearchException;

/**
 * Base exception for all OpenSearch errors
 * @author joseross
 */
public class OpenSearchException extends SearchException {

    public OpenSearchException(final String indexId, final String msg) {
        super(indexId, msg);
    }

    public OpenSearchException(final String indexId, final String msg, final Throwable throwable) {
        super(indexId, msg, throwable);
    }

}
