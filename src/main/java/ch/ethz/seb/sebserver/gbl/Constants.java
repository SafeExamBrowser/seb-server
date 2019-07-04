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

    public static final int JN_CRYPTOR_ITERATIONS = 10000;

    public static final String TRUE_STRING = Boolean.TRUE.toString();
    public static final String FALSE_STRING = Boolean.FALSE.toString();

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;
    public static final long HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;
    public static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

    public static final Character LIST_SEPARATOR_CHAR = ',';
    public static final String LIST_SEPARATOR = ",";
    public static final String EMBEDDED_LIST_SEPARATOR = "|";
    public static final String EMPTY_NOTE = "--";
    public static final String FORM_URL_ENCODED_SEPARATOR = "&";
    public static final String FORM_URL_ENCODED_NAME_VALUE_SEPARATOR = "=";

    public static final String PERCENTAGE = "%";

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DEFAULT_DISPLAY_DATE_FORMAT = "MM-dd-yyy HH:mm";
    public static final String TIME_ZONE_OFFSET_TAIL_FORMAT = "|ZZ";

    public static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER = DateTimeFormat
            .forPattern(DEFAULT_DATE_TIME_FORMAT)
            .withZoneUTC();

    public static final String XML_VERSION_HEADER =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String XML_DOCTYPE_HEADER =
            "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
    public static final String XML_PLIST_START_V1 =
            "<plist version=\"1.0\">";
    public static final String XML_PLIST_END =
            "</plist>";
    public static final String XML_DICT_START =
            "<dict>";
    public static final String XML_DICT_END =
            "</dict>";

}
