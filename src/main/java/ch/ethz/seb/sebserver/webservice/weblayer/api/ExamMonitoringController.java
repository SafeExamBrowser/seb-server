/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_MONITORING_ENDPOINT)
@SecurityRequirement(name = "oauth2")
public class ExamMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamMonitoringController.class);

    private final SEBClientConnectionService sebClientConnectionService;
    private final ExamSessionService examSessionService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final AuthorizationService authorization;
    private final PaginationService paginationService;
    private final SEBClientNotificationService sebClientNotificationService;
    private final ExamProctoringRoomService examProcotringRoomService;
    private final ExamAdminService examAdminService;
    private final Executor executor;

    public ExamMonitoringController(
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientInstructionService sebClientInstructionService,
            final AuthorizationService authorization,
            final PaginationService paginationService,
            final SEBClientNotificationService sebClientNotificationService,
            final ExamProctoringRoomService examProcotringRoomService,
            final ExamAdminService examAdminService,
            @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) final Executor executor) {

        this.sebClientConnectionService = sebClientConnectionService;
        this.examSessionService = sebClientConnectionService.getExamSessionService();
        this.sebClientInstructionService = sebClientInstructionService;
        this.authorization = authorization;
        this.paginationService = paginationService;
        this.sebClientNotificationService = sebClientNotificationService;
        this.examProcotringRoomService = examProcotringRoomService;
        this.examAdminService = examAdminService;
        this.executor = executor;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users insitutionId as default.
     *
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    /** Get a page of all currently running exams
     *
     * GET /{api}/{entity-type-endpoint-name}
     *
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
                UserRole.EXAM_SUPPORTER,
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
     *
     * GET /{api}/{entity-type-endpoint-name}
     *
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
            @RequestHeader(name = API.EXAM_MONITORING_STATE_FILTER, required = false) final String hiddenStates) {

        checkPrivileges(institutionId, examId);

        final EnumSet<ConnectionStatus> filterStates = EnumSet.noneOf(ConnectionStatus.class);
        if (StringUtils.isNoneBlank(hiddenStates)) {
            final String[] split = StringUtils.split(hiddenStates, Constants.LIST_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                filterStates.add(ConnectionStatus.valueOf(split[i]));
            }
        }

        final boolean active = filterStates.contains(ConnectionStatus.ACTIVE);
        if (active) {
            filterStates.remove(ConnectionStatus.ACTIVE);
        }

        return this.examSessionService
                .getMonitoringSEBConnectionsData(
                        examId,
                        filterStates.isEmpty()
                                ? Objects::nonNull
                                : active
                                        ? withActiveFilter(filterStates)
                                        : noneActiveFilter(filterStates))
                .getOrThrow().connections;
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_MONITORING_FULLPAGE,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MonitoringFullPageData getFullpageData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestHeader(name = API.EXAM_MONITORING_STATE_FILTER, required = false) final String hiddenStates) {

        final Exam runningExam = checkPrivileges(institutionId, examId);

        final EnumSet<ConnectionStatus> filterStates = EnumSet.noneOf(ConnectionStatus.class);
        if (StringUtils.isNoneBlank(hiddenStates)) {
            final String[] split = StringUtils.split(hiddenStates, Constants.LIST_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                filterStates.add(ConnectionStatus.valueOf(split[i]));
            }
        }

        final boolean active = filterStates.contains(ConnectionStatus.ACTIVE);
        if (active) {
            filterStates.remove(ConnectionStatus.ACTIVE);
        }

        final MonitoringSEBConnectionData monitoringSEBConnectionData = this.examSessionService
                .getMonitoringSEBConnectionsData(
                        examId,
                        filterStates.isEmpty()
                                ? Objects::nonNull
                                : active
                                        ? withActiveFilter(filterStates)
                                        : noneActiveFilter(filterStates))
                .getOrThrow();

        if (this.examAdminService.isProctoringEnabled(runningExam).getOr(false)) {
            final Collection<RemoteProctoringRoom> proctoringData = this.examProcotringRoomService
                    .getProctoringCollectingRooms(examId)
                    .getOrThrow();

            return new MonitoringFullPageData(
                    examId,
                    monitoringSEBConnectionData,
                    proctoringData);
        } else {
            return new MonitoringFullPageData(
                    examId,
                    monitoringSEBConnectionData,
                    Collections.emptyList());
        }
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
                .getOrThrow();
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

    private Exam checkPrivileges(final Long institutionId, final Long examId) {
        // check overall privilege
        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER,
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
                && (exam.isOwner(userId) || userInfo.hasRole(UserRole.EXAM_ADMIN));
    }

    private Predicate<ClientConnectionData> noneActiveFilter(final EnumSet<ConnectionStatus> filterStates) {
        return conn -> conn != null && !filterStates.contains(conn.clientConnection.status);
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

}
