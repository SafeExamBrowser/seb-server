/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class LmsSetupTestResult {

    public static final String ATTR_ERROR_TYPE = "errorType";
    public static final String ATTR_ERROR_MESSAGE = "errorMessage";
    public static final String ATTR_ERRORS = "errors";
    public static final String ATTR_MISSING_ATTRIBUTE = "missingLMSSetupAttribute";

    public enum ErrorType {
        MISSING_ATTRIBUTE,
        TOKEN_REQUEST,
        QUIZ_ACCESS_API_REQUEST,
        QUIZ_RESTRICTION_API_REQUEST
    }

    @JsonProperty(ATTR_ERRORS)
    public final Collection<Error> errors;
    @JsonProperty(ATTR_MISSING_ATTRIBUTE)
    public final Collection<APIMessage> missingLMSSetupAttribute;

    @JsonCreator
    public LmsSetupTestResult(
            @JsonProperty(ATTR_ERRORS) final Collection<Error> errors,
            @JsonProperty(ATTR_MISSING_ATTRIBUTE) final Collection<APIMessage> missingLMSSetupAttribute) {

        this.errors = Utils.immutableCollectionOf(errors);
        this.missingLMSSetupAttribute = Utils.immutableCollectionOf(missingLMSSetupAttribute);
    }

    protected LmsSetupTestResult() {
        this(
                Collections.emptyList(),
                Collections.emptyList());
    }

    protected LmsSetupTestResult(final Error error) {
        this(
                Utils.immutableCollectionOf(Arrays.asList(error)),
                Collections.emptyList());
    }

    protected LmsSetupTestResult(final Error error, final Collection<APIMessage> missingLMSSetupAttribute) {
        this(
                Utils.immutableCollectionOf(Arrays.asList(error)),
                Utils.immutableCollectionOf(missingLMSSetupAttribute));
    }

    @JsonIgnore
    public boolean isOk() {
        return this.errors == null || this.errors.isEmpty();
    }

    @JsonIgnore
    public boolean isQuizAccessOk() {
        return isOk() || hasError(ErrorType.QUIZ_RESTRICTION_API_REQUEST);
    }

    @JsonIgnore
    public boolean hasError(final ErrorType type) {
        return this.errors
                .stream()
                .anyMatch(error -> error.errorType == type);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LmsSetupTestResult [errors=");
        builder.append(this.errors);
        builder.append(", missingLMSSetupAttribute=");
        builder.append(this.missingLMSSetupAttribute);
        builder.append("]");
        return builder.toString();
    }

    public static LmsSetupTestResult ofOkay() {
        return new LmsSetupTestResult();
    }

    public static LmsSetupTestResult ofMissingAttributes(final Collection<APIMessage> attrs) {
        return new LmsSetupTestResult(new Error(ErrorType.MISSING_ATTRIBUTE, "missing attribute(s)"), attrs);
    }

    public static LmsSetupTestResult ofMissingAttributes(final APIMessage... attrs) {
        return new LmsSetupTestResult(new Error(ErrorType.MISSING_ATTRIBUTE, "missing attribute(s)"),
                Arrays.asList(attrs));
    }

    public static LmsSetupTestResult ofTokenRequestError(final String message) {
        return new LmsSetupTestResult(new Error(ErrorType.TOKEN_REQUEST, message));
    }

    public static LmsSetupTestResult ofQuizAccessAPIError(final String message) {
        return new LmsSetupTestResult(new Error(ErrorType.QUIZ_ACCESS_API_REQUEST, message));
    }

    public static LmsSetupTestResult ofQuizRestrictionAPIError(final String message) {
        return new LmsSetupTestResult(new Error(ErrorType.QUIZ_RESTRICTION_API_REQUEST, message));
    }

    public final static class Error {

        @JsonProperty(ATTR_ERROR_TYPE)
        public final ErrorType errorType;
        @JsonProperty(ATTR_ERROR_MESSAGE)
        public final String message;

        @JsonCreator
        public Error(
                @JsonProperty(ATTR_ERROR_TYPE) final ErrorType errorType,
                @JsonProperty(ATTR_ERROR_MESSAGE) final String message) {

            this.errorType = errorType;
            this.message = message;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Error [errorType=");
            builder.append(this.errorType);
            builder.append(", message=");
            builder.append(this.message);
            builder.append("]");
            return builder.toString();
        }

    }

}
