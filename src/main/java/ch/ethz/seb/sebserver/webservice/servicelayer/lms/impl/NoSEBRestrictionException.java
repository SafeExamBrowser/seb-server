/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

public class NoSEBRestrictionException extends RuntimeException {

    private static final long serialVersionUID = -6444577025412136884L;

    public NoSEBRestrictionException() {
    }

    public NoSEBRestrictionException(final Throwable cause) {
        super(cause);
    }

}
