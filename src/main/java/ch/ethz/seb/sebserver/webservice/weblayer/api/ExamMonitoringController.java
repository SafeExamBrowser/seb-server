/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import ch.ethz.seb.sebserver.gbl.model.session.*;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionIssueStatus;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringStaticClientData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_MONITORING_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class ExamMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamMonitoringController.class);

    private final SEBClientConnectionService sebClientConnectionService;
    private final ExamSessionService examSessionService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final AuthorizationService authorization;
    private final PaginationService paginationService;
    private final SEBClientNotificationService sebClientNotificationService;
    private final ExamAdminService examAdminService;
    private final SecurityKeyService securityKeyService;
    private final ScreenProctoringService screenProctoringService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExamDAO examDAO;
    private final Executor executor;

    public ExamMonitoringController(
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientInstructionService sebClientInstructionService,
            final AuthorizationService authorization,
            final PaginationService paginationService,
            final SEBClientNotificationService sebClientNotificationService,
            final SecurityKeyService securityKeyService,
            final ExamAdminService examAdminService,
            final ScreenProctoringService screenProctoringService,
            final ApplicationEventPublisher applicationEventPublisher,
            final ExamDAO examDAO,
            @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) final Executor executor) {

        this.sebClientConnectionService = sebClientConnectionService;
        this.examSessionService = sebClientConnectionService.getExamSessionService();
        this.sebClientInstructionService = sebClientInstructionService;
        this.authorization = authorization;
        this.paginationService = paginationService;
        this.sebClientNotificationService = sebClientNotificationService;
        this.examAdminService = examAdminService;
        this.securityKeyService = securityKeyService;
        this.screenProctoringService = screenProctoringService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.examDAO = examDAO;
        this.executor = executor;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users institutionId as default.
     * <p>
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    /** Get a page of all currently running exams
     * <p>
     * GET /{api}/{entity-type-endpoint-name}
     * <p>
     * GET /admin-api/v1/monitoring
     * GET /admin-api/v1/monitoring?page_number=2&page_size=10&sort=-name
     * GET /admin-api/v1/monitoring?name=seb&active=true
     *
     * @param institutionId The institution identifier of the request.
     *            Default is the institution identifier of the institution of the current user
     * @param pageNumber the number of the page that is requested
     * @param pageSize the size of the page that is requested
     * @param sort the sort parameter to sort the list of entities before paging
     *            the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
     *            descending sort order
     * @param allRequestParams a MultiValueMap of all request parameter that is used for filtering
     * @return Page of domain-model-entities of specified type */
    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Exam> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER, UserRole.TEACHER,
                UserRole.EXAM_ADMIN);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());

        // if current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, EntityType.EXAM)) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final Collection<Exam> exams = this.examSessionService
                .getFilteredRunningExams(
                        filterMap,
                        exam -> this.hasRunningExamPrivilege(exam, institutionId))
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                exams,
                ExamAdministrationController.pageSort(sort));
    }

    /** Get a page of all currently finished exams
     * <p>
     * GET /{api}/{entity-type-endpoint-name}
     * <p>
     * GET /admin-api/v1/monitoring
     * GET /admin-api/v1/monitoring?page_number=2&page_size=10&sort=-name
     * GET /admin-api/v1/monitoring?name=seb&active=true
     *
     * @param institutionId The institution identifier of the request.
     *            Default is the institution identifier of the institution of the current user
     * @param pageNumber the number of the page that is requested
     * @param pageSize the size of the page that is requested
     * @param sort the sort parameter to sort the list of entities before paging
     *            the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
     *            descending sort order
     * @param allRequestParams a MultiValueMap of all request parameter that is used for filtering
     * @return Page of domain-model-entities of specified type */
    @RequestMapping(
            path = API.EXAM_MONITORING_FINISHED_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Exam> getFinishedExamsPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER,
                UserRole.TEACHER,
                UserRole.EXAM_ADMIN);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());

        // if current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, EntityType.EXAM)) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final Collection<Exam> exams = this.examSessionService
                .getFilteredFinishedExams(
                        filterMap,
                        exam -> this.hasRunningExamPrivilege(exam, institutionId))
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                exams,
                ExamAdministrationController.pageSort(sort));
    }

    @RequestMapping(
            path = API.EXAM_MONITORING_TEST_RUN_ENDPOINT +
                    API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam toggleTestRunForExam(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long examId) {

        // check overall privileges
        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER,
                UserRole.TEACHER,
                UserRole.EXAM_ADMIN);

        return this.examDAO.byPK(examId)
                .flatMap(authorization::checkModify)
                .flatMap(examSessionService::toggleTestRun)
                .map(exam -> {
                    examSessionService.flushCache(exam);
                    if (exam.status == Exam.ExamStatus.TEST_RUN) {
                        applicationEventPublisher.publishEvent(new ExamStartedEvent(exam));
                    } else if (exam.status == Exam.ExamStatus.UP_COMING) {
                        applicationEventPublisher.publishEvent(new ExamFinishedEvent(exam));
                    }
                    return exam;
                })
                .getOrThrow();
    }

    /** This is the older exam monitoring data endpoint where all Client Connection data for an exam can be requested 
     *  in a list. This results in a list with full ClientConnectionData for every request and is therefore not performant
     *  when a lot of client connection must be displayed and updated.
     * @param institutionId the institution identifier from the user. If absent the system will get the one from logged in user
     * @param examId The exam identifier
     * @param hiddenStates Comma separated list of exam state names that are hidden from the filter and shall not be included
     * @param hiddenClientGroups Comma separated list of client group ids of client groups that are hidden from the filter and shall not be included
     * @param hiddenIssues Comma separated list of ConnectionIssueStatus names that are hidden from the filter and shall not be included
     * @return Filtered Collection of ClientConnectionData for exam monitoring display and update
     */
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ClientConnectionData> getConnectionData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestHeader(name = API.EXAM_MONITORING_STATE_FILTER, required = false) final String hiddenStates,
            @RequestHeader(name = API.EXAM_MONITORING_CLIENT_GROUP_FILTER, required = false) final String hiddenClientGroups,
            @RequestHeader(name = API.EXAM_MONITORING_ISSUE_FILTER, required = false) final String hiddenIssues){


        checkPrivileges(institutionId, examId);

        return this.examSessionService
                .getConnectionData(examId, createMonitoringFilter(hiddenStates, hiddenClientGroups, hiddenIssues))
                .getOrThrow();
    }

    /** Get the static client connection monitoring data for a given exam.
     * <p> 
     * Note: To get better performance the above endpoint and result that contains the all monitoring client data, has been 
     * split up into static and non-static data. Static date usually is only changing once or twice per SEB connection or only 
     * within a SEB connection state change where none-static data can change anytime and needs to be constantly updated.
     * <p> 
     * This gets the static data for specific requested SEB connections for update. A GUI client can use this to get the static data
     * for all SEB connections that has changed it state since the last update for example and do not need to call this every 
     * one or two seconds.
     * 
     * @param institutionId the institution identifier from the user. If absent the system will get the one from logged in user
     * @param examId The exam identifier
     * @param clientConnectionIds Comma separated list of ClientConnection ids (PKs not connectionTokens)
     * @return The MonitoringStaticClientData containing all static data of requested SEB Client connections
     */
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_STATIC_CLIENT_DATA,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MonitoringStaticClientData getMonitoringStaticClientData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestParam(name = API.PARAM_MODEL_ID_LIST, required = true) final String clientConnectionIds) {

        final Exam runningExam = checkPrivileges(institutionId, examId);

        final Set<Long> ids = Stream.of(StringUtils.split(clientConnectionIds, Constants.LIST_SEPARATOR))
                .map(Utils::toLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return this.examSessionService
                .getMonitoringSEBConnectionStaticData(runningExam.id, ids)
                .getOrThrow();
    }
    
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_FULLPAGE,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MonitoringFullPageData getFullMonitoringPageData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestHeader(name = API.EXAM_MONITORING_STATE_FILTER, required = false) final String hiddenStates,
            @RequestHeader(name = API.EXAM_MONITORING_CLIENT_GROUP_FILTER, required = false) final String hiddenClientGroups,
            @RequestHeader(name = API.EXAM_MONITORING_ISSUE_FILTER, required = false) final String hiddenIssues){


        // TODO respond this within another Thread-pool (Executor)
        // TODO try to cache some MonitoringSEBConnectionData throughout multiple requests (for about 2 sec.)
        //      problem: different filter settings, maybe cache unfiltered data and then just filter cached data per request

        final Exam runningExam = checkPrivileges(institutionId, examId);

        final MonitoringSEBConnectionData monitoringSEBConnectionData = this.examSessionService
                .getMonitoringSEBConnectionsData(
                        examId,
                        createMonitoringFilter(hiddenStates, hiddenClientGroups, hiddenIssues))
                .getOrThrow();

        final boolean proctoringEnabled = this.examAdminService.isProctoringEnabled(runningExam);
        final boolean screenProctoringEnabled = this.examAdminService.isScreenProctoringEnabled(runningExam);
        final Collection<ProctoringGroupMonitoringData> screenProctoringData = (screenProctoringEnabled)
                ? this.screenProctoringService
                    .getCollectingGroupsMonitoringData(examId)
                    .getOr(Collections.emptyList())
                : Collections.emptyList();

        return new MonitoringFullPageData(
                examId,
                monitoringSEBConnectionData,
                screenProctoringData);
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientConnectionData getConnectionDataForSingleConnection(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken) {

        checkPrivileges(institutionId, examId);

        return this.examSessionService
                .getConnectionData(connectionToken)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_INSTRUCTION_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public void registerInstruction(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @Valid @RequestBody final ClientInstruction clientInstruction) {

        checkPrivileges(institutionId, examId);
        this.sebClientInstructionService.registerInstructionAsync(clientInstruction);
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_NOTIFICATION_ENDPOINT +
                    API.EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ClientNotification> pendingNotifications(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken) {

        checkPrivileges(institutionId, examId);

        final ClientConnectionData connection = getConnectionDataForSingleConnection(
                institutionId,
                examId,
                connectionToken);
        return this.sebClientNotificationService
                .getPendingNotifications(connection.getConnectionId())
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_NOTIFICATION_ENDPOINT +
                    API.MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void confirmNotification(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long notificationId,
            @PathVariable(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken) {

        checkPrivileges(institutionId, examId);

        this.sebClientNotificationService.confirmPendingNotification(
                notificationId,
                examId,
                connectionToken)
                .onError(error -> {
                    log.error("Failed to confirm pending notification: {} for exam {}, cause: {}",
                            notificationId, examId, error.getMessage());
                });
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_DISABLE_CONNECTION_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void disableConnection(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestParam(
                    name = Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                    required = true) final String connectionToken) {

        checkPrivileges(institutionId, examId);
        
        if (connectionToken.contains(Constants.LIST_SEPARATOR)) {
            // If we have a bunch of client connections to disable, make it asynchronously and respond to the caller immediately
            this.executor.execute(() -> {
                final String[] tokens = StringUtils.split(connectionToken, Constants.LIST_SEPARATOR);
                this.sebClientConnectionService
                        .disableConnections(tokens, institutionId)
                        .onError(error -> log.error(
                                "Unexpected error while disable connection. See previous logs for more information.",
                                error));
            });
        } else {
            this.sebClientConnectionService
                    .disableConnection(connectionToken, institutionId)
                    .getOrThrow();
        }
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_SIGNATURE_KEY_ENDPOINT +
                    API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SecurityKey getAppSignatureKey(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long connectionId) {

        checkPrivileges(institutionId, examId);
        return this.securityKeyService
                .getAppSignatureKey(institutionId, connectionId)
                .getOrThrow();

    }

    private Exam checkPrivileges(final Long institutionId, final Long examId) {
        // check overall privilege
        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER, UserRole.TEACHER,
                UserRole.EXAM_ADMIN);

        // check exam running
        final Exam runningExam = this.examSessionService.getRunningExam(examId).getOr(null);
        if (runningExam == null) {
            throw new ExamNotRunningException("Exam not currently running: " + examId);
        }

        // check running exam privilege for specified exam
        if (!hasRunningExamPrivilege(runningExam, institutionId)) {
            throw new PermissionDeniedException(
                    EntityType.EXAM,
                    PrivilegeType.READ,
                    this.authorization.getUserService().getCurrentUser().getUserInfo());
        }

        return runningExam;
    }

    private boolean hasRunningExamPrivilege(final Exam exam, final Long institution) {
        if (exam == null) {
            return false;
        }

        final UserInfo userInfo = this.authorization.getUserService().getCurrentUser().getUserInfo();
        final String userId = userInfo.uuid;
        return exam.institutionId.equals(institution)
                && (exam.isOwnerOrSupporter(userId) || userInfo.hasRole(UserRole.EXAM_ADMIN));
    }

    private Predicate<ClientConnectionData> noneActiveFilter(final EnumSet<ConnectionStatus> filterStates) {
        return conn -> conn != null && !filterStates.contains(conn.clientConnection.status);
    }

    private Predicate<ClientConnectionData> activeIssueFilterSebVersion(final EnumSet<ConnectionIssueStatus> filterStates) {
        return conn -> BooleanUtils.isFalse(conn.clientConnection.clientVersionGranted);
    }

    private Predicate<ClientConnectionData> activeIssueFilterAsk(final EnumSet<ConnectionIssueStatus> filterStates) {
        return conn -> BooleanUtils.isFalse(conn.clientConnection.securityCheckGranted);
    }

    /** If we have a filter criteria for ACTIVE connection, we shall filter only the active connections
     * that has no incident. */
    private Predicate<ClientConnectionData> withActiveFilter(final EnumSet<ConnectionStatus> filterStates) {
        return conn -> {
            if (conn == null) {
                return false;
            } else if (conn.clientConnection.status == ConnectionStatus.ACTIVE) {
                return conn.hasAnyIncident();
            } else {
                return !filterStates.contains(conn.clientConnection.status);
            }
        };
    }

    private Predicate<ClientConnectionData> createMonitoringFilter(
            final String hiddenStates,
            final String hiddenClientGroups,
            final String hiddenIssues) {

        final EnumSet<ConnectionStatus> filterStates = EnumSet.noneOf(ConnectionStatus.class);
        if (StringUtils.isNotBlank(hiddenStates)) {
            final String[] split = StringUtils.split(hiddenStates, Constants.LIST_SEPARATOR);
            for (final String s : split) {
                filterStates.add(ConnectionStatus.valueOf(s));
            }
        }

        final boolean active = filterStates.contains(ConnectionStatus.ACTIVE);
        if (active) {
            filterStates.remove(ConnectionStatus.ACTIVE);
        }

        Predicate<ClientConnectionData> filter = filterStates.isEmpty()
                ? Objects::nonNull
                : active
                        ? withActiveFilter(filterStates)
                        : noneActiveFilter(filterStates);

        final EnumSet<ConnectionIssueStatus> filterIssues = EnumSet.noneOf(ConnectionIssueStatus.class);
        if (StringUtils.isNotBlank(hiddenIssues)) {
            final String[] split = StringUtils.split(hiddenIssues, Constants.LIST_SEPARATOR);
            for (final String s : split) {
                filterIssues.add(ConnectionIssueStatus.valueOf(s));
            }
        }

        if (filterIssues.contains(ConnectionIssueStatus.SEB_VERSION_GRANTED)) {
            filter = filter.and(activeIssueFilterSebVersion(filterIssues));
        }
        if (filterIssues.contains(ConnectionIssueStatus.ASK_GRANTED)) {
            filter = filter.and(activeIssueFilterAsk(filterIssues));
        }

        Set<Long> filterClientGroups = null;
        if (StringUtils.isNotBlank(hiddenClientGroups)) {
            filterClientGroups = new HashSet<>();
            final String[] split = StringUtils.split(hiddenClientGroups, Constants.LIST_SEPARATOR);
            for (final String s : split) {
                filterClientGroups.add(Long.parseLong(s));
            }
        }

        final Set<Long> _filterClientGroups = filterClientGroups;
        return filter.and(ccd -> ccd.filter(_filterClientGroups));
    }

}
