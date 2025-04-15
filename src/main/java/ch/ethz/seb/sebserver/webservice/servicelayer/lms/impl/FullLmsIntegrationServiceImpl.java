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
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.*;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AdHocAccountData;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.DeleteExamAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamImportService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleResponseException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamArchivedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
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
    
    public static final String DEFAULT_SEB_QUIT_URL = "http://quit_seb";

    private final LmsSetupDAO lmsSetupDAO;
    private final UserActivityLogDAO userActivityLogDAO;
    private final TeacherAccountService teacherAccountService;
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
    private final SEBRestrictionService sebRestrictionService;
    private final OAuth2RestTemplate restTemplate;
    private final AdditionalAttributesDAO additionalAttributesDAO;

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
            final TeacherAccountService teacherAccountService,
            final LmsAPITemplateCacheService lmsAPITemplateCacheService,
            final SEBRestrictionService sebRestrictionService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            @Value("${sebserver.webservice.lms.api.endpoint}") final String lmsAPIEndpoint,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.teacherAccountService = teacherAccountService;
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
        this.sebRestrictionService = sebRestrictionService;
        this.additionalAttributesDAO = additionalAttributesDAO;

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
            if (hasActiveFullIntegration(exam.lmsSetupId)) {
                this.applyExamData(exam, !exam.active);
            }
            return exam;
        });
    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        event.ids.forEach( examId -> this.examDAO.byPK(examId)
                    .map(exam -> applyExamData(exam, true))
                    .onError(error -> log.warn("Failed delete exam data on LMS for exam: {}", examId))
        );
    }

    @Override
    public void notifyExamConfigChange(final ExamConfigUpdateEvent event) {
        try {

            final Exam exam = examDAO.byPK(event.examId).getOrThrow();
            if (!hasActiveFullIntegration(exam.lmsSetupId)) {
                return;
            }

            this.applyExamData(exam, !exam.active);

        } catch (final Exception e) {
            log.error( "Failed to apply Exam Configuration change to fully integrated LMS for exam: {}", event.examId, e);
        }
    }

    @Override
    public void notifyExamArchived(final ExamArchivedEvent event) {
        try {

            if (event.exam == null || !hasActiveFullIntegration(event.exam.lmsSetupId)) {
                return;
            }

            this.applyExamData(event.exam, true);

        } catch (final Exception e) {
            log.error("Failed to apply Exam archive and exam data deletion on LMS for exam: {}", event.exam, e);
        }
    }

    @Override
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        
        final LmsSetup lmsSetup = event.getLmsSetup();
        if (!hasFullIntegration(lmsSetup.id, false)) {
            return;
        }
        
        try {
            if (event.activation == Activatable.ActivationAction.NONE) {
                if (!lmsSetup.integrationActive) {
                    applyFullLmsIntegration(lmsSetup.id)
                            .onError(error -> log.warn(
                                    "Failed to update LMS integration for: {} error {}", lmsSetup, error.getMessage()))
                            .onSuccess(data -> log.debug(
                                    "Successfully updated LMS integration for: {} data: {}", lmsSetup, data));
                }
            } else if (event.activation == Activatable.ActivationAction.ACTIVATE) {
                applyFullLmsIntegration(lmsSetup.id)
                        .map(data -> reapplyExistingExams(data, lmsSetup))
                        .onError(error -> log.warn(
                                "Failed to update LMS integration for: {} error {}", lmsSetup, error.getMessage()))
                        .onSuccess(data -> log.debug(
                                "Successfully updated LMS integration for: {} data: {}", lmsSetup, data));
            } else if (event.activation == Activatable.ActivationAction.DEACTIVATE) {

                log.info("Deactivate full integration for LMS: {}", lmsSetup);

                // remove all exam data for involved exams before deactivate them
                this.examDAO
                        .allForLMSSetup(lmsSetup.id)
                        .getOrThrow()
                        .forEach(exam -> applyExamData(exam, true));
                
                // delete full integration on Moodle side due to deactivation
                this.teacherAccountService.deleteAllFromLMS(lmsSetup.id);
                this.deleteFullLmsIntegration(lmsSetup.id)
                        .getOrThrow();
            }
        } catch (final Exception e) {
            log.error("Failed to apply LMS Setup change for full LMS integration for: {}", lmsSetup, e);
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
                            .filter(this::hasActiveFullIntegration)
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
                .onError(error -> log.error(
                        "Failed to notifyConnectionConfigurationChange: {}",
                        error.getMessage()))
                .getOr(Collections.emptyList())
                .stream()
                .filter(exam -> this.needsConnectionConfigurationChange(exam, event.configId))
                .forEach(exam -> applyExamData(exam, false));
    }

    @Override
    public Result<IntegrationData> applyFullLmsIntegration(final Long lmsSetupId) {
        return lmsSetupDAO
                .byPK(lmsSetupId)
                .flatMap(this::applyFullLmsIntegration);
    }

    @Override
    public Result<IntegrationData> applyFullLmsIntegration(final LmsSetup lmsSetup) {
        return Result.tryCatch(() -> {
            final Long lmsSetupId = lmsSetup.id;
            final String connectionId = lmsSetup.getConnectionId();
            if (connectionId == null) {
                throw new RuntimeException("Illegal state", new MoodleResponseException(
                        "The Assessment Tool Setup must be saved first before full integration can be tested and applied.","none"));
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
                    .onSuccess(d -> lmsSetupDAO
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
            final boolean showQuitLink,
            final String examData) {

        return lmsSetupDAO
                .getLmsSetupIdByConnectionId(lmsUUID)
                .flatMap(lmsAPITemplateCacheService::getLmsAPITemplate)
                .map(template -> getQuizData(template, courseId, quizId, examData))
                .map(createExam(examTemplateId, showQuitLink, quitPassword))
                .map(exam -> applyExamData(exam, false))
                .map(this::applySEBClientRestrictionIfRunning);
    }

    private Exam applySEBClientRestrictionIfRunning(final Exam exam) {
        if (exam.status == Exam.ExamStatus.RUNNING) {
            return sebRestrictionService
                    .applySEBClientRestriction(exam)
                    .getOrThrow();
        }
        return exam;
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
                log.error(
                        "Failed to find exam for SEB Connection Configuration download: ",
                        examResult.getError());
                throw new APIMessage.APIMessageException(
                        APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("Exam not found"));
            }

            final Exam exam = examResult.get();

            final String connectionConfigId = getConnectionConfigurationId(exam);
            if (StringUtils.isBlank(connectionConfigId)) {
                log.error(
                        "Failed to verify SEB Connection Configuration id for exam: {}",
                        exam.name);
                throw new APIMessage.APIMessageException(
                        APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of("No active Connection Configuration found"));
            }

            this.connectionConfigurationService.exportSEBClientConfiguration(
                    out,
                    connectionConfigId,
                    exam.id);

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
                                    APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT.of(
                                            "No active Connection Configuration found"))))
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
                .flatMap(exam  -> this.teacherAccountService
                        .getOneTimeTokenForTeacherAccount(exam, adHocAccountData, true));
    }

    private QuizData getQuizData(
            final LmsAPITemplate lmsAPITemplate,
            final String courseId,
            final String quizId,
            final String examData) {

        if (StringUtils.isNotBlank(examData)) {
            return lmsAPITemplate
                    .getQuizDataForRemoteImport(examData)
                    .getOrThrow();
        } else {

            final String internalQuizId = MoodleUtils.getInternalQuizId(
                    quizId,
                    courseId,
                    null,
                    null);

            return lmsAPITemplate.getQuiz(internalQuizId)
                    .getOrThrow();

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
                    .getOrThrow();
        };
    }

    private Result<Exam> findExam(final QuizData quizData) {
        return examDAO.byExternalIdLike(quizData.id);
    }

    private Function<QuizData, Exam> createExam(
            final String examTemplateId,
            final boolean showQuitLink,
            final String quitPassword) {

        return quizData -> {

            final SEBServerUser currentUser = userService.getCurrentUser();

            // check if the exam has already been imported, If so return the existing exam if it is not archived
            final Result<Exam> existingExam = findExam(quizData);
            if (!existingExam.hasError()) {
                final Exam exam = existingExam.get();
                if (exam.status == Exam.ExamStatus.ARCHIVED) {
                    throw new IllegalArgumentException("Exam is archived and cannot be re-connected by LMS full integration!");
                }
                return exam;
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
                    .map( e -> this.applyQuitLinkToSEBConfig(e, showQuitLink))
                    .map(this::logExamCreated)
                    .getOrThrow();
        };
    }

    private Exam checkDeletion(final Exam exam) {
        if (exam.status != Exam.ExamStatus.RUNNING) {
            return exam;
        }

        // if exam is running and has active SEB client connections, it cannot be deleted
        final Integer active = this.clientConnectionDAO
                .getAllActiveConnectionTokens(exam.id)
                .map(Collection::size)
                .onError(error -> log.warn(
                        "Failed to get active access tokens for exam: {}",
                        error.getMessage()))
                .getOr(1);

        if (active == null || active == 0) {
            return exam;
        }

        throw new APIMessage.APIMessageException(
                APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                        .of("Exam currently has active SEB Client connections."));
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
        if (!hasFullIntegration(exam.lmsSetupId, true)) {
            return exam;
        }

        try {

            final LmsAPITemplate lmsAPITemplate = lmsAPITemplateCacheService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .getOrThrow();
            final String lmsUUID = lmsAPITemplate.lmsSetup().connectionId;
            final String courseId = lmsAPITemplate.getCourseIdFromExam(exam);
            final String quizId = lmsAPITemplate.getQuizIdFromExam(exam);

            final String templateId = deletion
                    ? null
                    : exam.examTemplateId != null
                        ? String.valueOf(exam.examTemplateId)
                        : "0"; // no selection on Moodle site
            final String quitPassword = deletion
                    ? null
                    : examConfigurationValueService.getQuitPassword(exam.id);
            final String quitLink = deletion
                    ? null
                    : examConfigurationValueService.getQuitLink(exam.id);
            
            String nextCourseId = null;
            String nextQuizId = null;
            if (exam.followUpId != null) {
                final Exam followupExam = examDAO.byPK(exam.followUpId).getOr(null);
                if (followupExam != null) {
                    nextCourseId = lmsAPITemplate.getCourseIdFromExam(followupExam);
                    nextQuizId = lmsAPITemplate.getQuizIdFromExam(followupExam);
                }
            }

            final ExamData examData = new ExamData(
                    lmsUUID,
                    courseId,
                    quizId,
                    !deletion,
                    templateId,
                    quitLink,
                    quitPassword,
                    nextCourseId,
                    nextQuizId);

            final String response = lmsAPITemplate.applyExamData(examData).getOrThrow();
            if (exam.followUpId != null) {
                try {


                    final String reconfigurationLink = extractReconfigurationLinkFromLMSResponse(lmsAPITemplate, response);
                    if (reconfigurationLink == null) {
                        log.error("Failed to parse reconfiguration link from LMS response: {}", response);
                        throw new RuntimeException("Failed to parse reconfiguration link");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Apply next quit download link as reconfiguration link: {}", reconfigurationLink);
                    }
                    
                    // save link in additional attributes for later apply if needed
                    additionalAttributesDAO
                            .saveAdditionalAttribute(
                                    EntityType.EXAM, 
                                    exam.id, 
                                    Exam.ADDITIONAL_ATTR_CONSECUTIVE_QUIZ_DOWNLOAD_LINK, 
                                    reconfigurationLink)
                            .onError(error -> log.error(
                                    "Failed to save download link for next exam in additional attributes: {}", 
                                    error.getMessage()));

                    // apply to SEB Settings of current Exam Config if available on Exam
                    return examConfigurationValueService
                            .applyConsecutiveExamSettings(exam)
                            .onError( error -> log.error(
                                    "Failed to automatically set SEB Settings for consecutive quizzes for Exam: {} cause: {}", 
                                    exam, 
                                    error.getMessage()))
                            .getOr(exam);
                } catch (final Exception e) {
                    log.warn("Failed to create download link for next exam: {}", e.getMessage());
                }
            }

        } catch (final Exception e) {
            log.warn("Failed to apply exam data to LMS for exam: {} error: {}", exam, e.getMessage());
        }
        return exam;
    }

    private String extractReconfigurationLinkFromLMSResponse(
            final LmsAPITemplate lmsAPITemplate, 
            final String response) {
        
        // response contains the quiz context id which is used to create the download link for next Connection Config
        // URL/pluginfile.php/XYZ/quizaccess_sebserver/filemanager_sebserverconfigfile/0/SEBServerSettings.seb
        Integer quizContextId = null;
        
        if (lmsAPITemplate.lmsSetup().lmsType == LmsSetup.LmsType.MOODLE_PLUGIN) {
            try {
                // response is {"success":true,"context":489,....
                final JSONMapper jsonMapper = new JSONMapper();
                final MoodleUtils.ExamDataApplyResponse examDataApplyResponse = jsonMapper.readValue(
                        response,
                        MoodleUtils.ExamDataApplyResponse.class);

                quizContextId = examDataApplyResponse.context;
            } catch (final Exception e) {
                log.error("Failed to parse context id from Moodle response: {}, cause: {}", response, e.getMessage());
                return null;
            }
        } else if (lmsAPITemplate.lmsSetup().lmsType == LmsSetup.LmsType.MOCKUP) {
            quizContextId = Integer.valueOf(response);
        }
        
        final String lmsApiUrl = lmsAPITemplate.lmsSetup().lmsApiUrl;
        final String downloadLink = lmsApiUrl.endsWith(Constants.SLASH.toString()) 
                ? lmsApiUrl 
                : lmsApiUrl + Constants.SLASH;
        return downloadLink + MoodleUtils.SEB_RECONFIGURATION_LINK_TEMPLATE.formatted(quizContextId);
    }

    private Exam applyQuitLinkToSEBConfig(final Exam exam, final boolean showQuitLink) {
        try {

            if (!showQuitLink) {
                // check set no quit link to SEB config
                examConfigurationValueService
                        .applyQuitURLToConfigs(exam.id, "")
                        .getOrThrow();
            } else {
                // check if in config quit link is set, if so nothing to do, if not generate one and apply
                String quitLink = examConfigurationValueService.getQuitLink(exam.id);
                if (StringUtils.isNotBlank(quitLink)) {
                    return exam;
                }

                quitLink = DEFAULT_SEB_QUIT_URL;

                examConfigurationValueService
                        .applyQuitURLToConfigs(exam.id, quitLink)
                        .getOrThrow();
            }

            return exam;
        } catch (final Exception e) {
            log.error("Failed to apply quit link to SEB Exam Configuration: ", e);
            return exam;
        }
    }

    private boolean hasActiveFullIntegration(final Long lmsSetupId) {
        return hasFullIntegration(lmsSetupId, true);
    }
    private boolean hasFullIntegration(final Long lmsSetupId, final boolean checkActive) {
        // no LMS
        if (lmsSetupId == null) {
            return false;
        }

        final LmsAPITemplate lmsAPITemplate = this.lmsAPITemplateCacheService
                .getLmsAPITemplate(lmsSetupId)
                .onError(error -> log.error("Failed to get LmsAPITemplate: ", error))
                .getOr(null);
        
        if (lmsAPITemplate == null) {
            return false;
        }
        
        final LmsSetup lmsSetup = lmsAPITemplate.lmsSetup();
        if (!lmsSetup.getLmsType().features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            return false;
        }

        return !checkActive || lmsAPITemplate.fullIntegrationActive();
    }

    private boolean needsConnectionConfigurationChange(final Exam exam, final Long ccId) {
        if (exam.status == Exam.ExamStatus.ARCHIVED) {
            return false;
        }

        final String configId = getConnectionConfigurationId(exam);
        return StringUtils.isNotBlank(configId) && configId.equals(String.valueOf(ccId));
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
                .onError(error -> log.warn(
                        "Failed to log exam creation from LMS: {}",
                        error.getMessage()));
        return exam;
    }

    private Exam logExamDeleted(final Exam exam) {
        this.userActivityLogDAO
                .logDelete(exam)
                .onError(error -> log.warn(
                        "Failed to log exam deletion from LMS: {}",
                        error.getMessage()));
        return exam;
    }

}
