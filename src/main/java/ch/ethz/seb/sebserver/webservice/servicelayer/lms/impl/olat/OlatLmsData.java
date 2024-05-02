/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public final class OlatLmsData {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public final class AssessmentData {
        /*
         * OLAT API example:
         * {
         * "begin": 1624420800000,
         * "end": 1624658400000,
         * "description": "",
         * "key": 6356992,
         * “repositoryEntryKey”: 462324,
         * "name": "SEB test",
         * "leadTime": 15,
         * "followupTime", 5
         * }
         */
        public long key;
        public long repositoryEntryKey;
        public String name;
        public String description;
        public Long begin;
        public Long end;
        public long leadTime;
        public long followupTime;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class UserData {
        /*
         * OLAT API example:
         * {
         * "firstName": "OpenOLAT",
         * "key": 360448,
         * "lastName": "Administrator",
         * "login": "administrator",
         * "email": "admin@example.org"
         * }
         */
        public long key;
        public String firstName;
        public String lastName;
        public String login;
        public String email;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RestrictionData {
        /*
         * OLAT API example:
         * {
         * "browserExamKeys": [ "1" ],
         * "configKeys": null,
         * "quitLink": "<the quit link from Exam Configuration>",
         * "quitSecret": "<the quit password from Exam Configuration>"
         * "key": 8028160
         * }
         */
        public long key;
        public List<String> browserExamKeys;
        public List<String> configKeys;
        public String quitLink;
        public String quitSecret;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RestrictionDataPost {
        /*
         * OLAT API example:
         * {
         * "configKeys": ["a", "b"],
         * "browserExamKeys": ["1", "2"],
         * "quitLink": "<the quit link from Exam Configuration>",
         * "quitSecret": "<the quit password from Exam Configuration>"
         * }
         */
        public List<String> browserExamKeys;
        public List<String> configKeys;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String quitLink;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String quitSecret;
    }

}
