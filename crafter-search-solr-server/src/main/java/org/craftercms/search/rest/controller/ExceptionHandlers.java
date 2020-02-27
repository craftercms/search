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
package org.craftercms.search.rest.controller;

import org.craftercms.commons.exceptions.InvalidManagementTokenException;
import org.craftercms.commons.rest.BaseRestExceptionHandlers;
import org.craftercms.search.exception.IndexNotFoundException;
import org.craftercms.search.exception.SearchServerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Contains all the REST exception handlers.
 *
 * @author avasquez
 */
@ControllerAdvice
public class ExceptionHandlers extends BaseRestExceptionHandlers {

    @ExceptionHandler(IndexNotFoundException.class)
    public ResponseEntity<Object> handleIndexNotFoundException(IndexNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "Index not found", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(SearchServerException.class)
    public ResponseEntity<Object> handleSearchServerException(SearchServerException ex, WebRequest request) {
        return handleExceptionInternal(ex, "Service unavailable, please try again later", new HttpHeaders(),
            HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(InvalidManagementTokenException.class)
    public ResponseEntity<Object> handleInvalidManagementTokenException(InvalidManagementTokenException ex,
                                                                        WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED,
                                       request);
    }

}
