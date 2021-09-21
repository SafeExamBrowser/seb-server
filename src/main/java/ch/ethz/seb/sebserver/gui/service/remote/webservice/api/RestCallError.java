/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class RestCallError extends RuntimeException implements APIMessageError {

    private static final long serialVersionUID = -5201349295667957490L;

    final List<APIMessage> errors;

    public RestCallError(final String message, final Throwable cause) {
        super(message, cause);
        this.errors = new ArrayList<>();
    }

    public RestCallError(final String message, final Collection<APIMessage> apiErrors) {
        super(message);
        this.errors = Utils.immutableListOf(apiErrors);
    }

    public RestCallError(final String message) {
        super(message);
        this.errors = new ArrayList<>();
    }

    @Override
    public List<APIMessage> getAPIMessages() {
        return this.errors;
    }

    public boolean hasErrorMessages() {
        return !this.errors.isEmpty();
    }

    public boolean isFieldValidationError() {
        return this.errors
                .stream()
                .anyMatch(APIMessage.ErrorMessage.FIELD_VALIDATION::isOf);
    }

    public boolean isUnexpectedError() {
        return this.errors
                .stream()
                .anyMatch(error -> Integer.valueOf(error.messageCode) < 1200);
    }

    @Override
    public String toString() {
        return "RestCallError [errors=" + this.errors + "]";
    }
}
