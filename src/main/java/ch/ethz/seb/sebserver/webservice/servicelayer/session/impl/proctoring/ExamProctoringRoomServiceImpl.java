/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@Lazy
@Service
@WebServiceProfile
public class ExamProctoringRoomServiceImpl implements ExamProctoringRoomService {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringRoomServiceImpl.class);

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamAdminService examAdminService;
    private final ExamSessionService examSessionService;
    private final SEBClientInstructionService sebInstructionService;

    public ExamProctoringRoomServiceImpl(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamAdminService examAdminService,
            final ExamSessionService examSessionService,
            final SEBClientInstructionService sebInstructionService) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.examAdminService = examAdminService;
        this.examSessionService = examSessionService;
        this.sebInstructionService = sebInstructionService;
    }

    @Override
    public Result<Collection<RemoteProctoringRoom>> getProctoringCollectingRooms(final Long examId) {
        return this.remoteProctoringRoomDAO.getCollectingRooms(examId);
    }

    @Override
    public Result<Collection<ClientConnection>> getRoomConnections(final Long roomId) {
        return this.clientConnectionDAO.getRoomConnections(roomId);
    }

    @Override
    public Result<Collection<ClientConnection>> getCollectingRoomConnections(final Long examId, final String roomName) {
        return this.clientConnectionDAO.getCollectingRoomConnections(examId, roomName);
    }

    @Override
    public void updateProctoringCollectingRooms() {
        try {
            // Applying to collecting room
            this.clientConnectionDAO
                    .getAllConnectionIdsForRoomUpdateActive()
                    .getOrThrow()
                    .stream()
                    .forEach(this::assignToCollectingRoom);

            // Dispose from collecting room
            this.clientConnectionDAO
                    .getAllConnectionIdsForRoomUpdateInactive()
                    .getOrThrow()
                    .stream()
                    .forEach(this::removeFromRoom);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update proctoring collecting rooms: ", e);
        }
    }

    @Override
    public Result<Exam> disposeRoomsForExam(final Exam exam) {
        if (exam.status != ExamStatus.FINISHED) {
            log.warn("The exam has not been finished yet. No proctoring rooms will be deleted: {} / {}",
                    exam.name,
                    exam.externalId);
        }

        return Result.tryCatch(() -> {

            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(exam.id)
                    .getOrThrow();

            this.examAdminService
                    .getExamProctoringService(proctoringSettings.serverType)
                    .flatMap(service -> service.disposeServiceRoomsForExam(exam.id, proctoringSettings))
                    .onError(error -> log.error("Failed to dispose proctoring service rooms for exam: {} / {}",
                            exam.name,
                            exam.externalId,
                            error));

            this.remoteProctoringRoomDAO
                    .deleteRooms(exam.id)
                    .getOrThrow();

            return exam;
        });
    }

    @Override
    public Result<ProctoringRoomConnection> openTownhallRoom(final Long examId, final String subject) {
        if (!this.examSessionService.isExamRunning(examId)) {
            return Result.ofRuntimeError("Exam with id: " + examId + " is not currently running");
        }

        return Result.tryCatch(() -> {

            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            // First create and get the town-hall room for specified exam
            final RemoteProctoringRoom townhallRoom = examProctoringService
                    .newBreakOutRoom(settings, subject)
                    .flatMap(room -> this.remoteProctoringRoomDAO.createTownhallRoom(examId, room))
                    .getOrThrow();

            // Then send a join instruction to all active clients of the exam to join the town-hall
            return this.sendJoinRoomBreakOutInstructions(
                    settings,
                    this.examSessionService
                            .getActiveConnectionTokens(examId)
                            .getOrThrow(),
                    townhallRoom.name,
                    townhallRoom.subject);
        });
    }

    @Override
    public Result<RemoteProctoringRoom> getTownhallRoomData(final Long examId) {
        return this.remoteProctoringRoomDAO.getTownhallRoom(examId);
    }

    @Override
    public Result<ProctoringRoomConnection> createBreakOutRoom(
            final Long examId,
            final String subject,
            final String connectionTokens) {

        return Result.tryCatch(() -> {

            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            final RemoteProctoringRoom breakOutRoom = examProctoringService
                    .newBreakOutRoom(settings, subject)
                    .flatMap(room -> this.remoteProctoringRoomDAO.createBreakOutRoom(examId, room, connectionTokens))
                    .getOrThrow();

            return this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .map(service -> sendJoinRoomBreakOutInstructions(
                            settings,
                            Arrays.asList(StringUtils.split(
                                    connectionTokens,
                                    Constants.LIST_SEPARATOR_CHAR)),
                            breakOutRoom.name,
                            subject))
                    .getOrThrow();
        });
    }

    @Override
    public Result<Void> closeProctoringRoom(final Long examId, final String roomName) {
        return Result.tryCatch(() -> {

            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            // Get room
            final RemoteProctoringRoom remoteProctoringRoom = this.remoteProctoringRoomDAO
                    .getRoom(examId, roomName)
                    .getOrThrow();

            if (!remoteProctoringRoom.breakOutConnections.isEmpty()) {
                closeBreakOutRoom(examId, settings, examProctoringService, remoteProctoringRoom);
            } else if (remoteProctoringRoom.townhallRoom) {
                closeTownhall(examId, settings, examProctoringService);
            } else {
                closeCollectingRoom(examId, roomName, examProctoringService);
            }
        });
    }

    private void assignToCollectingRoom(final ClientConnectionRecord cc) {
        try {

            final RemoteProctoringRoom proctoringRoom = getProctoringRoom(
                    cc.getExamId(),
                    cc.getConnectionToken());

            this.clientConnectionDAO
                    .assignToProctoringRoom(
                            cc.getId(),
                            cc.getConnectionToken(),
                            proctoringRoom.id)
                    .getOrThrow();

            final Result<RemoteProctoringRoom> townhallRoomResult = this.remoteProctoringRoomDAO
                    .getTownhallRoom(cc.getExamId());

            if (townhallRoomResult.hasValue()) {
                final RemoteProctoringRoom townhallRoom = townhallRoomResult.get();
                applyProcotringInstruction(
                        cc.getExamId(),
                        cc.getConnectionToken(),
                        townhallRoom.name,
                        townhallRoom.subject)
                                .getOrThrow();
            } else {
                applyProcotringInstruction(
                        cc.getExamId(),
                        cc.getConnectionToken(),
                        proctoringRoom.name,
                        proctoringRoom.subject)
                                .getOrThrow();
            }
        } catch (final Exception e) {
            log.error("Failed to assign connection to collecting room: {}", cc, e);
        }
    }

    private void removeFromRoom(final ClientConnectionRecord cc) {
        try {

            this.remoteProctoringRoomDAO.releasePlaceInCollectingRoom(
                    cc.getExamId(),
                    cc.getRemoteProctoringRoomId());

            this.clientConnectionDAO
                    .removeFromProctoringRoom(cc.getId(), cc.getConnectionToken())
                    .onError(error -> log.error("Failed to remove client connection form room: ", error))
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to update client connection for proctoring room: ", e);
            this.clientConnectionDAO.setNeedsRoomUpdate(cc.getId());
        }
    }

    private RemoteProctoringRoom getProctoringRoom(final Long examId, final String connectionToken) {
        try {

            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(proctoringSettings.serverType)
                    .getOrThrow();

            return this.remoteProctoringRoomDAO.reservePlaceInCollectingRoom(
                    examId,
                    proctoringSettings.collectingRoomSize,
                    roomNumber -> examProctoringService.newCollectingRoom(
                            proctoringSettings,
                            roomNumber))
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to initialize remote proctoring room for exam: {} and connection: {}",
                    examId,
                    connectionToken,
                    e);
            return null;
        }
    }

    private void closeTownhall(
            final Long examId,
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService) {

        // Get all active connections
        final Collection<String> connectionTokens = this.examSessionService
                .getActiveConnectionTokens(examId)
                .getOrThrow();

        // Send default settings to clients
        this.sendReconfigurationInstructions(
                examId,
                connectionTokens,
                examProctoringService.getDefaultInstructionAttributes());

        // Close and delete town-hall room
        this.remoteProctoringRoomDAO
                .getTownhallRoom(examId)
                .map(RemoteProctoringRoom::getName)
                .flatMap(roomName -> examProctoringService.disposeBreakOutRoom(
                        proctoringSettings,
                        roomName))
                .flatMap(service -> this.remoteProctoringRoomDAO.deleteTownhallRoom(examId))
                .getOrThrow();

        // Send the rejoin to collecting room instruction to all involved clients
        sendJoinCollectingRoomInstructions(
                proctoringSettings,
                connectionTokens,
                examProctoringService);
    }

    private void closeCollectingRoom(
            final Long examId,
            final String roomName,
            final ExamProctoringService examProctoringService) {

        // get all connections of the room
        final List<String> connectionTokens = this.getCollectingRoomConnections(examId, roomName)
                .getOrThrow()
                .stream()
                .map(cc -> cc.connectionToken)
                .collect(Collectors.toList());

        // Send default settings to clients
        this.sendReconfigurationInstructions(
                examId,
                connectionTokens,
                examProctoringService.getDefaultInstructionAttributes());
    }

    private void closeBreakOutRoom(
            final Long examId,
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService,
            final RemoteProctoringRoom remoteProctoringRoom) {

        // Send default settings to clients
        this.sendReconfigurationInstructions(
                examId,
                remoteProctoringRoom.breakOutConnections,
                examProctoringService.getDefaultInstructionAttributes());

        // Delete room on persistent
        this.remoteProctoringRoomDAO
                .deleteRoom(remoteProctoringRoom.id)
                .getOrThrow();

        // Dispose the proctoring room on service side
        examProctoringService
                .disposeBreakOutRoom(proctoringSettings, remoteProctoringRoom.name)
                .getOrThrow();

        // Send join collecting rooms to involving clients
        sendJoinCollectingRoomInstructions(
                proctoringSettings,
                remoteProctoringRoom.breakOutConnections,
                examProctoringService);
    }

    @Override
    public Result<Void> sendReconfigurationInstructions(
            final Long examId,
            final String roomName,
            final Map<String, String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return Result.ofRuntimeError(
                    "Missing reconfigure instruction attributes. Skip sending empty instruction to SEB clients");
        }

        return Result.tryCatch(() -> {

            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            final RemoteProctoringRoom room = this.remoteProctoringRoomDAO
                    .getRoom(examId, roomName)
                    .getOrThrow();

            final List<String> connectionTokens = new ArrayList<>();
            if (room.townhallRoom) {
                connectionTokens.addAll(this.examSessionService
                        .getActiveConnectionTokens(examId)
                        .getOrThrow());
            } else if (!room.breakOutConnections.isEmpty()) {
                connectionTokens.addAll(room.breakOutConnections);
            } else {
                connectionTokens.addAll(this.getCollectingRoomConnections(examId, roomName)
                        .getOrThrow()
                        .stream()
                        .map(cc -> cc.connectionToken)
                        .collect(Collectors.toList()));
            }

            sendReconfigurationInstructions(
                    examId,
                    connectionTokens,
                    examProctoringService.getInstructionAttributes(attributes));
        });
    }

    private void sendReconfigurationInstructions(
            final Long examId,
            final Collection<String> connectionTokens,
            final Map<String, String> attributes) {

        connectionTokens
                .stream()
                .forEach(connectionToken -> {
                    this.sebInstructionService
                            .registerInstruction(
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

    private Result<Void> applyProcotringInstruction(
            final Long examId,
            final String connectionToken,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {
            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            sendJoinCollectingRoomInstructions(
                    settings,
                    Arrays.asList(connectionToken),
                    examProctoringService);
        });
    }

    private ProctoringRoomConnection sendJoinRoomBreakOutInstructions(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final String roomName,
            final String subject) {

        final ExamProctoringService examProctoringService = this.examAdminService
                .getExamProctoringService(proctoringSettings.serverType)
                .getOrThrow();

        clientConnectionTokens
                .stream()
                .forEach(connectionToken -> {
                    final ProctoringRoomConnection proctoringConnection = examProctoringService
                            .getClientRoomConnection(
                                    proctoringSettings,
                                    connectionToken,
                                    examProctoringService.verifyRoomName(roomName, connectionToken),
                                    (StringUtils.isNotBlank(subject)) ? subject : roomName)
                            .onError(error -> log.error(
                                    "Failed to get client room connection data for {} cause: {}",
                                    connectionToken,
                                    error.getMessage()))
                            .get();
                    if (proctoringConnection != null) {
                        sendJoinInstruction(
                                proctoringSettings.examId,
                                connectionToken,
                                proctoringConnection,
                                examProctoringService);
                    }
                });

        return examProctoringService.getProctorRoomConnection(
                proctoringSettings,
                roomName,
                (StringUtils.isNotBlank(subject)) ? subject : roomName)
                .getOrThrow();

    }

    private void sendJoinCollectingRoomInstructions(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final ExamProctoringService examProctoringService) {

        clientConnectionTokens
                .stream()
                .forEach(connectionToken -> sendJoinCollectingRoomInstruction(
                        proctoringSettings,
                        examProctoringService,
                        connectionToken));
    }

    private void sendJoinCollectingRoomInstruction(
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService,
            final String connectionToken) {

        try {
            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();
            final String roomName = this.remoteProctoringRoomDAO
                    .getRoomName(clientConnection.clientConnection.getRemoteProctoringRoomId())
                    .getOrThrow();

            final ProctoringRoomConnection proctoringConnection = examProctoringService
                    .getClientRoomConnection(
                            proctoringSettings,
                            clientConnection.clientConnection.connectionToken,
                            roomName,
                            clientConnection.clientConnection.userSessionId)
                    .getOrThrow();

            sendJoinInstruction(
                    proctoringSettings.examId,
                    clientConnection.clientConnection.connectionToken,
                    proctoringConnection,
                    examProctoringService);
        } catch (final Exception e) {
            log.error("Failed to send proctoring room join instruction to client: {}", connectionToken, e);
        }
    }

    private void sendJoinInstruction(
            final Long examId,
            final String connectionToken,
            final ProctoringRoomConnection proctoringConnection,
            final ExamProctoringService examProctoringService) {

        final Map<String, String> attributes = examProctoringService
                .createJoinInstructionAttributes(proctoringConnection);

        this.sebInstructionService
                .registerInstruction(
                        examId,
                        InstructionType.SEB_PROCTORING,
                        attributes,
                        connectionToken,
                        true)
                .onError(error -> log.error("Failed to send join instruction: {}", connectionToken, error));
    }
}
