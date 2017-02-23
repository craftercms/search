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
package org.craftercms.search.rest.controller;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.Collections;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.commons.validation.ValidationResult;
import org.craftercms.search.exception.IndexNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Created by alfonsovasquez on 12/19/16.
 */
@ControllerAdvice
public class ExceptionHandlers extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IndexNotFoundException.class)
    public ResponseEntity<Object> handleIndexNotFoundException(IndexNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "Index not found", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        Throwable cause = ExceptionUtils.getRootCause(ex);
        if (cause instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException)cause;
            String field = upe.getPropertyName();

            ValidationResult result = new ValidationResult();
            result.addFieldError(field, "Unrecognized field");

            return handleExceptionInternal(ex, result, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        } else {
            return handleExceptionInternal(ex, "Invalid or missing request body", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR,
                                           request);
        }
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        ValidationResult result = new ValidationResult();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            result.addFieldError(fieldError.getField(), "Missing required field");
        }

        return handleExceptionInternal(ex, result, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, String message, HttpHeaders headers, HttpStatus status,
                                                             WebRequest request) {
        return handleExceptionInternal(ex, new Result(message), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status,
                                                             WebRequest request) {
        logger.error("Request " + ((ServletWebRequest) request).getRequest().getRequestURI() + " failed with status " + status, ex);

        if (body == null) {
            body = new Result(ex.getMessage());
        }

        return new ResponseEntity<>(body, headers, status);
    }

}
