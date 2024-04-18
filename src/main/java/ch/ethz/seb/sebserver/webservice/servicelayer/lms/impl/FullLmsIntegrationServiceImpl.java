/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamTemplateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
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
    private final ExamTemplateDAO examTemplateDAO;
    private final WebserviceInfo webserviceInfo;

    private final ClientCredentialsResourceDetails resource;

    private final OAuth2RestTemplate restTemplate;

    public FullLmsIntegrationServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final LmsAPIService lmsAPIService,
            final ExamAdminService examAdminService,
            final ExamDAO examDAO,
            final ExamTemplateDAO examTemplateDAO,
            final WebserviceInfo webserviceInfo,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.lmsAPIService = lmsAPIService;
        this.examAdminService = examAdminService;
        this.examDAO = examDAO;
        this.examTemplateDAO = examTemplateDAO;
        this.webserviceInfo = webserviceInfo;

        resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(webserviceInfo.getOAuthTokenURI());
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource.setGrantType(API.GRANT_TYPE_CLIENT);
        resource.setScope(API.RW_SCOPES);

        this.restTemplate = new OAuth2RestTemplate(resource);
        clientHttpRequestFactoryService
                .getClientHttpRequestFactory()
                .onSuccess(this.restTemplate::setRequestFactory)
                .onError(error -> log.warn("Failed to set HTTP request factory: ", error));
        //this.restTemplate.setErrorHandler(new OAuth2AuthorizationContextHolder.OAuth2AuthorizationContext.ErrorHandler(this.resource));
        this.restTemplate
                .getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    @Override
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        final LmsSetup lmsSetup = event.getLmsSetup();
        if (!lmsSetup.getLmsType().features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            return;
        }

        if (lmsSetup.active) {
            applyFullLmsIntegration(lmsSetup.id)
                    .onError(error -> log.warn("Failed to update LMS integration for: {}", lmsSetup, error))
                    .onSuccess(data -> log.debug("Successfully updated LMS integration for: {} data: {}", lmsSetup, data));
        } else if (lmsSetup.integrationActive) {
            deleteFullLmsIntegration(lmsSetup.id)
                    .onError(error -> log.warn("Failed to delete LMS integration for: {}", lmsSetup, error))
                    .onSuccess(data -> log.debug("Successfully deleted LMS integration for: {} data: {}", lmsSetup, data));
        }
    }

    @Override
    public void notifyExamTemplateChange(final ExamTemplateChangeEvent event) {
            final ExamTemplate examTemplate = event.getExamTemplate();
            if (examTemplate == null) {
                return;
            }

            lmsSetupDAO.idsOfActiveWithFullIntegration(examTemplate.institutionId)
                    .onSuccess(all -> all.stream()
                            .map(this::applyFullLmsIntegration)
                            .forEach(res -> {
                                res.onError(error -> log.warn(
                                        "Failed to update LMS Full Integration: {}",
                                        error.getMessage()) );
                            }))
                    .onError(error -> log.warn(
                            "Failed to apply LMS Full Integration change caused by Exam Template: {}",
                            examTemplate,
                            error));
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

                    // reset old token to get actual one
                    resource.setScope(Arrays.asList(String.valueOf(lmsSetupId)));
                    restTemplate.getOAuth2ClientContext().setAccessToken(null);
                    final String accessToken = restTemplate.getAccessToken().getValue();

                    final IntegrationData data = new IntegrationData(
                            connectionId,
                            lmsSetup.name,
                            webserviceInfo.getExternalServerURL(),
                            accessToken,
                            this.getIntegrationTemplates(lmsSetup.institutionId)
                    );

                    return lmsAPIService.getLmsAPITemplate(lmsSetupId)
                            .getOrThrow()
                            .applyConnectionDetails(data)
                            .onError(error -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, false)
                                    .onError(er -> log.error("Failed to set LMS integration inactive", er)))
                            .onSuccess( d -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, true)
                                    .onError(er -> log.error("Failed to set LMS integration active", er)))
                            .getOrThrow();
                });
    }

    private Collection<ExamTemplateSelection> getIntegrationTemplates(final Long institutionId) {
        return examTemplateDAO
                .getAllForLMSIntegration(institutionId)
                .map(all -> all
                        .stream()
                        .map(one -> new ExamTemplateSelection(
                        one.getModelId(),
                                one.name,
                                one.description))
                        .collect(Collectors.toList()))
                .getOrThrow();
    }

    @Override
    public Result<Void> deleteFullLmsIntegration(final Long lmsSetupId) {
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
