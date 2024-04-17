/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.event.EventListener;

public interface FullLmsIntegrationService {

    Result<IntegrationData> applyFullLmsIntegration(Long lmsSetupId);

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

    @EventListener(ExamDeletionEvent.class)
    void notifyExamDeletion(ExamDeletionEvent event);

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
        public final Collection<ExamTemplateSelection> exam_templates;

        @JsonCreator
        public IntegrationData(
                @JsonProperty("id") final String id,
                @JsonProperty("name") final String name,
                @JsonProperty("url") final String url,
                @JsonProperty("access_token") final String access_token,
                @JsonProperty("exam_templates") final Collection<ExamTemplateSelection> exam_templates) {

            this.id = id;
            this.name = name;
            this.url = url;
            this.access_token = access_token;
            this.exam_templates = Utils.immutableCollectionOf(exam_templates);
        }
    }

    final class ExamTemplateSelection {
        @JsonProperty("template_id")
        public final String template_id;
        @JsonProperty("template_name")
        public final String template_name;
        @JsonProperty("template_description")
        public final String template_description;

        @JsonCreator
        public ExamTemplateSelection(
                @JsonProperty("template_id") final String template_id,
                @JsonProperty("template_name") final String template_name,
                @JsonProperty("template_description") final String template_description) {

            this.template_id = template_id;
            this.template_name = template_name;
            this.template_description = template_description;
        }

    }
}
