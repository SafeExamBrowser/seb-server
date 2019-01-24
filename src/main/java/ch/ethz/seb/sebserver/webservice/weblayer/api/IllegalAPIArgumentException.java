/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

public class IllegalAPIArgumentException extends RuntimeException {

    private static final long serialVersionUID = 3732727447520974727L;

    public IllegalAPIArgumentException() {
        super();
    }

    public IllegalAPIArgumentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IllegalAPIArgumentException(final String message) {
        super(message);
    }

}
