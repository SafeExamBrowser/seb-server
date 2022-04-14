/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_PROCTORING_ENDPOINT)
@SecurityRequirement(name = "oauth2")
public class ExamProctoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringController.class);

    private final ExamProctoringRoomService examProcotringRoomService;
    private final ExamAdminService examAdminService;
    private final AuthorizationService authorizationService;
    private final ExamSessionService examSessionService;

    public ExamProctoringController(
            final ExamProctoringRoomService examProcotringRoomService,
            final ExamAdminService examAdminService,
            final AuthorizationService authorizationService,
            final ExamSessionService examSessionService) {

        this.examProcotringRoomService = examProcotringRoomService;
        this.examAdminService = examAdminService;
        this.authorizationService = authorizationService;
        this.examSessionService = examSessionService;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users insitutionId as default.
     *
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorizationService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_COLLECTING_ROOMS_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<RemoteProctoringRoom> getCollectingRoomsOfExam(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkAccess(institutionId, examId);
        return this.examProcotringRoomService
                .getProctoringCollectingRooms(examId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,

            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProctoringRoomConnection getProctorRoomConnection(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = ProctoringRoomConnection.ATTR_ROOM_NAME, required = true) final String roomName,
            @RequestParam(name = ProctoringRoomConnection.ATTR_SUBJECT, required = false) final String subject) {

        checkAccess(institutionId, examId);
        return this.examSessionService.getRunningExam(examId)
                .flatMap(this.authorizationService::checkRead)
                .flatMap(exam -> this.examAdminService.getExamProctoringService(exam.id))
                .flatMap(service -> service.getProctorRoomConnection(
                        this.examAdminService.getProctoringServiceSettings(examId).getOrThrow(),
                        roomName,
                        StringUtils.isNoneBlank(subject) ? subject : roomName))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_NOTIFY_OPEN_ROOM_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void notifyProctoringRoomOpened(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = ProctoringRoomConnection.ATTR_ROOM_NAME, required = true) final String roomName) {

        checkAccess(institutionId, examId);
        this.examSessionService.getRunningExam(examId)
                .flatMap(this.authorizationService::checkRead)
                .flatMap(exam -> this.examProcotringRoomService.notifyRoomOpened(exam.id, roomName))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_ROOM_CONNECTIONS_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ClientConnection> getAllClientConnectionsInRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = Domain.REMOTE_PROCTORING_ROOM.ATTR_ID,
                    required = true) final String roomName) {

        checkAccess(institutionId, examId);
        return this.examProcotringRoomService
                .getCollectingRoomConnections(examId, roomName)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_RECONFIGURATION_ATTRIBUTES,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void sendReconfigurationAttributes(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = Domain.REMOTE_PROCTORING_ROOM.ATTR_ID,
                    required = false) final String roomName,
            @RequestParam(
                    name = API.EXAM_PROCTORING_ATTR_RECEIVE_AUDIO,
                    required = false,
                    defaultValue = "false") final String sendReceiveAudio,
            @RequestParam(
                    name = API.EXAM_PROCTORING_ATTR_RECEIVE_VIDEO,
                    required = false,
                    defaultValue = "false") final String sendReceiveVideo,
            @RequestParam(
                    name = API.EXAM_PROCTORING_ATTR_ALLOW_CHAT,
                    required = false,
                    defaultValue = "false") final String sendAllowChat) {

        checkAccess(institutionId, examId);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put(
                API.EXAM_PROCTORING_ATTR_RECEIVE_AUDIO,
                sendReceiveAudio);
        attributes.put(
                API.EXAM_PROCTORING_ATTR_RECEIVE_VIDEO,
                sendReceiveVideo);
        attributes.put(
                API.EXAM_PROCTORING_ATTR_ALLOW_CHAT,
                sendAllowChat);

        this.examProcotringRoomService
                .sendReconfigurationInstructions(examId, roomName, attributes)
                .onError(error -> log.error("Failed to send remote proctoring instructions: {}",
                        attributes,
                        error));
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_OPEN_BREAK_OUT_ROOM_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProctoringRoomConnection openBreakOutRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = ProctoringRoomConnection.ATTR_SUBJECT,
                    required = false) final String subject,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = true) final String connectionTokens) {

        checkAccess(institutionId, examId);
        return this.examProcotringRoomService
                .createBreakOutRoom(examId, subject, connectionTokens)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_CLOSE_ROOM_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProctoringRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = ProctoringRoomConnection.ATTR_ROOM_NAME,
                    required = true) final String roomName) {

        checkAccess(institutionId, examId);
        this.examProcotringRoomService
                .closeProctoringRoom(examId, roomName)
                .onError(error -> log.error("Failed to close remote proctoring break out room {}", roomName, error));
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_TOWNHALL_ROOM_AVAILABLE,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String isTownhallRoomAvialbale(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkExamReadAccess(institutionId);
        return String.valueOf(!this.examProcotringRoomService.isTownhallRoomActive(examId));
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_TOWNHALL_ROOM_DATA,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public RemoteProctoringRoom getTownhallRoom(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkExamReadAccess(institutionId);
        return this.examProcotringRoomService
                .getTownhallRoomData(examId)
                .getOrElse(() -> RemoteProctoringRoom.NULL_ROOM);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_ACTIVATE_TOWNHALL_ROOM,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProctoringRoomConnection activateTownhall(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = ProctoringRoomConnection.ATTR_SUBJECT,
                    required = false) final String subject) {

        checkAccess(institutionId, examId);
        return this.examProcotringRoomService
                .openTownhallRoom(examId, subject)
                .onError(error -> this.examProcotringRoomService.closeTownhallRoom(examId)
                        .onError(err -> log.error("Failed to close town-hall after failed opening: {}",
                                err.getMessage())))
                .getOrThrow();
    }

    private void checkExamReadAccess(final Long institutionId) {
        this.authorizationService.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);
    }

    private void checkAccess(final Long institutionId, final Long examId) {
        this.authorizationService.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        this.authorizationService.checkRead(this.examSessionService
                .getExamDAO()
                .examGrantEntityByPK(examId)
                .getOrThrow());
    }

}
