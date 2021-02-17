/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamProctoringRoomServiceImpl implements ExamProctoringRoomService {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringRoomServiceImpl.class);

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamAdminService examAdminService;
    private final ExamSessionService examSessionService;

    public ExamProctoringRoomServiceImpl(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamAdminService examAdminService,
            final ExamSessionService examSessionService) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.examAdminService = examAdminService;
        this.examSessionService = examSessionService;
    }

    @Override
    public Result<Collection<RemoteProctoringRoom>> getProctoringCollectingRooms(final Long examId) {
        return this.remoteProctoringRoomDAO.getCollectingRoomsForExam(examId);
    }

    @Override
    public Result<Collection<ClientConnection>> getRoomConnections(final Long roomId) {
        return this.clientConnectionDAO.getRoomConnections(roomId);
    }

    @Override
    public Result<Collection<ClientConnection>> getRoomConnections(final Long examId, final String roomName) {
        return this.clientConnectionDAO.getRoomConnections(examId, roomName);
    }

    @Override
    public void updateProctoringCollectingRooms() {
        try {
            this.clientConnectionDAO.getAllConnectionIdsForRoomUpdateActive()
                    .getOrThrow()
                    .stream()
                    .forEach(this::assignToCollectingRoom);

            this.clientConnectionDAO.getAllConnectionIdsForRoomUpdateInactive()
                    .getOrThrow()
                    .stream()
                    .forEach(this::removeFromRoom);
        } catch (final Exception e) {
            log.error("Unexpected error while trying to update proctoring collecting rooms: ", e);
        }
    }

    @Override
    public Result<RemoteProctoringRoom> createTownhallRoom(final Long examId, final String subject) {
        if (!this.examSessionService.isExamRunning(examId)) {
            return Result.ofRuntimeError("Exam with id: " + examId + " is not currently running");
        }

        return this.remoteProctoringRoomDAO.createTownhallRoom(examId, subject);
    }

    @Override
    public Result<RemoteProctoringRoom> getTownhallRoomData(final Long examId) {
        return this.remoteProctoringRoomDAO.getTownhallRoom(examId);
    }

    @Override
    public void disposeTownhallRoom(final Long examId) {
        this.remoteProctoringRoomDAO.deleteTownhallRoom(examId);
    }

    private void assignToCollectingRoom(final ClientConnectionRecord cc) {
        try {

            final RemoteProctoringRoom proctoringRoom = getProctoringRoom(
                    cc.getExamId(),
                    cc.getConnectionToken());

            this.clientConnectionDAO.assignToProctoringRoom(
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
            this.remoteProctoringRoomDAO.releasePlaceInCollectingRoom(cc.getExamId(), cc.getRemoteProctoringRoomId());
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
            final ProctoringSettings proctoringSettings = this.examAdminService
                    .getExamProctoringSettings(examId)
                    .getOrThrow();
            return this.remoteProctoringRoomDAO.reservePlaceInCollectingRoom(
                    examId,
                    proctoringSettings.collectingRoomSize,
                    num -> UUID.randomUUID().toString(),
                    num -> "Room " + (num + 1))
                    .getOrThrow();
        } catch (final Exception e) {
            log.error("Failed to initialize remote proctoring room for exam: {} and connection: {}",
                    examId,
                    connectionToken,
                    e);
            return null;
        }
    }

    private Result<Void> applyProcotringInstruction(
            final Long examId,
            final String connectionToken,
            final String roomName,
            final String subject) {

        return this.examAdminService
                .getExamProctoringSettings(examId)
                .flatMap(proctoringSettings -> this.examAdminService
                        .getExamProctoringService(proctoringSettings.serverType)
                        .getOrThrow()
                        .sendJoinCollectingRoomToClients(
                                proctoringSettings,
                                Arrays.asList(connectionToken)));

    }

}
