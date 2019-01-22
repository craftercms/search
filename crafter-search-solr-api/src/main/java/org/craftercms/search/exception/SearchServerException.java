/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.exception;

/**
 * Exception thrown when the search engine is not available.
 * @author joseross
 */
public class SearchServerException extends SearchException {

    public SearchServerException(final String msg) {
        super(msg);
    }

    public SearchServerException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    public SearchServerException(final String indexId, final String msg) {
        super(indexId, msg);
    }

    public SearchServerException(final String indexId, final String msg, final Throwable throwable) {
        super(indexId, msg, throwable);
    }

}
