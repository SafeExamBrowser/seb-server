/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** Global Constants used in SEB Server web-service as well as in web-gui component */
public interface Constants {

    /** Calendar using the UTC time-zone */
    Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /** Date-Time formatter without milliseconds using UTC time-zone. Pattern is yyyy-MM-dd HH:mm:ss */
    DateTimeFormatter DATE_TIME_PATTERN_UTC_NO_MILLIS = DateTimeFormat
            .forPattern("yyyy-MM-dd HH:mm:ss")
            .withZoneUTC();

    /** Date-Time formatter with milliseconds using UTC time-zone. Pattern is yyyy-MM-dd HH:mm:ss.S */
    DateTimeFormatter DATE_TIME_PATTERN_UTC_MILLIS = DateTimeFormat
            .forPattern("yyyy-MM-dd HH:mm:ss.S")
            .withZoneUTC();

}
