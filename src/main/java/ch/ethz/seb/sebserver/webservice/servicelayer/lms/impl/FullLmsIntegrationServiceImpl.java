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
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.DeleteExamAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
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
    private final UserActivityLogDAO userActivityLogDAO;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final ClientConfigService clientConfigService;
    private final DeleteExamAction deleteExamAction;
    private final LmsAPIService lmsAPIService;
    private final ExamAdminService examAdminService;
    private final ExamSessionService examSessionService;
    private final ExamDAO examDAO;
    private final ExamTemplateDAO examTemplateDAO;
    private final WebserviceInfo webserviceInfo;
    private final String lmsAPIEndpoint;
    private final UserService userService;
    private final ClientCredentialsResourceDetails resource;
    private final OAuth2RestTemplate restTemplate;

    public FullLmsIntegrationServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final SEBClientConfigDAO sebClientConfigDAO,
            final ClientConfigService clientConfigService,
            final DeleteExamAction deleteExamAction,
            final LmsAPIService lmsAPIService,
            final ExamAdminService examAdminService,
            final ExamSessionService examSessionService,
            final ExamDAO examDAO,
            final ExamTemplateDAO examTemplateDAO,
            final WebserviceInfo webserviceInfo,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final UserService userService,
            @Value("${sebserver.webservice.lms.api.endpoint}") final String lmsAPIEndpoint,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientConfigService = clientConfigService;
        this.deleteExamAction = deleteExamAction;
        this.lmsAPIService = lmsAPIService;
        this.examAdminService = examAdminService;
        this.examSessionService = examSessionService;
        this.examDAO = examDAO;
        this.examTemplateDAO = examTemplateDAO;
        this.webserviceInfo = webserviceInfo;
        this.lmsAPIEndpoint = lmsAPIEndpoint;
        this.userService = userService;

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
        return lmsSetupDAO
                .byPK(lmsSetupId)
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
                            getAPIRootURL(),
                            accessToken,
                            this.getIntegrationTemplates(lmsSetup.institutionId)
                    );

                    return lmsAPIService.getLmsAPITemplate(lmsSetupId)
                            .getOrThrow()
                            .applyConnectionDetails(data)
                            .onError(error -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, false)
                                    .onError(er -> log.error("Failed to mark LMS integration inactive", er)))
                            .onSuccess( d -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, true)
                                    .onError(er -> log.error("Failed to mark LMS integration active", er)))
                            .getOrThrow();
                });
    }



    @Override
    public Result<Boolean> deleteFullLmsIntegration(final Long lmsSetupId) {
        return lmsSetupDAO
                .byPK(lmsSetupId)
                .map(lmsSetup -> {
                    if (lmsSetup.getConnectionId() == null) {
                        log.warn("No LMS Setup connectionId available for: {}", lmsSetup);
                        return false;
                    }

                    lmsAPIService.getLmsAPITemplate(lmsSetupId)
                            .getOrThrow()
                            .deleteConnectionDetails()
                            .onError(error -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, false)
                                    .onError(er -> log.error("Failed to mark LMS integration inactive", er)))
                            .onSuccess( d -> lmsSetupDAO
                                    .setIntegrationActive(lmsSetupId, false)
                                    .onError(er -> log.error("Failed to mark LMS integration inactive", er)))
                            .getOrThrow();

                    return true;
                });
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
                .map(createAccountAndExam(examTemplateId, quitPassword));
    }

    @Override
    public Result<EntityKey> deleteExam(
            final String lmsUUID,
            final String courseId,
            final String quizId) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPIService::getLmsAPITemplate)
                .map(findQuizData(courseId, quizId))
                .flatMap(this::findExam)
                .map(this::checkDeletion)
                .map(this::logExamDeleted)
                .flatMap(deleteExamAction::deleteExamFromLMSIntegration);
    }

    private Exam checkDeletion(final Exam exam) {
        // TODO check if Exam can be deleted according to the Spec

        // check if there are no active SEB client connections
        if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
            throw new APIMessage.APIMessageException(
                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                            .of("Exam currently has active SEB Client connections."));
        }

        return exam;
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

        try {

            final Result<Exam> examResult = lmsSetupDAO
                    .getLmsSetupIdByConnectionId(lmsUUID)
                    .flatMap(lmsAPIService::getLmsAPITemplate)
                    .map(findQuizData(courseId, quizId))
                    .flatMap(this::findExam);

            if (examResult.hasError()) {
                throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("Exam not found"));
            }

            final Exam exam = examResult.get();

            String connectionConfigId = exam.getAdditionalAttribute(Exam.ADDITIONAL_ATTR_DEFAULT_CONNECTION_CONFIGURATION);
            if (StringUtils.isBlank(connectionConfigId)) {
                connectionConfigId = this.sebClientConfigDAO
                        .all(exam.institutionId, true)
                        .map(all -> all.iterator().next())
                        .map(SEBClientConfig::getModelId)
                        .getOr(null);
            }
            if (StringUtils.isBlank(connectionConfigId)) {
                return Result.ofRuntimeError("No active Connection Configuration found");
            }

            this.clientConfigService.exportSEBClientConfiguration(out, connectionConfigId, exam.id);
            return Result.EMPTY;

        } catch (final Exception e) {
            return Result.ofError(e);
        }
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
                    .onError(error -> log.error("Failed to find quiz-data for id: {}", quizId))
                    // this is only for debugging until Moodle Plugin is ready
                    .getOr(new QuizData(
                            MoodleUtils.getInternalQuizId(quizId, courseId, "MoodlePluginMockQuiz", null),
                            lmsAPITemplate.lmsSetup().institutionId,
                            lmsAPITemplate.lmsSetup().id,
                            lmsAPITemplate.lmsSetup().lmsType,
                            "MoodlePluginMockQuiz",
                            "",
                            DateTime.now(),
                            DateTime.now().plusDays(1),
                            "https://mockmoodle/swvgfrwef.sdvw",
                            null
                    ));
        };
    }

    private Result<Exam> findExam(final QuizData quizData) {
        return examDAO.byExternalIdLike(quizData.id);
    }

    private Function<QuizData, Exam> createAccountAndExam(
            final String examTemplateId,
            final String quitPassword) {

        return quizData -> {

            final SEBServerUser currentUser = userService.getCurrentUser();

            // check if the exam has already been imported, If so return the existing exam
            final Result<Exam> existingExam = findExam(quizData);
            if (!existingExam.hasError()) {
                // TODO do we need to check if ad-hoc account exists and if not, create one?
                return existingExam.get();
            }

            // import exam
            final POSTMapper post = new POSTMapper(null, null);
            post.putIfAbsent(Domain.EXAM.ATTR_EXAM_TEMPLATE_ID, examTemplateId);
            if (StringUtils.isNotBlank(quitPassword)) {
                post.putIfAbsent(Domain.EXAM.ATTR_QUIT_PASSWORD, quitPassword);
            }

            final String accountUUID = createAdHocSupporterAccount(quizData);
            post.putIfAbsent(Domain.EXAM.ATTR_OWNER, accountUUID);

            final Exam exam = new Exam(null, quizData, post);
            return examDAO
                    .createNew(exam)
                    .flatMap(examAdminService::applyExamImportInitialization)
                    .map(this::logExamCreated)
                    .getOrThrow();
        };
    }

    private String createAdHocSupporterAccount(final QuizData data) {
        // TODO create an ad hoc supporter account for this exam and apply it to the exam
        return "mockAccountUUID";
    }

    private void deleteAdHocAccount(final Long examId) {
        try {
            // TODO check if exam has an ad-hoc account and if true, delete it
        } catch (final Exception e) {
            log.error("Failed to delete ad hoc account for exam: {}", examId, e);
        }
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

    private String getAPIRootURL() {
        return webserviceInfo.getExternalServerURL() + lmsAPIEndpoint;
    }

    private Exam logExamCreated(final Exam exam) {
        this.userActivityLogDAO
                .logCreate(exam)
                .onError(error -> log.warn("Failed to log exam creation from LMS: {}", error.getMessage()));
        return exam;
    }

    private Exam logExamDeleted(final Exam exam) {
        this.userActivityLogDAO
                .logDelete(exam)
                .onError(error -> log.warn("Failed to log exam deletion from LMS: {}", error.getMessage()));
        return exam;
    }

}
