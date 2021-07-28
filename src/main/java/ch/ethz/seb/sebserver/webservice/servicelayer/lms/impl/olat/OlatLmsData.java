/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OlatLmsData {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public final class AssessmentData {
        /* OLAT API example:
          {
            "courseName": "course 1",
            "dateFrom": 1624420800000,
            "dateTo": 1624658400000,
            "description": "",
            "assessmentModeKey": 6356992,
            “repositoryEntryKey”: 462324,
            "name": "SEB test"
          }
         */
        public long assessmentModeKey;
        public long repositoryEntryKey;
        public String name;
        public String description;
        public String courseName;
        public long dateFrom;
        public long dateTo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class UserData {
        /* OLAT API example:
          {
            "firstName": "OpenOLAT",
            "key": 360448,
            "lastName": "Administrator",
            "username": "administrator"
          }
         */
        public long key;
        public String firstName;
        public String lastName;
        public String username;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class RestrictionData {
        /* OLAT API example:
          {
            "browserExamKeys": [ "1" ],
            "configKeys": null,
            "key": 8028160
          }
         */
        public long key;
        public List<String> browserExamKeys;
        public List<String> configKeys;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class RestrictionDataPost {
        /* OLAT API example:
          {
            "configKeys": ["a", "b"],
            "browserExamKeys": ["1", "2"]
          }
         */
        public List<String> browserExamKeys;
        public List<String> configKeys;
    }

}


