/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

public class ExamNotRunningException extends RuntimeException {

    private static final long serialVersionUID = -2931666431463176875L;

    public ExamNotRunningException() {
        super();
    }

    public ExamNotRunningException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ExamNotRunningException(final String message) {
        super(message);
    }

}
