/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.TeacherAccountServiceImpl;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.DeleteExamAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamImportService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.apache.commons.io.IOUtils;
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
    private final UserActivityLogDAO userActivityLogDAO;
    private final TeacherAccountServiceImpl teacherAccountServiceImpl;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final ConnectionConfigurationService connectionConfigurationService;
    private final DeleteExamAction deleteExamAction;
    private final LmsAPITemplateCacheService lmsAPITemplateCacheService;
    private final ExamImportService examImportService;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamConfigurationValueService examConfigurationValueService;
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
            final UserDAO userDAO,
            final SEBClientConfigDAO sebClientConfigDAO,
            final ScreenProctoringService screenProctoringService,
            final ConnectionConfigurationService connectionConfigurationService,
            final DeleteExamAction deleteExamAction,
            final ExamConfigurationValueService examConfigurationValueService,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamImportService examImportService,
            final ExamTemplateDAO examTemplateDAO,
            final WebserviceInfo webserviceInfo,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final UserService userService,
            final TeacherAccountServiceImpl teacherAccountServiceImpl,
            final LmsAPITemplateCacheService lmsAPITemplateCacheService,
            @Value("${sebserver.webservice.lms.api.endpoint}") final String lmsAPIEndpoint,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.teacherAccountServiceImpl = teacherAccountServiceImpl;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.connectionConfigurationService = connectionConfigurationService;
        this.deleteExamAction = deleteExamAction;
        this.lmsAPITemplateCacheService = lmsAPITemplateCacheService;
        this.examDAO = examDAO;
        this.examTemplateDAO = examTemplateDAO;
        this.webserviceInfo = webserviceInfo;
        this.lmsAPIEndpoint = lmsAPIEndpoint;
        this.userService = userService;
        this.examConfigurationValueService = examConfigurationValueService;
        this.examImportService = examImportService;
        this.clientConnectionDAO = clientConnectionDAO;

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
        this.restTemplate
                .getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    @Override
    public Result<Exam> applyExamDataToLMS(final Exam exam) {
        return Result.tryCatch(() -> {
            final LmsSetup lmsSetup = lmsSetupDAO.byPK(exam.lmsSetupId).getOrThrow();
            if (lmsSetup.lmsType.features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
                return this.applyExamData(exam, !exam.active);
            }

            return exam;
        });

    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        event.ids.forEach( examId -> this.examDAO.byPK(examId)
                    .map(exam -> applyExamData(exam, true))
                    .onError(error -> log.warn("Failed delete teacher accounts for exam: {}", examId))
        );
    }

    @Override
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        final LmsSetup lmsSetup = event.getLmsSetup();
        if (!lmsSetup.getLmsType().features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            return;
        }

        if (event.activation == Activatable.ActivationAction.NONE) {
            if (!lmsSetup.integrationActive) {
                applyFullLmsIntegration(lmsSetup.id)
                        .onError(error -> log.warn("Failed to update LMS integration for: {} error {}", lmsSetup, error.getMessage()))
                        .onSuccess(data -> log.debug("Successfully updated LMS integration for: {} data: {}", lmsSetup, data));
            }
        } else if (event.activation == Activatable.ActivationAction.ACTIVATE) {
            applyFullLmsIntegration(lmsSetup.id)
                    .map(data -> reapplyExistingExams(data,lmsSetup))
                    .onError(error -> log.warn("Failed to update LMS integration for: {} error {}", lmsSetup, error.getMessage()))
                    .onSuccess(data -> log.debug("Successfully updated LMS integration for: {} data: {}", lmsSetup, data));
        } else if (event.activation == Activatable.ActivationAction.DEACTIVATE) {
            // remove all active exam data for involved exams before deactivate them
            this.examDAO
                    .allActiveForLMSSetup(Arrays.asList(lmsSetup.id))
                    .getOrThrow();
            // delete full integration on Moodle side due to deactivation
            this.deleteFullLmsIntegration(lmsSetup.id)
                    .getOrThrow();
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
                            .forEach(res ->
                                res.onError(error -> log.warn(
                                        "Failed to update LMS Full Integration: {}",
                                        error.getMessage()) )
                            ))
                    .onError(error -> log.warn(
                            "Failed to apply LMS Full Integration change caused by Exam Template: {}",
                            examTemplate,
                            error));
    }

    @Override
    public void notifyConnectionConfigurationChange(final ConnectionConfigurationChangeEvent event) {
        lmsSetupDAO.idsOfActiveWithFullIntegration(event.institutionId)
                .flatMap(examDAO::allActiveForLMSSetup)
                .onError(error -> log.error("Failed to notifyConnectionConfigurationChange: {}", error.getMessage()))
                .getOr(Collections.emptyList())
                .stream()
                .filter(exam -> this.needsConnectionConfigurationChange(exam, event.configId))
                .forEach(this::applyConnectionConfiguration);
    }

    private boolean needsConnectionConfigurationChange(final Exam exam, final Long ccId) {
        if (exam.status == Exam.ExamStatus.ARCHIVED) {
            return false;
        }

        final String configId = getConnectionConfigurationId(exam);
        return StringUtils.isNotBlank(configId) && configId.equals(String.valueOf(ccId));
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

                    return lmsAPITemplateCacheService.getLmsAPITemplate(lmsSetupId)
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

                    if (!lmsSetupDAO.isIntegrationActive(lmsSetupId)) {
                        return true;
                    }

                    lmsAPITemplateCacheService.getLmsAPITemplate(lmsSetupId)
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
            final String quitLink,
            final String examData) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPITemplateCacheService::getLmsAPITemplate)
                .map(template -> getQuizData(template, courseId, quizId, examData))
                //.map(findQuizData(courseId, quizId))
                .map(createExam(examTemplateId, quitPassword))
                .map(exam -> applyExamData(exam, false))
                .map(this::applyConnectionConfiguration);
    }

    @Override
    public Result<EntityKey> deleteExam(
            final String lmsUUID,
            final String courseId,
            final String quizId) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPITemplateCacheService::getLmsAPITemplate)
                .map(findQuizData(courseId, quizId))
                .flatMap(this::findExam)
                .map(this::checkDeletion)
                .map(this::logExamDeleted)
                .flatMap(deleteExamAction::deleteExamInternal);
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
                    .flatMap(lmsAPITemplateCacheService::getLmsAPITemplate)
                    .map(findQuizData(courseId, quizId))
                    .flatMap(this::findExam);

            if (examResult.hasError()) {
                throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("Exam not found"));
            }

            final Exam exam = examResult.get();

            final String connectionConfigId = getConnectionConfigurationId(exam);
            if (StringUtils.isBlank(connectionConfigId)) {
                throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("No active Connection Configuration found"));
            }

            this.connectionConfigurationService.exportSEBClientConfiguration(out, connectionConfigId, exam.id);
            return Result.EMPTY;

        } catch (final Exception e) {
            return Result.ofError(e);
        }
    }

    private LmsSetup reapplyExistingExams(
            final IntegrationData integrationData,
            final LmsSetup lmsSetup) {

            examDAO.allActiveForLMSSetup(Arrays.asList(lmsSetup.id))
                    .getOrThrow()
                    .forEach(exam -> applyExamData(exam, false));

            return lmsSetup;
    }

    private String getConnectionConfigurationId(final Exam exam) {
        String connectionConfigId = exam.getAdditionalAttribute(Exam.ADDITIONAL_ATTR_DEFAULT_CONNECTION_CONFIGURATION);
        if (StringUtils.isBlank(connectionConfigId)) {
            connectionConfigId = this.sebClientConfigDAO
                    .all(exam.institutionId, true)
                    .map(all -> all.stream().filter(config -> config.configPurpose == SEBClientConfig.ConfigPurpose.START_EXAM)
                            .findFirst()
                            .orElseThrow(() -> new APIMessage.APIMessageException(
                                    APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("No active Connection Configuration found"))))
                    .map(SEBClientConfig::getModelId)
                    .getOr(null);
        }
        return connectionConfigId;
    }

    @Override
    public Result<String> getOneTimeLoginToken(
            final String lmsUUID,
            final String courseId,
            final String quizId,
            final AdHocAccountData adHocAccountData) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPITemplateCacheService::getLmsAPITemplate)
                .map(findQuizData(courseId, quizId))
                .flatMap(this::findExam)
                .flatMap(exam  -> this.teacherAccountServiceImpl
                        .getOneTimeTokenForTeacherAccount(exam, adHocAccountData, true));
    }

    private QuizData getQuizData(
            final LmsAPITemplate lmsAPITemplate,
            final String courseId,
            final String quizId,
            final String examData) {

        final String internalQuizId = MoodleUtils.getInternalQuizId(
                quizId,
                courseId,
                null,
                null);

        return lmsAPITemplate.getQuizDataForRemoteImport(examData)
                .getOrThrow();
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
                    .getOrThrow();
        };
    }

    private Result<Exam> findExam(final QuizData quizData) {
        return examDAO.byExternalIdLike(quizData.id);
    }

    private Function<QuizData, Exam> createExam(
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

            final ExamTemplate examTemplate = examTemplateDAO
                    .byModelId(examTemplateId)
                    .getOrThrow();


            // import exam
            final POSTMapper post = new POSTMapper(null, null);
            post.putIfAbsent(Domain.EXAM.ATTR_EXAM_TEMPLATE_ID, examTemplateId);
            post.putIfAbsent(Domain.EXAM.ATTR_OWNER, userService.getCurrentUser().uuid());
            post.putIfAbsent(
                    Domain.EXAM.ATTR_SUPPORTER,
                    StringUtils.join(examTemplate.supporter, Constants.LIST_SEPARATOR));
            post.putIfAbsent(Domain.EXAM.ATTR_TYPE, examTemplate.examType.name());
            if (StringUtils.isNotBlank(quitPassword)) {
                post.putIfAbsent(Domain.EXAM.ATTR_QUIT_PASSWORD, quitPassword);
            }

            final Exam exam = new Exam(null, quizData, post);
            return examDAO
                    .createNew(exam)
                    .flatMap(examImportService::applyExamImportInitialization)
                    .map(this::logExamCreated)
                    .getOrThrow();
        };
    }

    private Exam checkDeletion(final Exam exam) {
        // TODO check if Exam can be deleted according to the Spec

        if (exam.status != Exam.ExamStatus.RUNNING) {
            return exam;
        }

        final Integer active = this.clientConnectionDAO
                .getAllActiveConnectionTokens(exam.id)
                .map(Collection::size)
                .onError(error -> log.warn("Failed to get active access tokens for exam: {}", error.getMessage()))
                .getOr(1);

        if (active == null || active == 0) {
            return exam;
        }

        throw new APIMessage.APIMessageException(
                APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                        .of("Exam currently has active SEB Client connections."));

        // check if there are no active SEB client connections
//        if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
//            throw new APIMessage.APIMessageException(
//                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
//                            .of("Exam currently has active SEB Client connections."));
//        }
//
//        return exam;
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

    private Exam applyExamData(final Exam exam, final boolean deletion) {
        if (exam.examTemplateId == null) {
            throw new IllegalStateException("Exam has no template id: " + exam.getName());
        }

        try {

            final LmsAPITemplate lmsAPITemplate = lmsAPITemplateCacheService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .getOrThrow();
            final String lmsUUID = lmsAPITemplate.lmsSetup().connectionId;
            final String courseId = lmsAPITemplate.getCourseIdFromExam(exam);
            final String quizId = lmsAPITemplate.getQuizIdFromExam(exam);

            final String templateId = deletion ? null : String.valueOf(exam.examTemplateId);
            final String quitPassword = deletion ? null : examConfigurationValueService.getQuitPassword(exam.id);
            final Boolean quitLink = deletion ? null : StringUtils.isNotBlank(examConfigurationValueService.getQuitLink(exam.id));

            final ExamData examData = new ExamData(
                    lmsUUID,
                    courseId,
                    quizId,
                    !deletion,
                    templateId,
                    quitLink,
                    quitPassword);

            lmsAPITemplate.applyExamData(examData).getOrThrow();

        } catch (final Exception e) {
            log.warn("Failed to apply exam data to LMS for exam: {} error: {}", exam, e.getMessage());
        }
        return exam;
    }

    private Exam applyConnectionConfiguration(final Exam exam) {
        return lmsAPITemplateCacheService
                .getLmsAPITemplate(exam.lmsSetupId)
                .flatMap(template -> {
                    final String connectionConfigId = getConnectionConfigurationId(exam);

                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final PipedOutputStream pout;
                    final PipedInputStream pin;
                    try {
                        pout = new PipedOutputStream();
                        pin = new PipedInputStream(pout);

                        this.connectionConfigurationService
                                .exportSEBClientConfiguration(pout, connectionConfigId, exam.id);

                        out.flush();

                        IOUtils.copyLarge(pin, out);

                        // TODO check if this works as expected
                        return template.applyConnectionConfiguration(exam, out.toByteArray());

                    } catch (final Exception e) {
                        throw new RuntimeException("Failed to stream output", e);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                })
                .onError(error -> log.error("Failed to apply ConnectionConfiguration for exam: {} error: ", exam, error))
                .getOr(exam);
    }

    private String getAPIRootURL() {
        return webserviceInfo.getExternalServerURL() + lmsAPIEndpoint;
    }

    private String getAutoLoginURL() {
        return webserviceInfo.getExternalServerURL() + webserviceInfo.getAutoLoginEndpoint();
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
