/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.seb.sebserver.gbl.model.APIMessage;

public class RestCallError extends RuntimeException {

    private static final long serialVersionUID = -5201349295667957490L;

    final List<APIMessage> errors = new ArrayList<>();

    public RestCallError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RestCallError(final String message) {
        super(message);
    }

    public List<APIMessage> getErrorMessages() {
        return this.errors;
    }

    public boolean hasErrorMessages() {
        return !this.errors.isEmpty();
    }

}
