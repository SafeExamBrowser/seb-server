/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBInstructionService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_MONITORING_ENDPOINT)
public class ExamMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamMonitoringController.class);

    private final SEBClientConnectionService sebClientConnectionService;
    private final ExamSessionService examSessionService;
    private final ExamAdminService examAdminService;
    private final SEBInstructionService sebInstructionService;
    private final AuthorizationService authorization;
    private final PaginationService paginationService;

    public ExamMonitoringController(
            final ExamAdminService examAdminService,
            final SEBClientConnectionService sebClientConnectionService,
            final SEBInstructionService sebInstructionService,
            final AuthorizationService authorization,
            final PaginationService paginationService) {

        this.examAdminService = examAdminService;
        this.sebClientConnectionService = sebClientConnectionService;
        this.examSessionService = sebClientConnectionService.getExamSessionService();
        this.sebInstructionService = sebInstructionService;
        this.authorization = authorization;
        this.paginationService = paginationService;
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
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
                UserRole.EXAM_SUPPORTER);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());

        // if current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, EntityType.EXAM)) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final List<Exam> exams = new ArrayList<>(this.examSessionService
                .getFilteredRunningExams(
                        filterMap,
                        exam -> this.hasRunningExamPrivilege(exam, institutionId))
                .getOrThrow());

        return ExamAdministrationController.buildSortedExamPage(
                this.paginationService.getPageNumber(pageNumber),
                this.paginationService.getPageSize(pageSize),
                sort,
                exams);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Collection<ClientConnectionData> getConnectionData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long examId,
            @RequestHeader(name = API.EXAM_MONITORING_STATE_FILTER, required = false) final String hiddenStates) {

        // check overall privilege
        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER);

        // check running exam privilege for specified exam
        if (!hasRunningExamPrivilege(examId, institutionId)) {
            throw new PermissionDeniedException(
                    EntityType.EXAM,
                    PrivilegeType.READ,
                    this.authorization.getUserService().getCurrentUser().getUserInfo());
        }

        final EnumSet<ConnectionStatus> filterStates = EnumSet.noneOf(ConnectionStatus.class);
        if (StringUtils.isNoneBlank(hiddenStates)) {
            final String[] split = StringUtils.split(hiddenStates, Constants.LIST_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                filterStates.add(ConnectionStatus.valueOf(split[0]));
            }
        }

        return this.examSessionService
                .getConnectionData(
                        examId,
                        filterStates.isEmpty()
                                ? Objects::nonNull
                                : conn -> conn != null && !filterStates.contains(conn.clientConnection.status))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ClientConnectionData getConnectionDataForSingleConnection(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken) {

        // check overall privilege
        this.authorization.checkRole(
                institutionId,
                EntityType.EXAM,
                UserRole.EXAM_SUPPORTER);

        // check running exam privilege for specified exam
        if (!hasRunningExamPrivilege(examId, institutionId)) {
            throw new PermissionDeniedException(
                    EntityType.EXAM,
                    PrivilegeType.READ,
                    this.authorization.getUserService().getCurrentUser().getUserInfo());
        }

        return this.examSessionService
                .getConnectionData(connectionToken)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.EXAM_MONITORING_INSTRUCTION_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void registerInstruction(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long examId,
            @Valid @RequestBody final ClientInstruction clientInstruction) {

        this.sebInstructionService.registerInstruction(clientInstruction);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.EXAM_MONITORING_DISABLE_CONNECTION_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void disableConnection(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long examId,
            @RequestParam(
                    name = Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                    required = true) final String connectionToken) {

        if (connectionToken.contains(Constants.LIST_SEPARATOR)) {
            final String[] tokens = StringUtils.split(connectionToken, Constants.LIST_SEPARATOR);
            for (int i = 0; i < tokens.length; i++) {
                final String token = tokens[i];
                this.sebClientConnectionService.disableConnection(token, institutionId)
                        .onError(error -> log.error("Failed to disable SEB client connection: {}", token));
            }
        } else {
            this.sebClientConnectionService
                    .disableConnection(connectionToken, institutionId)
                    .getOrThrow();
        }

    }

    //***********************************************************************************************
    //**** Proctoring

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.PROCTOR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SEBProctoringConnectionData getProctorSingleRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = false) final String connectionToken,
            @RequestParam(name = SEBProctoringConnectionData.ATTR_ROOM_NAME, required = false) final String roomName) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        if (StringUtils.isNotBlank(connectionToken)) {
            return this.examSessionService.getRunningExam(examId)
                    .flatMap(this.authorization::checkRead)
                    .flatMap(this.examAdminService::getExamProctoring)
                    .flatMap(proc -> this.examAdminService
                            .getExamProctoringService(proc.serverType)
                            .flatMap(s -> s.createProctorPrivateRoomConnection(proc, connectionToken)))
                    .getOrThrow();
        } else if (StringUtils.isNotBlank(roomName)) {

            return this.examSessionService.getRunningExam(examId)
                    .flatMap(this.authorization::checkRead)
                    .flatMap(this.examAdminService::getExamProctoring)
                    .flatMap(proc -> this.examAdminService
                            .getExamProctoringService(proc.serverType)
                            .flatMap(s -> s.createProctorPublicRoomConnection(proc, roomName)))
                    .getOrThrow();
        } else {
            return null;
        }
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.PROCTOR_PATH_SEGMENT
                    + API.PROCTOR_JOIN_ROOM_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SEBProctoringConnectionData> joinProctoringRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = SEBProctoringConnectionData.ATTR_ROOM_NAME,
                    required = true) final String roomName,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = true) final String connectionTokens) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        final ProctoringSettings settings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(settings.serverType)
                .getOrThrow();

        if (StringUtils.isNotBlank(connectionTokens)) {
            return (connectionTokens.contains(Constants.LIST_SEPARATOR)
                    ? Arrays.asList(StringUtils.split(connectionTokens, Constants.LIST_SEPARATOR))
                    : Arrays.asList(connectionTokens))
                            .stream()
                            .map(connectionToken -> {
                                final SEBProctoringConnectionData data = examProctoringService
                                        .createClientPublicRoomConnection(settings, connectionToken, roomName)
                                        .getOrThrow();
                                sendJoinInstruction(examId, connectionToken, data)
                                        .onError(error -> log.error(
                                                "Failed to send proctoring leave instruction to client: {} ",
                                                connectionToken, error));
                                return data;
                            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.PROCTOR_PATH_SEGMENT
                    + API.PROCTOR_LEAVE_ROOM_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SEBProctoringConnectionData> leaveProctoringRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = SEBProctoringConnectionData.ATTR_ROOM_NAME,
                    required = true) final String roomName,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = true) final String connectionTokens) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        final ProctoringSettings settings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(settings.serverType)
                .getOrThrow();

        if (StringUtils.isNotBlank(connectionTokens)) {
            return (connectionTokens.contains(Constants.LIST_SEPARATOR)
                    ? Arrays.asList(StringUtils.split(connectionTokens, Constants.LIST_SEPARATOR))
                    : Arrays.asList(connectionTokens))
                            .stream()
                            .map(connectionToken -> {
                                final SEBProctoringConnectionData data = examProctoringService
                                        .createClientPublicRoomConnection(settings, connectionToken, roomName)
                                        .getOrThrow();

                                sendLeaveInstruction(examId, connectionTokens, data)
                                        .onError(error -> log.error(
                                                "Failed to send proctoring leave instruction for common room to client: {} ",
                                                connectionToken, error));
                                return data;

                            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    //**** Proctoring
    //***********************************************************************************************

    private boolean hasRunningExamPrivilege(final Long examId, final Long institution) {
        return hasRunningExamPrivilege(
                this.examSessionService.getRunningExam(examId).getOr(null),
                institution);
    }

    private boolean hasRunningExamPrivilege(final Exam exam, final Long institution) {
        if (exam == null) {
            return false;
        }

        final String userId = this.authorization.getUserService().getCurrentUser().getUserInfo().uuid;
        return exam.institutionId.equals(institution) && exam.isOwner(userId);
    }

    private Result<Void> sendJoinInstruction(
            final Long examId,
            final String connectionToken,
            final SEBProctoringConnectionData data) {

        return sendProctorInstruction(
                examId,
                connectionToken,
                data,
                ClientInstruction.ProctoringInstructionMethod.JOIN.name());
    }

    private Result<Void> sendLeaveInstruction(
            final Long examId,
            final String connectionToken,
            final SEBProctoringConnectionData data) {

        return sendProctorInstruction(
                examId,
                connectionToken,
                data,
                ClientInstruction.ProctoringInstructionMethod.LEAVE.name());
    }

    private Result<Void> sendProctorInstruction(
            final Long examId,
            final String connectionToken,
            final SEBProctoringConnectionData data,
            final String method) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.SERVICE_TYPE,
                ProctoringSettings.ServerType.JITSI_MEET.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.METHOD,
                method);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_URL,
                data.serverURL);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_ROOM,
                data.roomName);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_TOKEN,
                data.accessToken);
        return this.sebInstructionService.registerInstruction(
                examId,
                InstructionType.SEB_PROCTORING,
                attributes,
                connectionToken,
                true);
    }

}
