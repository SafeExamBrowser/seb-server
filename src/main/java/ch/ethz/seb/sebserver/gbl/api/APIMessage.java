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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class APIMessage implements Serializable {

    private static final long serialVersionUID = -6858683658311637361L;

    public enum ErrorMessage {
        GENERIC("0", HttpStatus.INTERNAL_SERVER_ERROR, "Generic error message"),
        UNAUTHORIZED("1000", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),
        FORBIDDEN("1001", HttpStatus.FORBIDDEN, "FORBIDDEN"),
        RESOURCE_NOT_FOUND("1002", HttpStatus.NOT_FOUND, "resource not found"),
        ILLEGAL_API_ARGUMENT("1010", HttpStatus.BAD_REQUEST, "Illegal API request argument"),
        UNEXPECTED("1100", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected intenral server-side error"),
        FIELD_VALIDATION("1200", HttpStatus.BAD_REQUEST, "Field validation error"),
        PASSWORD_MISMATCH("1300", HttpStatus.BAD_REQUEST, "new password do not match retyped password")

        ;

        public final String messageCode;
        public final HttpStatus httpStatus;
        public final String systemMessage;

        private ErrorMessage(
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

        public APIMessage of(final Throwable error) {
            return new APIMessage(this.messageCode, this.systemMessage, error.getMessage());
        }

        public ResponseEntity<List<APIMessage>> createErrorResponse() {
            final APIMessage message = of();
            return new ResponseEntity<>(Arrays.asList(message), this.httpStatus);
        }

        public ResponseEntity<Object> createErrorResponse(final String details, final String... attributes) {
            final APIMessage message = of(details, attributes);
            return new ResponseEntity<>(Arrays.asList(message), this.httpStatus);
        }
    }

    @JsonProperty("messageCode")
    public final String messageCode;
    @JsonProperty("systemMessage")
    public final String systemMessage;
    @JsonProperty("details")
    public final String details;
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

    public static final APIMessage fieldValidationError(final FieldError error) {
        final String[] args = StringUtils.split(error.getDefaultMessage(), ":");
        return ErrorMessage.FIELD_VALIDATION.of(error.toString(), args);
    }

    public static APIMessage fieldValidationError(final String fieldName, final String defaultMessage) {
        final String[] args = StringUtils.split(defaultMessage, ":");
        return ErrorMessage.FIELD_VALIDATION.of(fieldName, args);
    }

    public static String toHTML(final Collection<APIMessage> messages) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<b>Messages:</b><br/><br/>");
        messages.stream().forEach(message -> {
            builder
                    .append("&nbsp;&nbsp;code&nbsp;:&nbsp;")
                    .append(message.messageCode)
                    .append("<br/>")
                    .append("&nbsp;&nbsp;system message&nbsp;:&nbsp;")
                    .append(message.systemMessage)
                    .append("<br/>")
                    .append("&nbsp;&nbsp;details&nbsp;:&nbsp;")
                    .append(StringUtils.abbreviate(message.details, 100))
                    .append("<br/><br/>");
        });
        return builder.toString();
    }

    public static class APIMessageException extends RuntimeException {

        private static final long serialVersionUID = 1453431210820677296L;

        private final APIMessage apiMessage;

        public APIMessageException(final APIMessage apiMessage) {
            super();
            this.apiMessage = apiMessage;
        }

        public APIMessageException(final ErrorMessage errorMessage) {
            super();
            this.apiMessage = errorMessage.of();
        }

        public APIMessageException(final ErrorMessage errorMessage, final String detail, final String... attributes) {
            super();
            this.apiMessage = errorMessage.of(detail, attributes);
        }

        public APIMessage getAPIMessage() {
            return this.apiMessage;
        }
    }

    public static class FieldValidationException extends RuntimeException {

        private static final long serialVersionUID = 3324566460573096815L;

        public final APIMessage apiMessage;

        public FieldValidationException(final String fieldName, final String defaultMessage) {
            this.apiMessage = APIMessage.fieldValidationError(fieldName, defaultMessage);
        }
    }

}
