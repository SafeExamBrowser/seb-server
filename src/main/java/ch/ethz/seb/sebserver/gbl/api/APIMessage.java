/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** This class defines API error messages that are created and responded on error and/or exceptional
 * cases within the web-service. */
public class APIMessage implements Serializable {

    private static final long serialVersionUID = -6858683658311637361L;

    /** An enumeration of error messages defining the error code, the HTTP status for the response
     * and a short system message. This error message definition can be used to
     * generate APIMessages for default errors. */
    public enum ErrorMessage {
        /** For every unknown or unspecific internal error */
        GENERIC("0", HttpStatus.INTERNAL_SERVER_ERROR, "Generic error message"),
        UNAUTHORIZED("1000", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),
        FORBIDDEN("1001", HttpStatus.FORBIDDEN, "FORBIDDEN"),
        RESOURCE_NOT_FOUND("1002", HttpStatus.NOT_FOUND, "resource not found"),
        ILLEGAL_API_ARGUMENT("1010", HttpStatus.BAD_REQUEST, "Illegal API request argument"),
        UNEXPECTED("1100", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal server-side error"),

        FIELD_VALIDATION("1200", HttpStatus.BAD_REQUEST, "Field validation error"),
        INTEGRITY_VALIDATION("1201", HttpStatus.BAD_REQUEST, "Action would lied to an integrity violation"),
        PASSWORD_MISMATCH("1300", HttpStatus.BAD_REQUEST, "new password do not match confirmed password"),
        MISSING_PASSWORD("1301", HttpStatus.BAD_REQUEST, "Missing Password"),

        EXAM_CONSISTENCY_VALIDATION_SUPPORTER("1400", HttpStatus.OK, "No Exam Supporter defined for the Exam"),
        EXAM_CONSISTENCY_VALIDATION_CONFIG("1401", HttpStatus.OK, "No SEB Exam Configuration defined for the Exam"),
        EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION("1402", HttpStatus.OK,
                "SEB restriction API available but Exam not restricted on LMS side yet"),
        EXAM_CONSISTENCY_VALIDATION_INDICATOR("1403", HttpStatus.OK, "No Indicator defined for the Exam"),
        EXAM_CONSISTENCY_VALIDATION_LMS_CONNECTION("1404", HttpStatus.OK, "No Connection To LMS"),
        EXAM_CONSISTENCY_VALIDATION_INVALID_ID_REFERENCE("1405", HttpStatus.OK,
                "There seems to be an invalid exam - course identifier reference. The course cannot be found"),

        EXTERNAL_SERVICE_BINDING_ERROR("1500", HttpStatus.BAD_REQUEST, "External binding error"),

        EXAM_IMPORT_ERROR_AUTO_SETUP("1600", HttpStatus.PARTIAL_CONTENT,
                "Exam successfully imported but some additional initialization failed"),
        EXAM_IMPORT_ERROR_AUTO_INDICATOR("1601", HttpStatus.PARTIAL_CONTENT,
                "Failed to automatically create pre-defined indicators for the exam"),
        EXAM_IMPORT_ERROR_AUTO_ATTRIBUTES("1602", HttpStatus.PARTIAL_CONTENT,
                "Failed to automatically create pre-defined attributes for the exam"),
        EXAM_IMPORT_ERROR_AUTO_RESTRICTION("1603", HttpStatus.PARTIAL_CONTENT,
                "Failed to automatically apply SEB restriction for the exam to the involved LMS"),
        EXAM_IMPORT_ERROR_AUTO_CONFIG("1610", HttpStatus.PARTIAL_CONTENT,
                "Failed to automatically create and link exam configuration from the exam template to the exam"),
        EXAM_IMPORT_ERROR_AUTO_CONFIG_LINKING("1611", HttpStatus.PARTIAL_CONTENT,
                "Failed to automatically link auto-generated exam configuration to the exam");

        public final String messageCode;
        public final HttpStatus httpStatus;
        public final String systemMessage;

        ErrorMessage(
                final String messageCode,
                final HttpStatus httpStatus,
                final String systemMessage) {

            this.messageCode = messageCode;
            this.httpStatus = httpStatus;
            this.systemMessage = systemMessage;
        }

        public boolean isOf(final APIMessage message) {
            return message != null && this.messageCode.equals(message.messageCode);
        }

        public APIMessage of() {
            return new APIMessage(this.messageCode, this.systemMessage);
        }

        public APIMessage of(final String detail) {
            return new APIMessage(this.messageCode, this.systemMessage, detail);
        }

        public APIMessage of(final String detail, final String... attributes) {
            return new APIMessage(
                    this.messageCode,
                    this.systemMessage,
                    detail,
                    Utils.asImmutableList(attributes));
        }

        public APIMessage of(final Exception error) {
            return new APIMessage(this.messageCode, this.systemMessage, error.getMessage());
        }

        public APIMessage of(final Exception error, final String... attributes) {
            return new APIMessage(this.messageCode, this.systemMessage, error.getMessage(), Arrays.asList(attributes));
        }

        public ResponseEntity<List<APIMessage>> createErrorResponse() {
            final APIMessage message = of();
            return new ResponseEntity<>(
                    Arrays.asList(message),
                    Utils.createJsonContentHeader(),
                    this.httpStatus);
        }

        public ResponseEntity<Object> createErrorResponse(final String details, final String... attributes) {
            final APIMessage message = of(details, attributes);
            return new ResponseEntity<>(
                    Arrays.asList(message),
                    Utils.createJsonContentHeader(),
                    this.httpStatus);
        }
    }

    /** A specific message code that can be used to identify the type of message */
    @JsonProperty("messageCode")
    public final String messageCode;

    /** A short system message that describes the cause */
    @JsonProperty("systemMessage")
    public final String systemMessage;

    /** Message details */
    @JsonProperty("details")
    public final String details;

    /** A list of additional attributes */
    @JsonProperty("attributes")
    public final List<String> attributes;

    @JsonCreator
    public APIMessage(
            @JsonProperty("messageCode") final String messageCode,
            @JsonProperty("systemMessage") final String systemMessage,
            @JsonProperty("details") final String details,
            @JsonProperty("attributes") final List<String> attributes) {

        this.messageCode = messageCode;
        this.systemMessage = systemMessage;
        this.details = details;
        this.attributes = (attributes != null)
                ? Collections.unmodifiableList(attributes)
                : Collections.emptyList();
    }

    public APIMessage(final String messageCode, final String systemMessage, final String details) {
        this(messageCode, systemMessage, details, null);
    }

    public APIMessage(final String messageCode, final String systemMessage) {
        this(messageCode, systemMessage, null, null);
    }

    public String getMessageCode() {
        return this.messageCode;
    }

    public String getSystemMessage() {
        return this.systemMessage;
    }

    public String getDetails() {
        return this.details;
    }

    public List<String> getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("APIMessage [messageCode=");
        builder.append(this.messageCode);
        builder.append(", systemMessage=");
        builder.append(this.systemMessage);
        builder.append(", details=");
        builder.append(this.details);
        builder.append(", attributes=");
        builder.append(this.attributes);
        builder.append("]");
        return builder.toString();
    }

    /** Use this as a conversion from a given FieldError of Spring to a APIMessage
     * of type field validation.
     *
     * @param error FieldError instance
     * @return converted APIMessage of type field validation */
    public static APIMessage fieldValidationError(final FieldError error) {
        final String[] args = StringUtils.split(error.getDefaultMessage(), ":");
        return ErrorMessage.FIELD_VALIDATION.of(error.toString(), args);
    }

    public static APIMessage fieldValidationError(final String fieldName, final String defaultMessage) {
        final String[] args = StringUtils.split(defaultMessage, ":");
        return ErrorMessage.FIELD_VALIDATION.of(fieldName, args);
    }

    public static String toHTML(final String errorMessage, final Collection<APIMessage> messages) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<b>Failure:</b>").append("<br/><br/>").append(errorMessage).append("<br/><br/>");
        builder.append("<b>Detail Messages:</b><br/><br/>");
        return buildHTML(messages, builder);
    }

    public static String toHTML(final Collection<APIMessage> messages) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<b>Messages:</b><br/><br/>");
        return buildHTML(messages, builder);
    }

    /** This exception can be internal used to wrap a created APIMessage
     * within an Exception and throw. The Exception will be caught a the
     * APIExceptionHandler endpoint. The APIMessage will be extracted
     * and send as response. */
    public static class APIMessageException extends RuntimeException implements APIMessageError {

        private static final long serialVersionUID = 1453431210820677296L;

        private final Collection<APIMessage> apiMessages;

        public APIMessageException(final Collection<APIMessage> apiMessages) {
            super();
            this.apiMessages = apiMessages;
        }

        public APIMessageException(final APIMessage apiMessage) {
            super();
            this.apiMessages = Arrays.asList(apiMessage);
        }

        public APIMessageException(final ErrorMessage errorMessage) {
            super(errorMessage.systemMessage);
            this.apiMessages = Arrays.asList(errorMessage.of());
        }

        public APIMessageException(final ErrorMessage errorMessage, final String detail, final String... attributes) {
            super(errorMessage.systemMessage);
            this.apiMessages = Arrays.asList(errorMessage.of(detail, attributes));
        }

        public APIMessageException(final ErrorMessage errorMessage, final Exception errorCause) {
            super(errorMessage.systemMessage);
            this.apiMessages = Arrays.asList(errorMessage.of(errorCause));
        }

        public APIMessageException(
                final ErrorMessage errorMessage,
                final Exception errorCause,
                final String... attributes) {

            super(errorMessage.systemMessage);
            this.apiMessages = Arrays.asList(errorMessage.of(errorCause, attributes));
        }

        @Override
        public Collection<APIMessage> getAPIMessages() {
            return this.apiMessages;
        }
    }

    /** This is used as a field validation exception that creates a APIMessage of filed
     * validation. The Exception will be caught a the
     * APIExceptionHandler endpoint. The APIMessage will be extracted
     * and send as response. */
    public static class FieldValidationException extends RuntimeException {

        private static final long serialVersionUID = 3324566460573096815L;

        public final APIMessage apiMessage;

        public FieldValidationException(final String fieldName, final String defaultMessage) {
            super(defaultMessage);
            this.apiMessage = APIMessage.fieldValidationError(fieldName, defaultMessage);
        }
    }

    private static String buildHTML(final Collection<APIMessage> messages, final StringBuilder builder) {
        messages.forEach(message -> builder
                .append("&nbsp;&nbsp;code&nbsp;:&nbsp;")
                .append(message.messageCode)
                .append("<br/>")
                .append("&nbsp;&nbsp;system message&nbsp;:&nbsp;")
                .append(HtmlUtils.htmlEscape(message.systemMessage))
                .append("<br/>")
                .append("&nbsp;&nbsp;details&nbsp;:&nbsp;")
                .append((message.details != null)
                        ? HtmlUtils.htmlEscape(StringUtils.abbreviate(message.details, 100))
                        : Constants.EMPTY_NOTE)
                .append("<br/><br/>"));
        return builder.toString();
    }

    public static boolean checkError(final Exception error, final ErrorMessage errorMessage) {
        if (!(error instanceof APIMessageError)) {
            return false;
        }

        final APIMessageError _error = (APIMessageError) error;
        return _error.getAPIMessages()
                .stream()
                .filter(msg -> errorMessage.messageCode.equals(msg.messageCode))
                .findFirst()
                .isPresent();
    }

}
