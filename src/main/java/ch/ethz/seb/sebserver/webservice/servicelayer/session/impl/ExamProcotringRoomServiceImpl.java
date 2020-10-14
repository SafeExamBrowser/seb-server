/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isNotEqualTo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.ProctoringInstructionMethod;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ClientConnectionDAOImpl;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProcotringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBInstructionService;

@Lazy
@Service
@WebServiceProfile
public class ExamProcotringRoomServiceImpl implements ExamProcotringRoomService {

    private static final Logger log = LoggerFactory.getLogger(ExamProcotringRoomServiceImpl.class);

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final SEBInstructionService sebInstructionService;
    private final ExamAdminService examAdminService;
    private final ExamSessionCacheService examSessionCacheService;

    public ExamProcotringRoomServiceImpl(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final SEBInstructionService sebInstructionService,
            final ExamAdminService examAdminService,
            final ExamSessionCacheService examSessionCacheService) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
        this.sebInstructionService = sebInstructionService;
        this.examAdminService = examAdminService;
        this.examSessionCacheService = examSessionCacheService;
    }

    @Override
    public Result<Collection<RemoteProctoringRoom>> getProctoringRooms(final Long examId) {
        return this.remoteProctoringRoomDAO.getRoomsForExam(examId);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> getRoomConnections(final Long roomId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomId, isEqualTo(roomId))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionDAOImpl::toDomainModel)
                .map(result -> result.getOrThrow())
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> getRoomConnections(final Long examId, final String roomName) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .leftJoin(RemoteProctoringRoomRecordDynamicSqlSupport.remoteProctoringRoomRecord)
                .on(RemoteProctoringRoomRecordDynamicSqlSupport.id,
                        SqlBuilder.equalTo(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomId))
                .where(ClientConnectionRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.name, SqlBuilder.isLike(roomName))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionDAOImpl::toDomainModel)
                .map(result -> result.getOrThrow())
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void updateProctoringRooms() {
        // get all client connections that needs a proctoring room update
        final List<ClientConnectionRecord> toUpdate = this.clientConnectionRecordMapper.selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomUpdate, isNotEqualTo(0))
                .build()
                .execute();

        flagUpdated(toUpdate).stream()
                .forEach(cc -> {
                    if (ConnectionStatus.ACTIVE.name().equals(cc.getStatus())) {
                        assignToRoom(cc);
                    } else if (ConnectionStatus.DISABLED.name().equals(cc.getStatus())) {
                        removeFromRoom(cc);
                    }
                });
    }

    // TODO considering doing bulk update here
    private Collection<ClientConnectionRecord> flagUpdated(final Collection<ClientConnectionRecord> toUpdate) {
        return toUpdate.stream().map(cc -> {
            this.clientConnectionRecordMapper.updateByPrimaryKeySelective(new ClientConnectionRecord(
                    cc.getId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0));
            return cc;
        })
                .collect(Collectors.toList());
    }

    private void assignToRoom(final ClientConnectionRecord cc) {
        try {
            final RemoteProctoringRoom proctoringRoom = getProctoringRoom(cc.getExamId(), cc.getConnectionToken());
            if (proctoringRoom != null) {
                this.clientConnectionRecordMapper.updateByPrimaryKeySelective(new ClientConnectionRecord(
                        cc.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        proctoringRoom.id,
                        0));
                this.examSessionCacheService.evictRemoteProctoringRooms(cc.getExamId());
                applyProcotringInstruction(cc.getExamId(), cc.getConnectionToken(), proctoringRoom.name);
            }
        } catch (final Exception e) {
            log.error("Failed to process proctoring room update for client connection: {}", cc.getConnectionToken(), e);
            try {
                this.clientConnectionRecordMapper.updateByPrimaryKey(new ClientConnectionRecord(
                        cc.getId(),
                        cc.getInstitutionId(),
                        cc.getExamId(),
                        cc.getStatus(),
                        cc.getConnectionToken(),
                        cc.getExamUserSessionId(),
                        cc.getClientAddress(),
                        cc.getVirtualClientAddress(),
                        cc.getCreationTime(),
                        null,
                        1));
            } catch (final Exception ee) {
                log.error("Failed to reset update for proctoring room on client connection: {}",
                        cc.getConnectionToken(), e);
            }
        }
    }

    private void removeFromRoom(final ClientConnectionRecord cc) {
        this.remoteProctoringRoomDAO.releasePlaceInRoom(cc.getExamId(), cc.getRemoteProctoringRoomId());
        this.clientConnectionRecordMapper.updateByPrimaryKey(new ClientConnectionRecord(
                cc.getId(),
                cc.getInstitutionId(),
                cc.getExamId(),
                cc.getStatus(),
                cc.getConnectionToken(),
                cc.getExamUserSessionId(),
                cc.getClientAddress(),
                cc.getVirtualClientAddress(),
                cc.getCreationTime(),
                null,
                0));
    }

    private RemoteProctoringRoom getProctoringRoom(final Long examId, final String connectionToken) {
        try {
            final ProctoringSettings proctoringSettings = this.examAdminService
                    .getExamProctoring(examId)
                    .getOrThrow();
            return this.remoteProctoringRoomDAO.reservePlaceInRoom(
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

    private void applyProcotringInstruction(
            final Long examId,
            final String connectionToken,
            final String roomName) {

        try {
            // apply a SEB_PROCOTIRNG instruction for the specified SEB client connection
            final ProctoringSettings proctoringSettings = this.examAdminService
                    .getExamProctoring(examId)
                    .getOrThrow();

            final SEBProctoringConnectionData proctoringData =
                    this.examAdminService.getExamProctoringService(proctoringSettings.serverType)
                            .flatMap(s -> s.getClientExamCollectionRoomConnectionData(
                                    proctoringSettings,
                                    connectionToken,
                                    roomName))
                            .getOrThrow();

            final Map<String, String> attributes = new HashMap<>();
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.SERVICE_TYPE,
                    ProctoringServerType.JITSI_MEET.name());
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.METHOD,
                    ProctoringInstructionMethod.JOIN.name());

            if (proctoringSettings.serverType == ProctoringServerType.JITSI_MEET) {

                attributes.put(
                        ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_ROOM,
                        proctoringData.roomName);
                attributes.put(
                        ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_URL,
                        proctoringData.serverURL);
                attributes.put(
                        ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_TOKEN,
                        proctoringData.accessToken);
            }

            this.sebInstructionService.registerInstruction(
                    examId,
                    InstructionType.SEB_PROCTORING,
                    attributes,
                    connectionToken,
                    true);

        } catch (final Exception e) {
            log.error(
                    "Failed to process proctoring initialization for established SEB client connection: {}",
                    connectionToken, e);
        }
    }

}
