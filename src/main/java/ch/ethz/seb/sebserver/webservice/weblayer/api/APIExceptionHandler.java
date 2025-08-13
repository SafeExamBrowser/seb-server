/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.servicelayer.dao.NoResourceFoundException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.TooManyRequests;
import ch.ethz.seb.sebserver.gbl.util.Utils;
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
            @Nullable final Object body,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {

        if (ex instanceof AccessDeniedException) {
            log.warn("Access denied: ", ex);
        } else if (ex instanceof OnlyMessageLogExceptionWrapper) {
            ((OnlyMessageLogExceptionWrapper) ex).log(log);
            return new ResponseEntity<>(status);
        } else if (ex instanceof HttpMessageNotReadableException) {
            final String message = ex.getMessage();
            if (message.contains("I/O error") || message.contains("Connection reset")) {
                log.warn("Client IO error probably due to client disconnection. cause: {}", message);
                return null;
            }
        } else {
            log.error("Unexpected generic error caught at the API endpoint: ", ex);
        }

        final List<APIMessage> errors = Arrays.asList(APIMessage.ErrorMessage.GENERIC.of(ex.getMessage()));
        return new ResponseEntity<>(
                errors,
                Utils.createJsonContentHeader(),
                status);
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
                .map(APIMessage::fieldValidationError)
                .collect(Collectors.toList());

        return new ResponseEntity<>(
                valErrors,
                Utils.createJsonContentHeader(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TooManyRequests.class)
    public ResponseEntity<Object> handleToManyRequests(
            final TooManyRequests ex,
            final WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(String.valueOf(ex.code));
    }

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<Object> httpMessageNotReadableException(
//            final HttpMessageNotReadableException ex,
//            final WebRequest request) {
//
//        final String message = ex.getMessage();
//        if (message.contains("I/O error") || message.contains("Connection reset")) {
//            log.warn("Client IO error probably due to client disconnection. cause: {}", message);
//            return null;
//        }
//
//        return handleRuntimeException(ex, request);
//    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(
            final RuntimeException ex,
            final WebRequest request) {

        log.error("Unexpected internal error caught at the API endpoint: ", ex);
        final List<APIMessage> errors = Arrays.asList(APIMessage.ErrorMessage.UNEXPECTED.of(ex.getMessage()));
        return new ResponseEntity<>(
                errors,
                Utils.createJsonContentHeader(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(OnlyMessageLogExceptionWrapper.class)
    public ResponseEntity<Object> onlyMessageLogExceptionWrapper(
            final OnlyMessageLogExceptionWrapper ex,
            final WebRequest request) {

        ex.log(log);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<Object> handleOAuth2Exception(
            final OAuth2Exception ex,
            final WebRequest request) {

        log.warn("OAuth2Exception: ", ex);
        final APIMessage message = APIMessage.ErrorMessage.UNAUTHORIZED.of(ex.getMessage());
        return new ResponseEntity<>(
                message,
                Utils.createJsonContentHeader(),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BeanValidationException.class)
    public ResponseEntity<Object> handleBeanValidationException(
            final BeanValidationException ex,
            final WebRequest request) {

        final Collection<APIMessage> valErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(APIMessage::fieldValidationError)
                .collect(Collectors.toList());

        return new ResponseEntity<>(
                valErrors,
                Utils.createJsonContentHeader(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            final IllegalArgumentException ex,
            final WebRequest request) {

        log.warn("Illegal argument or state detected: {}\n    send 400 Bad Request response", ex.getMessage());
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFoundException(
            final NoResourceFoundException ex,
            final WebRequest request) {

        return APIMessage.ErrorMessage.RESOURCE_NOT_FOUND
                .createErrorResponse(ex.getMessage());
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

        log.info("Permission Denied Exception: {}", ex.getMessage());
        return APIMessage.ErrorMessage.FORBIDDEN
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ExamNotRunningException.class)
    public ResponseEntity<Object> handleExamNotRunning(
            final ExamNotRunningException ex,
            final WebRequest request) {

        log.debug("{}", ex.getMessage());
        return APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(APIConstraintViolationException.class)
    public ResponseEntity<Object> handleIllegalAPIArgumentException(
            final APIConstraintViolationException ex,
            final WebRequest request) {

        log.info("Illegal API Argument Exception: {}", ex.getMessage());

        return APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
            final AccessDeniedException ex,
            final WebRequest request) {

        log.info("Access denied: ", ex);
        return APIMessage.ErrorMessage.FORBIDDEN
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(
            final Exception ex,
            final WebRequest request) {

        log.error("Unexpected generic error caught at the API endpoint: ", ex);
        return APIMessage.ErrorMessage.GENERIC
                .createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(APIMessageException.class)
    public ResponseEntity<Object> handleAPIMessageException(
            final APIMessageException ex,
            final WebRequest request) {

        final HttpStatus httpStatus = ex.getHttpStatus();
        return new ResponseEntity<>(
                ex.getAPIMessages(),
                Utils.createJsonContentHeader(),
                httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Object> handleFieldValidationException(
            final FieldValidationException ex,
            final WebRequest request) {

        return new ResponseEntity<>(
                Arrays.asList(ex.apiMessage),
                Utils.createJsonContentHeader(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Object> handleUnsupportedOperationException(
            final UnsupportedOperationException ex,
            final WebRequest request) {

        return new ResponseEntity<>(
                Arrays.asList(APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of(ex)),
                Utils.createJsonContentHeader(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Object> handleCompletionException(
            final CompletionException ex,
            final WebRequest request) {

        final Throwable cause = ex.getCause();
        if (cause instanceof APIMessageException) {
            return handleAPIMessageException((APIMessageException) cause, request);
        } else if (cause instanceof APIConstraintViolationException) {
            return handleIllegalAPIArgumentException((APIConstraintViolationException) cause, request);
        } else if (cause instanceof ResourceNotFoundException) {
            return APIMessage.ErrorMessage.RESOURCE_NOT_FOUND.createErrorResponse(cause.getMessage());
        } else if (cause instanceof RuntimeException) {
            return APIMessage.ErrorMessage.UNEXPECTED.createErrorResponse(cause.getMessage());
        }

        return APIMessage.ErrorMessage.GENERIC.createErrorResponse(cause.getMessage());

    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Object> handleClientAbortException(
            final ClientAbortException ex,
            final WebRequest request) {

        log.warn("Client aborted: {}", ex.getMessage());

        return null;
    }

}
