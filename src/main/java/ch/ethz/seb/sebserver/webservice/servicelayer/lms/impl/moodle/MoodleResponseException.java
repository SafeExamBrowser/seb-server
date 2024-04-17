/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

public class MoodleResponseException extends RuntimeException {

    public final String moodleResponse;

    public MoodleResponseException(final String message, final String moodleResponse, final Throwable cause) {
        super(message, cause);
        this.moodleResponse = moodleResponse;
    }

    public MoodleResponseException(final String message, final String moodleResponse) {
        super(message);
        this.moodleResponse = moodleResponse;
    }


}
