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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@Lazy
@Service
@WebServiceProfile
public class ExamProctoringRoomServiceImpl implements ExamProctoringRoomService {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringRoomServiceImpl.class);

    private static final Object RESERVE_ROOM_LOCK = new Object();

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamAdminService examAdminService;
    private final ProctoringAdminService proctoringAdminService;
    private final ExamSessionService examSessionService;
    private final SEBClientInstructionService sebInstructionService;
    private final boolean sendBroadcastReset;

    public ExamProctoringRoomServiceImpl(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamAdminService examAdminService,
            final ProctoringAdminService proctoringAdminService,
            final ExamSessionService examSessionService,
            final SEBClientInstructionService sebInstructionService,
            @Value("${sebserver.webservice.proctoring.resetBroadcastOnLeav:true}") final boolean sendBroadcastReset) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.examAdminService = examAdminService;
        this.proctoringAdminService = proctoringAdminService;
        this.examSessionService = examSessionService;
        this.sebInstructionService = sebInstructionService;
        this.sendBroadcastReset = sendBroadcastReset;
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
    public Result<Collection<ClientConnection>> getCollectingRoomConnections(
            final Long examId,
            final String roomName) {

        return this.clientConnectionDAO
                .getCollectingRoomConnections(examId, roomName);
    }

    @Override
    public Result<Collection<ClientConnection>> getActiveCollectingRoomConnections(
            final Long examId,
            final String roomName) {

        final Collection<String> currentlyInBreakoutRooms = this.remoteProctoringRoomDAO
                .getConnectionsInBreakoutRooms(examId)
                .getOrElse(() -> Collections.emptyList());

        if (currentlyInBreakoutRooms.isEmpty()) {
            return this.clientConnectionDAO
                    .getCollectingRoomConnections(examId, roomName);
        } else {
            return this.clientConnectionDAO
                    .getCollectingRoomConnections(examId, roomName)
                    .map(connections -> connections
                            .stream()
                            .filter(cc -> !currentlyInBreakoutRooms.contains(cc.connectionToken))
                            .collect(Collectors.toList()));
        }
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

    @EventListener(ExamDeletionEvent.class)
    public void notifyExamDeletionEvent(final ExamDeletionEvent event) {
        event.ids.forEach(examId -> {
            try {

                this.examAdminService
                        .examForPK(examId)
                        .flatMap(this::disposeRoomsForExam)
                        .getOrThrow();

            } catch (final Exception e) {
                log.error("Failed to delete depending proctoring data for exam: {}", examId, e);
            }
        });
    }

    @EventListener
    public void notifyExamFinished(final ExamFinishedEvent event) {

        log.info("ExamFinishedEvent received, process disposeRoomsForExam...");

        disposeRoomsForExam(event.exam)
                .onError(error -> log.error("Failed to dispose rooms for finished exam: {}", event.exam, error));
    }

    @Override
    public Result<Exam> disposeRoomsForExam(final Exam exam) {

        return Result.tryCatch(() -> {

            log.info("Dispose and deleting proctoring rooms for exam: {}", exam.externalId);

            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(exam.id)
                    .getOrThrow();

            this.proctoringAdminService
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

            final ExamProctoringService examProctoringService = this.proctoringAdminService
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

            final ExamProctoringService examProctoringService = this.proctoringAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            final RemoteProctoringRoom breakOutRoom = examProctoringService
                    .newBreakOutRoom(settings, subject)
                    .flatMap(room -> this.remoteProctoringRoomDAO.createBreakOutRoom(examId, room, connectionTokens))
                    .getOrThrow();

            return this.proctoringAdminService
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

            final ExamProctoringService examProctoringService = this.proctoringAdminService
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
                closeCollectingRoom(examId, roomName, settings, examProctoringService);
            }
        });
    }

    private void assignToCollectingRoom(final ClientConnectionRecord cc) {
        synchronized (RESERVE_ROOM_LOCK) {
            try {

                if (cc.getRemoteProctoringRoomId() == null) {

                    final RemoteProctoringRoom proctoringRoom = getProctoringRoom(
                            cc.getExamId(),
                            cc.getConnectionToken());

                    if (log.isDebugEnabled()) {
                        log.debug("Assigning new SEB client to proctoring room: {}, connection: {}",
                                proctoringRoom.id,
                                cc);
                    }

                    this.clientConnectionDAO
                            .assignToProctoringRoom(
                                    cc.getId(),
                                    cc.getConnectionToken(),
                                    proctoringRoom.id)
                            .getOrThrow();

                    applyProcotringInstruction(cc)
                            .getOrThrow();
                }

            } catch (final Exception e) {
                log.error("Failed to assign connection to collecting room: {}", cc, e);
            }
        }
    }

    private void removeFromRoom(final ClientConnectionRecord cc) {
        synchronized (RESERVE_ROOM_LOCK) {
            try {

                this.remoteProctoringRoomDAO.releasePlaceInCollectingRoom(
                        cc.getExamId(),
                        cc.getRemoteProctoringRoomId());

                this.cleanupBreakOutRooms(cc);

                this.clientConnectionDAO
                        .removeFromProctoringRoom(cc.getId(), cc.getConnectionToken())
                        .onError(error -> log.error("Failed to remove client connection from room: ", error))
                        .getOrThrow();

            } catch (final Exception e) {
                log.error("Failed to update client connection for proctoring room: ", e);
                try {
                    this.remoteProctoringRoomDAO.updateRoomSize(cc.getRemoteProctoringRoomId());
                } catch (final Exception ee) {
                    log.error("Failed to update room size: ", ee);
                }
            }
        }
    }

    private RemoteProctoringRoom getProctoringRoom(final Long examId, final String connectionToken) {
        try {

            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.proctoringAdminService
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

    @Override
    public boolean isTownhallRoomActive(final Long examId) {
        return this.remoteProctoringRoomDAO.isTownhallRoomActive(examId);
    }

    @Override
    public Result<EntityKey> closeTownhallRoom(final Long examId) {
        if (isTownhallRoomActive(examId)) {
            return this.remoteProctoringRoomDAO.getTownhallRoom(examId)
                    .flatMap(room -> this.remoteProctoringRoomDAO.deleteRoom(room.id));
        }

        return Result.ofRuntimeError("No active town-hall for exam: " + examId);
    }

    @Override
    public Result<Void> notifyRoomOpened(final Long examId, final String roomName) {
        return Result.tryCatch(() -> {
            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.proctoringAdminService
                    .getExamProctoringService(proctoringSettings.serverType)
                    .getOrThrow();

            final RemoteProctoringRoom room = this.remoteProctoringRoomDAO
                    .getRoom(examId, roomName)
                    .getOrThrow();

            if (room.townhallRoom || !room.breakOutConnections.isEmpty()) {
                examProctoringService
                        .notifyBreakOutRoomOpened(proctoringSettings, room)
                        .getOrThrow();
            } else {
                final Collection<ClientConnection> clientConnections = this
                        .getActiveCollectingRoomConnections(examId, roomName)
                        .getOrThrow();

                examProctoringService
                        .notifyCollectingRoomOpened(proctoringSettings, room, clientConnections)
                        .getOrThrow();

                this.remoteProctoringRoomDAO
                        .setCollectingRoomOpenFlag(room.id, true);
            }
        });
    }

    private void closeTownhall(
            final Long examId,
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService) {

        // Get all active connections
        final Collection<String> connectionTokens = this.examSessionService
                .getActiveConnectionTokens(examId)
                .getOrThrow();

        // Send default settings to clients if fearture is enabled
        if (this.sendBroadcastReset) {
            this.sendReconfigurationInstructions(
                    examId,
                    connectionTokens,
                    examProctoringService.getDefaultReconfigInstructionAttributes());
        }

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
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService) {

        // get all connections of the room
        final List<String> connectionTokens = this.getActiveCollectingRoomConnections(examId, roomName)
                .getOrThrow()
                .stream()
                .map(cc -> cc.connectionToken)
                .collect(Collectors.toList());

        final RemoteProctoringRoom room = this.remoteProctoringRoomDAO
                .getRoom(examId, roomName)
                .onError(error -> log.error("Failed to get room for setting closed: {} {} {}",
                        examId,
                        roomName,
                        error.getMessage()))
                .getOr(null);

        if (room != null) {
            this.remoteProctoringRoomDAO
                    .setCollectingRoomOpenFlag(room.id, false);
        }

        // Send default settings to clients if feature is enabled
        if (this.sendBroadcastReset) {
            this.sendReconfigurationInstructions(
                    examId,
                    connectionTokens,
                    examProctoringService.getDefaultReconfigInstructionAttributes());
        }
    }

    private void cleanupBreakOutRooms(final ClientConnectionRecord cc) {

        // check if there is a break-out room with matching single connection token
        final Collection<RemoteProctoringRoom> roomsToCleanup = this.remoteProctoringRoomDAO
                .getBreakoutRooms(cc.getConnectionToken())
                .getOrThrow();

        roomsToCleanup.stream().forEach(room -> {
            final ExamProctoringService examProctoringService = this.examAdminService
                    .getExamProctoringService(room.examId)
                    .getOrThrow();

            final ProctoringServiceSettings proctoringSettings = this.examAdminService
                    .getProctoringServiceSettings(room.examId)
                    .getOrThrow();

            // Dispose the proctoring room on service side
            examProctoringService
                    .disposeBreakOutRoom(proctoringSettings, room.name)
                    .getOrThrow();

            // Delete room on persistent
            this.remoteProctoringRoomDAO
                    .deleteRoom(room.id)
                    .getOrThrow();
        });
    }

    private void closeBreakOutRoom(
            final Long examId,
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService,
            final RemoteProctoringRoom remoteProctoringRoom) {

        // Send default settings to clients if feature is enabled
        if (this.sendBroadcastReset) {
            this.sendReconfigurationInstructions(
                    examId,
                    remoteProctoringRoom.breakOutConnections,
                    examProctoringService.getDefaultReconfigInstructionAttributes());
        }

        // Dispose the proctoring room on service side
        examProctoringService
                .disposeBreakOutRoom(proctoringSettings, remoteProctoringRoom.name)
                .getOrThrow();

        // Send join collecting rooms to involving clients
        sendJoinCollectingRoomInstructions(
                proctoringSettings,
                remoteProctoringRoom.breakOutConnections,
                examProctoringService);

        // Delete room on persistent
        this.remoteProctoringRoomDAO
                .deleteRoom(remoteProctoringRoom.id)
                .getOrThrow();
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

            final ExamProctoringService examProctoringService = this.proctoringAdminService
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
                connectionTokens.addAll(this.getActiveCollectingRoomConnections(examId, roomName)
                        .getOrThrow()
                        .stream()
                        .map(cc -> cc.connectionToken)
                        .collect(Collectors.toList()));
            }

            sendReconfigurationInstructions(
                    examId,
                    connectionTokens,
                    examProctoringService.mapReconfigInstructionAttributes(attributes));
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

    private Result<Void> applyProcotringInstruction(final ClientConnectionRecord cc) {

        return Result.tryCatch(() -> {
            final Long examId = cc.getExamId();
            final String connectionToken = cc.getConnectionToken();
            final ProctoringServiceSettings settings = this.examAdminService
                    .getProctoringServiceSettings(examId)
                    .getOrThrow();

            final ExamProctoringService examProctoringService = this.proctoringAdminService
                    .getExamProctoringService(settings.serverType)
                    .getOrThrow();

            final Result<RemoteProctoringRoom> townhallRoomResult = this.remoteProctoringRoomDAO
                    .getTownhallRoom(examId);

            if (townhallRoomResult.hasValue()) {

                final RemoteProctoringRoom townhallRoom = townhallRoomResult.get();
                final ProctoringRoomConnection roomConnection = examProctoringService.getClientRoomConnection(
                        settings,
                        connectionToken,
                        townhallRoom.name,
                        townhallRoom.subject)
                        .getOrThrow();

                try {
                    registerJoinInstruction(
                            examId,
                            connectionToken,
                            roomConnection,
                            examProctoringService);
                } catch (final Exception e) {
                    log.error("Failed to send join for town-hall room assignment to connection: {}", cc);
                    this.clientConnectionDAO
                            .markForProctoringUpdate(cc.getId())
                            .onError(error -> log.error("Failed to mark connection for proctoring update: ", error));
                }
            } else {
                try {

                    sendJoinCollectingRoomInstruction(
                            settings,
                            examProctoringService,
                            connectionToken);

                } catch (final Exception e) {
                    log.error("Failed to send join for collecting room assignment to connection: {}", cc);
                    this.clientConnectionDAO
                            .markForProctoringUpdate(cc.getId())
                            .onError(error -> log.error("Failed to mark connection for proctoring update: ", error));
                }

            }
        });
    }

    private ProctoringRoomConnection sendJoinRoomBreakOutInstructions(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final String roomName,
            final String subject) {

        final ExamProctoringService examProctoringService = this.proctoringAdminService
                .getExamProctoringService(proctoringSettings.serverType)
                .getOrThrow();

        clientConnectionTokens
                .stream()
                .forEach(connectionToken -> {
                    try {
                        final ProctoringRoomConnection proctoringConnection = examProctoringService
                                .getClientRoomConnection(
                                        proctoringSettings,
                                        connectionToken,
                                        roomName,
                                        (StringUtils.isNotBlank(subject)) ? subject : roomName)
                                .onError(error -> log.error(
                                        "Failed to get client room connection data for {} cause: {}",
                                        connectionToken,
                                        error.getMessage()))
                                .get();
                        if (proctoringConnection != null) {
                            registerJoinInstruction(
                                    proctoringSettings.examId,
                                    connectionToken,
                                    proctoringConnection,
                                    examProctoringService);
                        }
                    } catch (final Exception e) {
                        log.error("Failed to send join to break-out room: {} connection: {}",
                                roomName,
                                roomName,
                                e);
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
                .forEach(connectionToken -> {
                    try {
                        sendJoinCollectingRoomInstruction(
                                proctoringSettings,
                                examProctoringService,
                                connectionToken);
                    } catch (final Exception e) {
                        log.error(
                                "Failed to send proctoring room join instruction to single client. Skip and proceed with other clients. {}",
                                e.getMessage());
                    }
                });
    }

    private void sendJoinCollectingRoomInstruction(
            final ProctoringServiceSettings proctoringSettings,
            final ExamProctoringService examProctoringService,
            final String connectionToken) {

        try {
            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            final RemoteProctoringRoom remoteProctoringRoom = this.remoteProctoringRoomDAO
                    .getRoom(clientConnection.clientConnection.getRemoteProctoringRoomId())
                    .getOrThrow();

            final ProctoringRoomConnection proctoringConnection = examProctoringService
                    .getClientRoomConnection(
                            proctoringSettings,
                            clientConnection.clientConnection.connectionToken,
                            remoteProctoringRoom.name,
                            remoteProctoringRoom.subject)
                    .getOrThrow();

            registerJoinInstruction(
                    proctoringSettings.examId,
                    clientConnection.clientConnection.connectionToken,
                    proctoringConnection,
                    examProctoringService);

        } catch (final Exception e) {
            log.error("Failed to send proctoring room join instruction to client: {}", connectionToken, e);
            throw e;
        }
    }

    private void registerJoinInstruction(
            final Long examId,
            final String connectionToken,
            final ProctoringRoomConnection proctoringConnection,
            final ExamProctoringService examProctoringService) {

        if (log.isDebugEnabled()) {
            log.debug("Register proctoring join instruction for connection: {}, room: {}",
                    connectionToken,
                    proctoringConnection.roomName);
        }

        final Map<String, String> attributes = examProctoringService
                .createJoinInstructionAttributes(proctoringConnection);

        this.sebInstructionService
                .registerInstruction(
                        examId,
                        InstructionType.SEB_PROCTORING,
                        attributes,
                        connectionToken,
                        true)
                .onError(error -> log.error("Failed to send join instruction: {}", connectionToken, error))
                .getOrThrow();
    }

}
