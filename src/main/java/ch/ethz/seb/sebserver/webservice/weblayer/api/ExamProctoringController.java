/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
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

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_PROCTORING_ENDPOINT)
public class ExamProctoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringController.class);

    private final ExamProctoringRoomService examProcotringRoomService;
    private final ExamAdminService examAdminService;
    private final SEBClientInstructionService sebInstructionService;
    private final AuthorizationService authorization;
    private final ExamSessionService examSessionService;

    public ExamProctoringController(
            final ExamProctoringRoomService examProcotringRoomService,
            final ExamAdminService examAdminService,
            final SEBClientInstructionService sebInstructionService,
            final AuthorizationService authorization,
            final ExamSessionService examSessionService) {

        this.examProcotringRoomService = examProcotringRoomService;
        this.examAdminService = examAdminService;
        this.sebInstructionService = sebInstructionService;
        this.authorization = authorization;
        this.examSessionService = examSessionService;
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_ROOMS_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<RemoteProctoringRoom> getProcotringCollectingRoomsOfExam(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        return this.examProcotringRoomService
                .getProctoringCollectingRooms(examId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,

            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBProctoringConnection getProctorRoomData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = SEBProctoringConnection.ATTR_ROOM_NAME, required = true) final String roomName,
            @RequestParam(name = SEBProctoringConnection.ATTR_SUBJECT, required = false) final String subject) {

        checkAccess(institutionId, examId);

        return this.examSessionService.getRunningExam(examId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.examAdminService::getExamProctoring)
                .flatMap(proc -> this.examAdminService
                        .getExamProctoringService(proc.serverType)
                        .flatMap(s -> s.createProctorPublicRoomConnection(
                                proc,
                                roomName,
                                StringUtils.isNoneBlank(subject) ? subject : roomName)))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_ROOM_CONNECTIONS_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ClientConnection> getProctorRoomConnectionData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = Domain.REMOTE_PROCTORING_ROOM.ATTR_ID,
                    required = true) final String roomName) {

        checkAccess(institutionId, examId);

        return this.examProcotringRoomService.getRoomConnections(examId, roomName)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_BROADCAST_SEND_ATTRIBUTES,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void sendBroadcastAttributes(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = Domain.REMOTE_PROCTORING_ROOM.ATTR_ID,
                    required = false) final String roomName,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = false) final String connectionTokens,
            @RequestParam(
                    name = ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                    required = false,
                    defaultValue = "false") final String sendReceiveAudio,
            @RequestParam(
                    name = ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_VIDEO,
                    required = false,
                    defaultValue = "false") final String sendReceiveVideo,
            @RequestParam(
                    name = ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_ALLOW_CHAT,
                    required = false,
                    defaultValue = "false") final String sendAllowChat) {

        checkAccess(institutionId, examId);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                sendReceiveAudio);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_VIDEO,
                sendReceiveVideo);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_ALLOW_CHAT,
                sendAllowChat);

        sendBroadcastInstructions(examId, roomName, connectionTokens, attributes);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_REJOIN_COLLECTING_ROOM_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void sendRejoinExamCollectingRoomToClients(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = true) final String connectionTokens) {

        checkAccess(institutionId, examId);

        final ProctoringSettings proctoringSettings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        sendJoinInstructions(connectionTokens, proctoringSettings);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_JOIN_ROOM_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBProctoringConnection sendJoinProctoringRoomToClients(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = SEBProctoringConnection.ATTR_ROOM_NAME,
                    required = true) final String roomName,
            @RequestParam(
                    name = SEBProctoringConnection.ATTR_SUBJECT,
                    required = false) final String subject,
            @RequestParam(
                    name = API.EXAM_API_SEB_CONNECTION_TOKEN,
                    required = true) final String connectionTokens) {

        checkAccess(institutionId, examId);

        final ProctoringSettings settings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(settings.serverType)
                .getOrThrow();

        if (StringUtils.isNotBlank(connectionTokens)) {

            Arrays.asList(connectionTokens.split(Constants.LIST_SEPARATOR))
                    .stream()
                    .forEach(connectionToken -> {
                        final SEBProctoringConnection proctoringConnection =
                                examProctoringService
                                        .getClientRoomConnection(
                                                settings,
                                                connectionToken,
                                                verifyRoomName(roomName, connectionToken),
                                                (StringUtils.isNotBlank(subject)) ? subject : roomName)
                                        .onError(error -> log.error(
                                                "Failed to get client room connection data for {} cause: {}",
                                                connectionToken,
                                                error.getMessage()))
                                        .get();
                        if (proctoringConnection != null) {
                            sendJoinInstruction(settings.examId, connectionToken, proctoringConnection)
                                    .onError(error -> log.error(
                                            "Failed to send proctoring leave instruction to client: {}  cause: {}",
                                            connectionToken,
                                            error.getMessage()));
                        }
                    });
        }

        return examProctoringService.createProctorPublicRoomConnection(
                settings,
                roomName,
                (StringUtils.isNotBlank(subject)) ? subject : roomName)
                .getOrThrow();
    }

    private String verifyRoomName(final String requestedRoomName, final String connectionToken) {
        if (StringUtils.isNotBlank(requestedRoomName)) {
            return requestedRoomName;
        }

        final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        return urlEncoder.encodeToString(
                Utils.toByteArray(connectionToken));
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_TOWNHALL_ROOM_DATA,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public RemoteProctoringRoom getTownhallRoomData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkAccess(institutionId, examId);

        return this.examProcotringRoomService.getTownhallRoomData(examId)
                .getOrElse(() -> RemoteProctoringRoom.NULL_ROOM);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_ACTIVATE_TOWNHALL_ROOM,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBProctoringConnection activateTownhall(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(
                    name = SEBProctoringConnection.ATTR_SUBJECT,
                    required = false) final String subject) {

        checkAccess(institutionId, examId);

        final ProctoringSettings settings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(settings.serverType)
                .getOrThrow();

        // first create and register a room to collect all connection of the exam
        // As long as the room exists new connections will join this room immediately
        // after have been applied to the default collecting room
        final RemoteProctoringRoom townhallRoom = this.examProcotringRoomService.createTownhallRoom(examId, subject)
                .onError(error -> this.examProcotringRoomService.disposeTownhallRoom(examId))
                .getOrThrow();

        // get all active connections for the exam and send the join instruction
        this.examSessionService.getAllActiveConnectionData(examId)
                .getOrThrow()
                .stream()
                .forEach(cc -> {
                    final SEBProctoringConnection data = examProctoringService
                            .getClientRoomConnection(
                                    settings,
                                    cc.clientConnection.connectionToken,
                                    townhallRoom.name,
                                    townhallRoom.subject)
                            .onError(error -> log.error(
                                    "Failed to get client room connection data for {} cause: {}",
                                    cc.clientConnection.connectionToken,
                                    error.getMessage()))
                            .get();
                    if (data != null) {
                        sendJoinInstruction(examId, cc.clientConnection.connectionToken, data)
                                .onError(error -> log.error(
                                        "Failed to send proctoring leave instruction to client: {} ",
                                        cc.clientConnection.connectionToken, error));
                    }
                });

        return examProctoringService.createProctorPublicRoomConnection(
                settings,
                townhallRoom.name,
                townhallRoom.subject)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_PROCTORING_DEACTIVATE_TOWNHALL_ROOM,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void deactivateTownhall(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkAccess(institutionId, examId);

        final ProctoringSettings settings = this.examSessionService
                .getRunningExam(examId)
                .flatMap(this.examAdminService::getExamProctoring)
                .getOrThrow();

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(settings.serverType)
                .getOrThrow();

        // first unregister the current room to collect all connection of the exam
        this.examProcotringRoomService.disposeTownhallRoom(examId);

        // get all active connections for the exam and send the join instruction
        this.examSessionService.getConnectionData(
                examId,
                ExamSessionService.ACTIVE_CONNECTION_DATA_FILTER)
                .getOrThrow()
                .stream()
                .forEach(cc -> {
                    examProctoringService
                            .getClientExamCollectingRoomConnection(
                                    settings,
                                    cc.clientConnection)
                            .flatMap(data -> this.sendJoinInstruction(
                                    examId,
                                    cc.clientConnection.connectionToken,
                                    data))
                            .onError(error -> log.error("Failed to send rejoin for: {} cause: {}",
                                    cc.clientConnection.connectionToken,
                                    error.getMessage()));
                });
    }

    private void sendBroadcastInstructions(
            final Long examId,
            final String roomName,
            final String connectionTokens,
            final Map<String, String> attributes) {

        if (attributes.isEmpty()) {
            log.warn("Missing reconfigure instruction attributes. Skip sending empty instruction to SEB clients");
            return;
        }

        final boolean definedClients = StringUtils.isNotBlank(connectionTokens);
        final boolean inTownhall = this.examProcotringRoomService.getTownhallRoomData(examId).hasValue();
        final boolean roomSpecified = StringUtils.isNotBlank(roomName);

        if (definedClients) {
            sendBroadcastInstructionsToClients(examId, connectionTokens, attributes);
        } else if (inTownhall) {
            sendBroadcastInstructionToClientsInExam(examId, attributes);
        } else if (roomSpecified) {
            sendBroadcastInstructionToClientsInRoom(examId, roomName, attributes);
        } else {
            throw new RuntimeException("API attribute validation error: missing  "
                    + Domain.REMOTE_PROCTORING_ROOM.ATTR_ID + " and/or" +
                    API.EXAM_API_SEB_CONNECTION_TOKEN + " attribute");
        }
    }

    private void sendBroadcastInstructionsToClients(final Long examId, final String connectionTokens,
            final Map<String, String> attributes) {
        final boolean single = connectionTokens.contains(Constants.LIST_SEPARATOR);
        (single
                ? Arrays.asList(StringUtils.split(connectionTokens, Constants.LIST_SEPARATOR))
                : Arrays.asList(connectionTokens))
                        .stream()
                        .forEach(connectionToken -> {
                            this.sebInstructionService.registerInstruction(
                                    examId,
                                    InstructionType.SEB_RECONFIGURE_SETTINGS,
                                    attributes,
                                    connectionToken,
                                    true)
                                    .onError(error -> log.error(
                                            "Failed to register reconfiguring instruction for connection: {}",
                                            connectionToken,
                                            error));

                        });
    }

    private void sendBroadcastInstructionToClientsInExam(final Long examId, final Map<String, String> attributes) {
        this.examSessionService
                .getAllActiveConnectionData(examId)
                .getOrThrow()
                .stream()
                .forEach(connection -> {
                    this.sebInstructionService.registerInstruction(
                            examId,
                            InstructionType.SEB_RECONFIGURE_SETTINGS,
                            attributes,
                            connection.clientConnection.connectionToken,
                            true)
                            .onError(error -> log.error(
                                    "Failed to register reconfiguring instruction for connection: {}",
                                    connection.clientConnection.connectionToken,
                                    error));
                });
    }

    private void sendBroadcastInstructionToClientsInRoom(
            final Long examId,
            final String roomName,
            final Map<String, String> attributes) {

        this.examProcotringRoomService
                .getRoomConnections(examId, roomName)
                .getOrThrow()
                .stream()
                .filter(ExamSessionService.ACTIVE_CONNECTION_FILTER)
                .forEach(connection -> {
                    this.sebInstructionService.registerInstruction(
                            examId,
                            InstructionType.SEB_RECONFIGURE_SETTINGS,
                            attributes,
                            connection.connectionToken,
                            true)
                            .onError(error -> log.error(
                                    "Failed to register reconfiguring instruction for connection: {}",
                                    connection.connectionToken,
                                    error));
                });
    }

    private void sendJoinInstructions(
            final String connectionTokens,
            final ProctoringSettings proctoringSettings) {

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(proctoringSettings.serverType)
                .getOrThrow();

        Arrays.asList(StringUtils.split(connectionTokens, Constants.LIST_SEPARATOR))
                .stream()
                .forEach(connectionToken -> {
                    sendJoinInstructionToClient(proctoringSettings, examProctoringService, connectionToken);
                });
    }

    private void sendJoinInstructionToClient(
            final ProctoringSettings proctoringSettings,
            final ExamProctoringService examProctoringService,
            final String connectionToken) {

        this.examSessionService
                .getConnectionData(connectionToken)
                .flatMap(connection -> examProctoringService.getClientExamCollectingRoomConnection(
                        proctoringSettings,
                        connection.clientConnection))
                .flatMap(data -> this.sendJoinInstruction(
                        proctoringSettings.examId,
                        connectionToken, data))
                .onError(error -> log.error("Failed to send rejoin for: {} cause: {}",
                        connectionToken,
                        error.getMessage()));
    }

    private Result<Void> sendJoinInstruction(
            final Long examId,
            final String connectionToken,
            final SEBProctoringConnection data) {

        final Map<String, String> attributes = new HashMap<>();

        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.SERVICE_TYPE,
                ProctoringSettings.ProctoringServerType.JITSI_MEET.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.METHOD,
                ClientInstruction.ProctoringInstructionMethod.JOIN.name());
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

    private void checkAccess(final Long institutionId, final Long examId) {
        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        this.authorization.checkRead(this.examSessionService
                .getExamDAO()
                .byPK(examId)
                .getOrThrow());
    }

}
