/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.FieldError;

public class APIMessage {

    public enum ErrorMessage {
        UNEXPECTED("1000", "Unexpected intenral server-side error"),
        FIELD_VALIDATION("1500", "Field validation error"),
        PASSWORD_MISSMATCH("2000", "new password do not match retyped password")

        ;

        public final String messageCode;
        public final String systemMessage;

        private ErrorMessage(final String messageCode, final String systemMessage) {
            this.messageCode = messageCode;
            this.systemMessage = systemMessage;
        }

        public APIMessage of() {
            return new APIMessage(this.messageCode, this.systemMessage);
        }

        public APIMessage of(final String detail) {
            return new APIMessage(this.messageCode, this.systemMessage, detail);
        }

        public APIMessage of(final String detail, final String... attributes) {
            return new APIMessage(this.messageCode, this.systemMessage, detail, attributes);
        }

        public APIMessage of(final Throwable error) {
            return new APIMessage(this.messageCode, this.systemMessage, error.getMessage());
        }
    }

    public final String messageCode;
    public final String systemMessage;
    public final String details;
    public final String[] attributes;

    public APIMessage(
            final String messageCode,
            final String systemMessage,
            final String details,
            final String[] attributes) {

        this.messageCode = messageCode;
        this.systemMessage = systemMessage;
        this.details = details;
        this.attributes = attributes;
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

    public String[] getAttributes() {
        return this.attributes;
    }

    public static final APIMessage fieldValidationError(final FieldError error) {
        final String[] args = StringUtils.split(error.getDefaultMessage(), ":");
        return ErrorMessage.FIELD_VALIDATION.of(error.toString(), args);
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

        public APIMessage getAPIMessage() {
            return this.apiMessage;
        }

    }

}
