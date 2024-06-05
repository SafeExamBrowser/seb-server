/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.Map;

import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface MoodleAPIRestTemplate {

    String URI_VAR_USER_NAME = "username";
    String URI_VAR_PASSWORD = "pwd";
    String URI_VAR_SERVICE = "service";

    String MOODLE_DEFAULT_TOKEN_REQUEST_PATH =
            "/login/token.php?username={" + URI_VAR_USER_NAME +
                    "}&password={" + URI_VAR_PASSWORD + "}&service={" + URI_VAR_SERVICE + "}";

    String MOODLE_DEFAULT_REST_API_PATH = "/webservice/rest/server.php";
    String REST_REQUEST_TOKEN_NAME = "wstoken";
    String REST_REQUEST_FUNCTION_NAME = "wsfunction";
    String REST_REQUEST_FORMAT_NAME = "moodlewsrestformat";

    String getService();

    CharSequence getAccessToken();

    void testAPIConnection(String... functions);

    String callMoodleAPIFunction(String functionName);

    String postToMoodleAPIFunction(
            String functionName,
            MultiValueMap<String, String> queryParams,
            Map<String, Map<String, String>> queryAttributes);

    String callMoodleAPIFunction(
            String functionName,
            MultiValueMap<String, String> queryAttributes);

    String callMoodleAPIFunction(
            String functionName,
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> queryAttributes);

    String uploadMultiPart(
            String uploadEndpoint,
            MultiValueMap<String, Object> multiPartAttributes);


    /** This maps a Moodle warning JSON object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Warning {
        final String item;
        final String itemid;
        final String warningcode;
        final String message;

        @JsonCreator
        public Warning(
                @JsonProperty(value = "item") final String item,
                @JsonProperty(value = "itemid") final String itemid,
                @JsonProperty(value = "warningcode") final String warningcode,
                @JsonProperty(value = "message") final String message) {

            this.item = item;
            this.itemid = itemid;
            this.warningcode = warningcode;
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Warning [item=");
            builder.append(this.item);
            builder.append(", itemid=");
            builder.append(this.itemid);
            builder.append(", warningcode=");
            builder.append(this.warningcode);
            builder.append(", message=");
            builder.append(this.message);
            builder.append("]");
            return builder.toString();
        }
    }
}
