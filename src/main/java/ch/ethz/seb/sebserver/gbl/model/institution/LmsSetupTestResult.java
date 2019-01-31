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
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class LmsSetupTestResult {

    public static final String ATTR_OK_STATUS = "okStatus";
    public static final String ATTR_MISSING_ATTRIBUTE = "missingLMSSetupAttribute";
    public static final String ATTR_ERROR_TOKEN_REQUEST = "tokenRequestError";
    public static final String ATTR_ERROR_QUIZ_REQUEST = "quizRequestError";

    @JsonProperty(ATTR_OK_STATUS)
    @NotNull
    public final Boolean okStatus;

    @JsonProperty(ATTR_MISSING_ATTRIBUTE)
    public final Set<String> missingLMSSetupAttribute;

    @JsonProperty(ATTR_MISSING_ATTRIBUTE)
    public final String tokenRequestError;

    @JsonProperty(ATTR_MISSING_ATTRIBUTE)
    public final String quizRequestError;

    public LmsSetupTestResult(
            @JsonProperty(value = ATTR_OK_STATUS, required = true) final Boolean ok,
            @JsonProperty(ATTR_MISSING_ATTRIBUTE) final Collection<String> missingLMSSetupAttribute,
            @JsonProperty(ATTR_MISSING_ATTRIBUTE) final String tokenRequestError,
            @JsonProperty(ATTR_MISSING_ATTRIBUTE) final String quizRequestError) {

        this.okStatus = ok;
        // TODO
        this.missingLMSSetupAttribute = Utils.immutableSetOf(missingLMSSetupAttribute);
        this.tokenRequestError = tokenRequestError;
        this.quizRequestError = quizRequestError;
    }

    public Boolean getOkStatus() {
        return this.okStatus;
    }

    public Set<String> getMissingLMSSetupAttribute() {
        return this.missingLMSSetupAttribute;
    }

    public String getTokenRequestError() {
        return this.tokenRequestError;
    }

    public String getQuizRequestError() {
        return this.quizRequestError;
    }

    @Override
    public String toString() {
        return "LmsSetupTestResult [okStatus=" + this.okStatus + ", missingLMSSetupAttribute="
                + this.missingLMSSetupAttribute
                + ", tokenRequestError=" + this.tokenRequestError + ", quizRequestError=" + this.quizRequestError + "]";
    }

    public static final LmsSetupTestResult ofOkay() {
        return new LmsSetupTestResult(true, null, null, null);
    }

    public static final LmsSetupTestResult ofMissingAttributes(final Collection<String> attrs) {
        return new LmsSetupTestResult(false, attrs, null, null);
    }

    public static final LmsSetupTestResult ofMissingAttributes(final String... attrs) {
        if (attrs == null) {
            return new LmsSetupTestResult(false, null, null, null);
        }
        return new LmsSetupTestResult(false, Arrays.asList(attrs), null, null);
    }

    public static final LmsSetupTestResult ofTokenRequestError(final String message) {
        return new LmsSetupTestResult(false, null, message, null);
    }

    public static final LmsSetupTestResult ofQuizRequestError(final String message) {
        return new LmsSetupTestResult(false, null, null, message);
    }

}
