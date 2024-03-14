/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

/** This exception shall be used on additional validations of API attribute constraints.
 * Throwing an APIConstraintViolationException will lead to a HTTP 400 Bad Request response. */
public class APIConstraintViolationException extends RuntimeException {

    private static final long serialVersionUID = 3732727447520974727L;

    public APIConstraintViolationException() {
        super();
    }

    public APIConstraintViolationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public APIConstraintViolationException(final String message) {
        super(message);
    }

}
