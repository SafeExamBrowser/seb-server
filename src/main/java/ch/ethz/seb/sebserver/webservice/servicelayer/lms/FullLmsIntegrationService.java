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

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AdHocAccountData;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsSetupChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamArchivedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.event.EventListener;

public interface FullLmsIntegrationService {

    @EventListener(LmsSetupChangeEvent.class)
    void notifyLmsSetupChange(final LmsSetupChangeEvent event);
    @EventListener(ExamTemplateChangeEvent.class)
    void notifyExamTemplateChange(final ExamTemplateChangeEvent event);
    @EventListener(ConnectionConfigurationChangeEvent.class)
    void notifyConnectionConfigurationChange(ConnectionConfigurationChangeEvent event);
    @EventListener(ExamDeletionEvent.class)
    void notifyExamDeletion(ExamDeletionEvent event);
    @EventListener(ExamConfigUpdateEvent.class)
    void notifyExamConfigChange(ExamConfigUpdateEvent event);
    @EventListener(ExamArchivedEvent.class)
    void notifyExamArchived(ExamArchivedEvent event);

    /** Applies the exam data to LMS to inform the LMS that the exam exists on SEB Server site.
     * 
     * @param exam the Exam
     * @return Result refer to the exa or to an error when happened*/
    Result<Exam> applyExamDataToLMS(Exam exam);

    Result<IntegrationData> applyFullLmsIntegration(Long lmsSetupId);

    Result<IntegrationData> applyFullLmsIntegration(LmsSetup lmsSetup);

    Result<Boolean> deleteFullLmsIntegration(Long lmsSetupId);

    Result<Exam> importExam(
            String lmsUUID,
            String courseId,
            String quizId,
            String examTemplateId,
            String quitPassword,
            boolean showQuitLink,
            final String examData);

    Result<EntityKey> deleteExam(
            String lmsUUID,
            String courseId,
            String quizId);
    
    Result<Void> streamConnectionConfiguration(
            String lmsUUID,
            String courseId,
            String quizId,
            OutputStream out);

    Result<String> getOneTimeLoginToken(
            String lmsUUId,
            String courseId,
            String quizId,
            AdHocAccountData adHocAccountData);

    @JsonIgnoreProperties(ignoreUnknown = true)
    final class ExamData {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("course_id")
        public final String course_id;
        @JsonProperty("quiz_id")
        public final String quiz_id;
        @JsonProperty("exam_created")
        public final Boolean exam_created;
        @JsonProperty("template_id")
        public final String template_id;
        @JsonProperty("show_quit_link")
        public final String quit_link;
        @JsonProperty("quit_password")
        public final String quit_password;
        @JsonProperty("next_course_id")
        public final String next_course_id;
        @JsonProperty("next_quiz_id")
        public final String next_quiz_id;

        public ExamData(
                final String id,
                final String course_id,
                final String quiz_id,
                final Boolean exam_created,
                final String template_id,
                final String quit_link,
                final String quit_password,
                final String next_course_id,
                final String next_quiz_id) {

            this.id = id;
            this.course_id = course_id;
            this.quiz_id = quiz_id;
            this.exam_created = exam_created;
            this.template_id = template_id;
            this.quit_link = quit_link;
            this.quit_password = quit_password;
            this.next_course_id = next_course_id;
            this.next_quiz_id = next_quiz_id;
        }

        @Override
        public String toString() {
            return "ExamData{" +
                    "id='" + id + '\'' +
                    ", course_id='" + course_id + '\'' +
                    ", quiz_id='" + quiz_id + '\'' +
                    ", exam_created=" + exam_created +
                    ", template_id='" + template_id + '\'' +
                    ", quit_link='" + quit_link + '\'' +
                    ", quit_password='" + quit_password + '\'' +
                    ", next_course_id='" + next_course_id + '\'' +
                    ", next_quiz_id='" + next_quiz_id + '\'' +
                    '}';
        }
    }

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

        @Override
        public String toString() {
            return "IntegrationData{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    ", access_token='" + access_token + '\'' +
                    ", exam_templates=" + exam_templates +
                    '}';
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

    final class TokenLoginResponse {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("login_link")
        public final String loginLink;

        public TokenLoginResponse(
                final String id,
                final String loginLink) {

            this.id = id;
            this.loginLink = loginLink;
        }
    }
}
