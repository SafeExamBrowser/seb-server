/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl;

import java.util.Collection;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;

/** Global Constants used in SEB Server web-service as well as in web-gui component */
public final class Constants {

    public static final String FILE_EXT_CSV = ".csv";

    public static final String DEFAULT_LANG_CODE = "en";
    public static final String DEFAULT_TIME_ZONE_CODE = "UTC";
    public static final String TOOLTIP_TEXT_KEY_SUFFIX = ".tooltip";

    public static final int SEB_FILE_HEADER_SIZE = 4;
    public static final int JN_CRYPTOR_ITERATIONS = 10000;
    public static final int JN_CRYPTOR_VERSION_HEADER_SIZE = 1;

    public static final String TRUE_STRING = Boolean.TRUE.toString();
    public static final String FALSE_STRING = Boolean.FALSE.toString();

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;
    public static final long HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;
    public static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

    public static final int DAY_IN_MIN = 60 * 24;

    public static final Character ENTER = '\r';
    public static final Character CARRIAGE_RETURN = '\n';
    public static final Character CURLY_BRACE_OPEN = '{';
    public static final Character CURLY_BRACE_CLOSE = '}';
    public static final Character SQUARE_BRACE_OPEN = '[';
    public static final Character SQUARE_BRACE_CLOSE = ']';
    public static final Character ANGLE_BRACE_OPEN = '<';
    public static final Character ANGLE_BRACE_CLOSE = '>';
    public static final Character COLON = ':';
    public static final Character SEMICOLON = ';';
    public static final Character SPACE = ' ';
    public static final Character PERCENTAGE = '%';
    public static final Character SLASH = '/';
    public static final Character BACKSLASH = '\\';
    public static final Character QUOTE = '\'';
    public static final Character QUERY = '?';
    public static final Character DOUBLE_QUOTE = '"';
    public static final Character COMMA = ',';
    public static final Character PIPE = '|';
    public static final Character UNDERLINE = '_';
    public static final Character AMPERSAND = '&';
    public static final Character EQUALITY_SIGN = '=';
    public static final Character LIST_SEPARATOR_CHAR = COMMA;
    public static final Character COMPLEX_VALUE_SEPARATOR = COLON;
    public static final Character HASH_TAG = '#';

    public static final String NULL = "null";
    public static final String PERCENTAGE_STRING = Constants.PERCENTAGE.toString();
    public static final String LIST_SEPARATOR = COMMA.toString();
    public static final String EMBEDDED_LIST_SEPARATOR = PIPE.toString();
    public static final String NO_NAME = "NONE";
    public static final String EMPTY_NOTE = "--";
    public static final String FORM_URL_ENCODED_SEPARATOR = AMPERSAND.toString();
    public static final String FORM_URL_ENCODED_NAME_VALUE_SEPARATOR = EQUALITY_SIGN.toString();
    public static final String URL_PORT_SEPARATOR = COLON.toString();
    public static final String URL_ADDRESS_SEPARATOR = COLON.toString() + SLASH.toString() + SLASH.toString();
    public static final String URL_PATH_SEPARATOR = SLASH.toString();
    public static final String HASH_TAG_STRING = HASH_TAG.toString();

    public static final String DYN_HTML_ATTR_OPEN = "%%_";
    public static final String DYN_HTML_ATTR_CLOSE = "_%%";

    public static final String DEFAULT_DATE_TIME_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String TIME_ZONE_OFFSET_TAIL_FORMAT = "|ZZ";

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public static final DateTimeFormatter STANDARD_DATE_TIME_MILLIS_FORMATTER = DateTimeFormat
            .forPattern(DEFAULT_DATE_TIME_MILLIS_FORMAT)
            .withZoneUTC();
    public static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER = DateTimeFormat
            .forPattern(DEFAULT_DATE_TIME_FORMAT)
            .withZoneUTC();
    public static final DateTimeFormatter STANDARD_DATE_FORMATTER = DateTimeFormat
            .forPattern(DEFAULT_DATE_FORMAT)
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

    public static final String XML_PLIST_NAME = "plist";
    public static final String XML_PLIST_DICT_NAME = "dict";
    public static final String XML_PLIST_ARRAY_NAME = "array";
    public static final String XML_PLIST_KEY_NAME = "key";
    public static final String XML_PLIST_BOOLEAN_TRUE = "true";
    public static final String XML_PLIST_BOOLEAN_FALSE = "false";
    public static final String XML_PLIST_STRING = "string";
    public static final String XML_PLIST_DATA = "data";
    public static final String XML_PLIST_INTEGER = "integer";

    public static final String OAUTH2_GRANT_TYPE_PASSWORD = "password";
    public static final String OAUTH2_CLIENT_SECRET = "client_secret";
    public static final String OAUTH2_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String OAUTH2_SCOPE_READ = "read";
    public static final String OAUTH2_SCOPE_WRITE = "write";

    public static final int RWT_MOUSE_BUTTON_1 = 1;
    public static final int RWT_MOUSE_BUTTON_2 = 2;
    public static final int RWT_MOUSE_BUTTON_3 = 3;

    public static final int GZIP_HEADER_LENGTH = 4;
    public static final int GZIP_ID1 = 0x1F;
    public static final int GZIP_ID2 = 0x8B;
    public static final int GZIP_CM = 8;

    public static final String SHA_256 = "SHA-256";
    public static final String X_509 = "X.509";
    public static final String PKCS_12 = "pkcs12";
    public static final String SHA_1 = "SHA-1";
    public static final String AES = "AES";
    public static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";

    public static final RGB WHITE_RGB = new RGB(255, 255, 255);
    public static final RGB BLACK_RGB = new RGB(0, 0, 0);
    public static final RGBA GREY_DISABLED = new RGBA(150, 150, 150, 50);

    public static final String IMPORTED_PASSWORD_MARKER = "_IMPORTED_PASSWORD";

    public static final TypeReference<Collection<APIMessage>> TYPE_REFERENCE_API_MESSAGE =
            new TypeReferenceAPIMessage();
    public static final ParameterizedTypeReference<Collection<Privilege>> TYPE_REFERENCE_PRIVILEGES =
            new TypeReferencePrivileges();

    public static final class TypeReferenceAPIMessage extends TypeReference<Collection<APIMessage>> {
    }

    public static final class TypeReferencePrivileges extends ParameterizedTypeReference<Collection<Privilege>> {
    }

}
