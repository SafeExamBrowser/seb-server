/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

public class TooManyRequests extends RuntimeException {

    private static final long serialVersionUID = 3303246002774619224L;

    public static enum Code {
        INCOMMING,
        REGISTRATION
    }

    public final Code code;

    public TooManyRequests() {
        super("TooManyRequests");
        this.code = Code.INCOMMING;
    }

    public TooManyRequests(final Code code) {
        super("TooManyRequests");
        this.code = code;
    }

}
