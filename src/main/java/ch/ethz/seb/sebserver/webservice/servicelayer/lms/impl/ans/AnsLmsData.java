/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class AnsLmsData {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class SEBServerData {
        /* Ans API example: see nested in AssignmentData */
        public boolean enabled;
        public List<String> config_keys;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class IntegrationsData {
        /* Ans API example: see nested in AssignmentData */
        public SEBServerData safe_exam_browser_server;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class AssignmentData {
        /*
         * Ans API example:
         * {
         * "id": 78711,
         * "course_id": 44412,
         * "name": "Digital test demo",
         * "summative": false,
         * "assignment_type": "Quiz",
         * "start_at": "2021-08-18T09:00:00.000+02:00",
         * "end_at": "2021-08-18T12:00:00.000+02:00",
         * "created_at": "2021-06-21T12:24:28.538+02:00",
         * "updated_at": "2021-08-17T03:41:56.747+02:00",
         * "trashed": false,
         * "start_url": "https://staging.ans.app/digital_test/assignments/78805/results/new",
         * "integrations": {
         * "safe_exam_browser_server": {
         * "enabled": false,
         * "config_keys": [ "123" ] } }
         * "grades_settings": {
         * "grade_calculation": "formula",
         * "grade_formula": "1 + 9 * points / total",
         * "rounding": "decimal",
         * "grade_lower_bound": true,
         * "grade_lower_limit": "1.0",
         * "grade_upper_bound": true,
         * "grade_upper_limit": "10.0",
         * "guess_correction": false,
         * "passed_grade": "5.5"
         * }
         * }
         */
        public long id;
        public long course_id;
        public String name;
        public String external_id;
        public String start_at;
        public String end_at;
        public String start_url;
        public IntegrationsData integrations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class UserData {
        /*
         * Ans API example:
         * {
         * "id": 726404,
         * "student_number": null,
         * "first_name": "John",
         * "middle_name": null,
         * "last_name": "Doe",
         * "external_id": null,
         * "created_at": "2021-06-21T12:07:11.668+02:00",
         * "updated_at": "2021-07-26T20:16:01.638+02:00",
         * "active": true,
         * "email_address": "person@example.org",
         * "affiliation": "employee",
         * "role": "owner"
         * }
         */
        public long id;
        public String first_name;
        public String last_name;
        public String email_address;
        public String external_id;
        public String role;
        public String affiliation;
        public boolean active;
    }

}
