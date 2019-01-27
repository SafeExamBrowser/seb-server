/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ch.ethz.seb.sebserver.gbl.model.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class APIExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(APIExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            final Exception ex,
            final Object body,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {

        log.error("Unexpected generic error catched at the API endpoint: ", ex);
        return new ResponseEntity<>(APIMessage.ErrorMessage.GENERIC.of(ex.getMessage()), status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {

        final Collection<APIMessage> valErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field -> APIMessage.fieldValidationError(field))
                .collect(Collectors.toList());

        return new ResponseEntity<>(valErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BeanValidationException.class)
    public ResponseEntity<Object> handleBeanValidationException(
            final BeanValidationException ex,
            final WebRequest request) {

        final Collection<APIMessage> valErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field -> APIMessage.fieldValidationError(field))
                .collect(Collectors.toList());

        return new ResponseEntity<>(valErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(
            final ResourceNotFoundException ex,
            final WebRequest request) {

        return APIMessage.ErrorMessage.RESOURCE_NOT_FOUND
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(
            final UsernameNotFoundException ex,
            final WebRequest request) {

        return APIMessage.ErrorMessage.UNAUTHORIZED
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Object> handleNoPermission(
            final PermissionDeniedException ex,
            final WebRequest request) {

        log.warn("Permission Denied Exception: ", ex);
        return APIMessage.ErrorMessage.FORBIDDEN
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalAPIArgumentException.class)
    public ResponseEntity<Object> handleIllegalAPIArgumentException(
            final IllegalAPIArgumentException ex,
            final WebRequest request) {

        log.warn("Illegal API Argument Exception: ", ex);
        return APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(
            final Exception ex,
            final WebRequest request) {

        log.error("Unexpected internal error catched at the API endpoint: ", ex);
        return APIMessage.ErrorMessage.UNEXPECTED
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(APIMessageException.class)
    public ResponseEntity<Object> handleAPIMessageException(
            final APIMessageException ex,
            final WebRequest request) {

        return new ResponseEntity<>(
                Arrays.asList(ex.getAPIMessage()),
                HttpStatus.BAD_REQUEST);
    }

}
