/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

public class IllegalUserSessionStateException extends RuntimeException {

    private static final long serialVersionUID = 7856771073315779648L;

    protected IllegalUserSessionStateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    protected IllegalUserSessionStateException(final String message) {
        super(message);
    }

}
