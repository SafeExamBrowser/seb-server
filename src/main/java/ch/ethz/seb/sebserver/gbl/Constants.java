/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** Global Constants used in SEB Server web-service as well as in web-gui component */
public final class Constants {

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;
    public static final long HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;
    public static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

    public static final Character LIST_SEPARATOR_CHAR = ',';
    public static final String LIST_SEPARATOR = ",";
    public static final String EMPTY_NOTE = "--";
    public static final String FORM_URL_ENCODED_SEPARATOR = "&";
    public static final String FORM_URL_ENCODED_NAME_VALUE_SEPARATOR = "=";

    public static final String DEFAULT_DISPLAY_DATE_FORMAT = "MM-dd-yyy HH:mm";
    public static final String TIME_ZONE_OFFSET_TAIL_FORMAT = "|ZZ";

    /** Date-Time formatter without milliseconds using UTC time-zone. Pattern is yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DATE_TIME_PATTERN_UTC_NO_MILLIS = DateTimeFormat
            .forPattern("yyyy-MM-dd HH:mm:ss")
            .withZoneUTC();

    /** Date-Time formatter with milliseconds using UTC time-zone. Pattern is yyyy-MM-dd HH:mm:ss.S */
    public static final DateTimeFormatter DATE_TIME_PATTERN_UTC_MILLIS = DateTimeFormat
            .forPattern("yyyy-MM-dd HH:mm:ss.S")
            .withZoneUTC();

}
