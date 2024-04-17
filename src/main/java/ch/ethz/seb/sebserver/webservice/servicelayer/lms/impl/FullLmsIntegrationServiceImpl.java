/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class FullLmsIntegrationServiceImpl implements FullLmsIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(FullLmsIntegrationServiceImpl.class);

    private final LmsSetupDAO lmsSetupDAO;
    private final LmsAPIService lmsAPIService;
    private final ExamAdminService examAdminService;
    private final ExamDAO examDAO;
    private final WebserviceInfo webserviceInfo;

    public FullLmsIntegrationServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final LmsAPIService lmsAPIService,
            final ExamAdminService examAdminService,
            final ExamDAO examDAO,
            final WebserviceInfo webserviceInfo) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.lmsAPIService = lmsAPIService;
        this.examAdminService = examAdminService;
        this.examDAO = examDAO;
        this.webserviceInfo = webserviceInfo;
    }

    @Override
    public Result<IntegrationData> applyFullLmsIntegration(final Long lmsSetupId) {
        return lmsSetupDAO.byPK(lmsSetupId)
                .map(lmsSetup -> {
                    String connectionId = lmsSetup.getConnectionId();
                    if (connectionId == null) {
                        connectionId = lmsSetupDAO.save(lmsSetup).getOrThrow().connectionId;
                    }
                    if (connectionId == null) {
                        throw new IllegalStateException("No LMS Setup connectionId available for: " + lmsSetup);
                    }

                    final IntegrationData data = new IntegrationData(
                            connectionId,
                            lmsSetup.name,
                            webserviceInfo.getExternalServerURL(),
                            this.getIntegrationAccessToken(lmsSetup),
                            this.getIntegrationTemplates()
                    );

                    return lmsAPIService.getLmsAPITemplate(lmsSetupId)
                            .getOrThrow()
                            .applyConnectionDetails(data)
                            .getOrThrow();
                });
    }

    private String getIntegrationAccessToken(LmsSetup lmsSetup) {
        // TODO
        return null;
    };

    private Collection<ExamTemplateSelection> getIntegrationTemplates() {
        // TODO
        return null;
    }

    @Override
    public Result<Void> deleteFullLmsIntegration(final Long lmsSetupId) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Map<String, String>> getExamTemplateSelection() {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Exam> importExam(
            final String lmsUUID,
            final String courseId,
            final String quizId,
            final String examTemplateId,
            final String quitPassword,
            final String quitLink) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPIService::getLmsAPITemplate)
                .map(findQuizData(courseId, quizId))
                .map(createExam(examTemplateId, quitPassword))
                .map(this::createAdHocSupporterAccount);
    }

    @Override
    public Result<EntityKey> deleteExam(
            final String lmsUUID,
            final String courseId,
            final String quizId) {

        return findExam(courseId, quizId)
                .flatMap(exam -> examDAO.deleteOne(exam.id));
    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        event.ids.forEach(this::deleteAdHocAccount);
    }

    @Override
    public Result<Void> streamConnectionConfiguration(
            final String lmsUUID,
            final String courseId,
            final String quizId,
            final OutputStream out) {
        return Result.ofRuntimeError("TODO");
    }

    private Function<LmsAPITemplate, QuizData> findQuizData(
            final String courseId,
            final String quizId) {

        return lmsAPITemplate -> {
            final String internalQuizId = MoodleUtils.getInternalQuizId(
                    quizId,
                    courseId,
                    null,
                    null);

            return lmsAPITemplate
                    .getQuiz(internalQuizId)
                    .getOrThrow();
        };
    }

    private Result<Exam> findExam(
            final String courseId,
            final String quizId) {

        final String externalIdLike = quizId + Constants.COLON + courseId + Constants.PERCENTAGE;
        return examDAO.byExternalIdLike(externalIdLike);
    }

    private Function<QuizData, Exam> createExam(
            final String examTemplateId,
            final String quitPassword) {

        return quizData -> {

            final POSTMapper post = new POSTMapper(null, null);
            post.putIfAbsent(Domain.EXAM.ATTR_EXAM_TEMPLATE_ID, examTemplateId);
            if (StringUtils.isNotBlank(quitPassword)) {
                post.putIfAbsent(Domain.EXAM.ATTR_QUIT_PASSWORD, quitPassword);
            }

            final Exam exam = new Exam(null, quizData, post);

            return examDAO
                    .createNew(exam)
                    .flatMap(examAdminService::applyExamImportInitialization)
                    .getOrThrow();
        };
    }

    private Exam createAdHocSupporterAccount(final Exam exam) {
        // TODO create an ad hoc supporter account for this exam and apply it to the exam
        return exam;
    }

    private void deleteAdHocAccount(final Long examId) {
        try {
            // TODO check if exam has an ad-hoc account and if true, delete it
        } catch (final Exception e) {
            log.error("Failed to delete ad hoc account for exam: {}", examId, e);
        }
    }

}
