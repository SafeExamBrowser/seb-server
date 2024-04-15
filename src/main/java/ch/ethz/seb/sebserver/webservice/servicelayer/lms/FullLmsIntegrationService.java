/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.io.OutputStream;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface FullLmsIntegrationService {

    Result<Void> refreshAccessToken(String lmsUUID);

    Result<Void> applyFullLmsIntegration(Long lmsSetupId);

    Result<Void> deleteFullLmsIntegration(Long lmsSetupId);

    Result<Map<String, String>> getExamTemplateSelection();

    Result<Exam> importExam(
            String lmsUUID,
            String courseId,
            String quizId,
            String examTemplateId,
            String quitPassword,
            String quitLink);

    Result<EntityKey> deleteExam(
            String lmsUUID,
            String courseId,
            String quizId);

    Result<Void> streamConnectionConfiguration(
            String lmsUUID,
            String courseId,
            String quizId,
            OutputStream out);

    @JsonIgnoreProperties(ignoreUnknown = true)
    final class IntegrationData {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("url")
        public final String url;
        @JsonProperty("access_token")
        public final String access_token;
        @JsonProperty("exam_templates")
        public final Map<String, String> exam_templates;

        public IntegrationData(
                @JsonProperty("id") final String id,
                @JsonProperty("name") final String name,
                @JsonProperty("url") final String url,
                @JsonProperty("access_token") final String access_token,
                @JsonProperty("exam_templates") final Map<String, String> exam_templates) {

            this.id = id;
            this.name = name;
            this.url = url;
            this.access_token = access_token;
            this.exam_templates = exam_templates;
        }
    }
}
