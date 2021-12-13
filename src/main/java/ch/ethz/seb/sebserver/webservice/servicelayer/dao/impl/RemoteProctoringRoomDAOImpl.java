/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RemoteProctoringRoomRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.NewRoom;

@Lazy
@Component
@WebServiceProfile
public class RemoteProctoringRoomDAOImpl implements RemoteProctoringRoomDAO {

    private static final Logger log = LoggerFactory.getLogger(RemoteProctoringRoomDAOImpl.class);

    private final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final AdditionalAttributesDAO additionalAttributesDAO;

    protected RemoteProctoringRoomDAOImpl(
            final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper,
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final AdditionalAttributesDAO additionalAttributesDAO) {

        this.remoteProctoringRoomRecordMapper = remoteProctoringRoomRecordMapper;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
        this.additionalAttributesDAO = additionalAttributesDAO;
    }

    @Override
    public Result<Boolean> isServiceInUse(final Long examId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper
                .countByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .build()
                .execute() > 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<RemoteProctoringRoom>> getCollectingRooms(final Long examId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper.selectByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isEqualTo(0))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.breakOutConnections, isNull())
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<RemoteProctoringRoom>> getRooms(final Long examId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper.selectByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<RemoteProctoringRoom> getRoom(final Long roomId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper
                .selectByPrimaryKey(roomId))
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<RemoteProctoringRoom> getRoom(final Long examId, final String roomName) {
        return Result.tryCatch(() -> {
            return this.remoteProctoringRoomRecordMapper.selectByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .and(RemoteProctoringRoomRecordDynamicSqlSupport.name, isEqualTo(roomName))
                    .build()
                    .execute()
                    .get(0);
        })
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<String> getRoomName(final Long roomId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper
                .selectByPrimaryKey(roomId))
                .map(record -> record.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<RemoteProctoringRoom> getTownhallRoom(final Long examId) {
        return Result.tryCatch(() -> {
            return this.remoteProctoringRoomRecordMapper.selectByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isNotEqualTo(0))
                    .build()
                    .execute()
                    .get(0);
        })
                .map(this::toDomainModel);
    }

    @Override
    @Transactional
    public Result<RemoteProctoringRoom> createTownhallRoom(
            final Long examId,
            final NewRoom room) {

        return Result.tryCatch(() -> {

            // Check first if town-hall room is not already active
            if (isTownhallRoomActive(examId)) {
                throw new IllegalStateException("Townhall, for exam: " + examId + " already exists");
            }

            final RemoteProctoringRoomRecord townhallRoomRecord = new RemoteProctoringRoomRecord(
                    null,
                    examId,
                    room.name,
                    0,
                    StringUtils.isNotBlank(room.subject) ? room.subject : room.name,
                    1,
                    null,
                    (room.joinKey != null) ? room.joinKey.toString() : null,
                    room.additionalRoomData);

            this.remoteProctoringRoomRecordMapper.insert(townhallRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(townhallRoomRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTownhallRoomActive(final Long examId) {
        try {
            final long active = this.remoteProctoringRoomRecordMapper.countByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isNotEqualTo(0))
                    .build()
                    .execute();
            return (active > 0);
        } catch (final Exception e) {
            log.error(
                    "Failed to verify town-hall room activity for exam: {}. Mark it as active to avoid double openings",
                    examId, e);
            return true;
        }
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteTownhallRoom(final Long examId) {
        return getTownhallRoom(examId)
                .map(room -> {
                    this.remoteProctoringRoomRecordMapper.deleteByPrimaryKey(room.id);
                    return new EntityKey(room.id, EntityType.REMOTE_PROCTORING_ROOM);
                })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<RemoteProctoringRoom> createBreakOutRoom(
            final Long examId,
            final NewRoom room,
            final String connectionTokens) {

        return Result.tryCatch(() -> {

            final RemoteProctoringRoomRecord record = new RemoteProctoringRoomRecord(
                    null,
                    examId,
                    room.name,
                    0,
                    StringUtils.isNotBlank(room.subject) ? room.subject : room.name,
                    0,
                    connectionTokens,
                    (room.joinKey != null) ? room.joinKey.toString() : null,
                    room.additionalRoomData);

            this.remoteProctoringRoomRecordMapper.insert(record);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(record.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteRoom(final Long roomId) {
        return Result.tryCatch(() -> {

            this.additionalAttributesDAO
                    .deleteAll(EntityType.REMOTE_PROCTORING_ROOM, roomId);

            this.remoteProctoringRoomRecordMapper
                    .deleteByPrimaryKey(roomId);

            return new EntityKey(roomId, EntityType.REMOTE_PROCTORING_ROOM);
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteRooms(final Long examId) {
        final Result<Collection<EntityKey>> tryCatch = Result.tryCatch(() -> {
            final List<Long> ids = this.remoteProctoringRoomRecordMapper
                    .selectIdsByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute();

            if (ids == null || ids.isEmpty()) {
                log.info("No proctoring rooms found for exam to delete: {}", examId);
                return Collections.emptyList();
            }

            this.remoteProctoringRoomRecordMapper.deleteByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(roomId -> {
                        this.additionalAttributesDAO.deleteAll(
                                EntityType.REMOTE_PROCTORING_ROOM,
                                roomId);
                        return roomId;
                    })
                    .map(roomId -> new EntityKey(
                            String.valueOf(roomId),
                            EntityType.REMOTE_PROCTORING_ROOM))
                    .collect(Collectors.toList());

        });
        return tryCatch.onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<RemoteProctoringRoom> reservePlaceInCollectingRoom(
            final Long examId,
            final int roomMaxSize,
            final Function<Long, Result<NewRoom>> newRoomFunction) {

        return Result.tryCatch(() -> {
            final Optional<RemoteProctoringRoomRecord> room =
                    this.remoteProctoringRoomRecordMapper.selectByExample()
                            .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                            .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isEqualTo(0))
                            .and(RemoteProctoringRoomRecordDynamicSqlSupport.breakOutConnections, isNull())
                            .build()
                            .execute()
                            .stream()
                            .filter(r -> r.getSize() < roomMaxSize)
                            .findFirst();

            if (room.isPresent()) {
                return updateCollectingRoom(room.get());
            } else {
                return createNewCollectingRoom(examId, newRoomFunction);
            }
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<RemoteProctoringRoom> releasePlaceInCollectingRoom(final Long examId, final Long roomId) {
        return Result.tryCatch(() -> {
            final RemoteProctoringRoomRecord record = this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(roomId);

            final int size = record.getSize() - 1;
            if (size < 0) {
                throw new IllegalStateException("Room size mismatch, cannot be negative");
            }

            final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                    record.getId(), null, null,
                    size, null, null, null, null, null);

            this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<RemoteProctoringRoom>> getBreakoutRooms(final String connectionToken) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper
                .selectByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isEqualTo(0))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.breakOutConnections, isLike(connectionToken))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getConnectionsInBreakoutRooms(final Long examId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper
                .selectByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.breakOutConnections, isNotNull())
                .build()
                .execute()
                .stream()
                .flatMap(room -> Arrays.asList(
                        StringUtils.split(
                                room.getBreakOutConnections(),
                                Constants.LIST_SEPARATOR_CHAR))
                        .stream())
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void setCollectingRoomOpenFlag(final Long roomId, final boolean isOpen) {
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.REMOTE_PROCTORING_ROOM,
                roomId,
                RemoteProctoringRoom.ATTR_IS_OPEN,
                BooleanUtils.toStringTrueFalse(isOpen))
                .onError(error -> log.error("Failed to set open flag for proctoring room: {} : {}",
                        roomId,
                        error.getMessage()))
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Long> updateRoomSize(final Long remoteProctoringRoomId) {
        return Result.tryCatch(() -> {
            final Long size = this.clientConnectionRecordMapper
                    .countByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomId,
                            isEqualTo(remoteProctoringRoomId))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.status,
                            isEqualTo(ConnectionStatus.ACTIVE.name()))
                    .build()
                    .execute();

            this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(
                    new RemoteProctoringRoomRecord(
                            remoteProctoringRoomId, null, null,
                            size.intValue(), null, null,
                            null, null, null));

            return size;
        })
                .onError(TransactionHandler::rollback);
    }

    private RemoteProctoringRoom toDomainModel(final RemoteProctoringRoomRecord record) {
        final String breakOutConnections = record.getBreakOutConnections();
        final Collection<String> connections = StringUtils.isNotBlank(breakOutConnections)
                ? Arrays.asList(StringUtils.split(breakOutConnections, Constants.LIST_SEPARATOR_CHAR))
                : Collections.emptyList();

        return new RemoteProctoringRoom(
                record.getId(),
                record.getExamId(),
                record.getName(),
                record.getSize(),
                record.getSubject(),
                BooleanUtils.toBooleanObject(record.getTownhallRoom()),
                connections,
                record.getJoinKey(),
                record.getRoomData(),
                isOpen(record));
    }

    private boolean isOpen(final RemoteProctoringRoomRecord record) {
        if (record.getTownhallRoom() != 0 || !StringUtils.isBlank(record.getBreakOutConnections())) {
            return false;
        } else {
            return BooleanUtils.toBoolean(this.additionalAttributesDAO
                    .getAdditionalAttribute(
                            EntityType.REMOTE_PROCTORING_ROOM,
                            record.getId(),
                            RemoteProctoringRoom.ATTR_IS_OPEN)
                    .map(rec -> rec.getValue())
                    .getOrElse(() -> Constants.FALSE_STRING));
        }
    }

    private RemoteProctoringRoomRecord createNewCollectingRoom(
            final Long examId,
            final Function<Long, Result<NewRoom>> newRoomFunction) {

        final Long roomNumber = this.remoteProctoringRoomRecordMapper.countByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isEqualTo(0))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.breakOutConnections, isNull())
                .build()
                .execute();

        final NewRoom newRoom = newRoomFunction
                .apply(roomNumber)
                .getOrThrow();

        final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                null,
                examId,
                newRoom.name,
                1,
                StringUtils.isNotBlank(newRoom.subject) ? newRoom.subject : newRoom.name,
                0,
                null,
                (newRoom.joinKey != null) ? newRoom.joinKey.toString() : null,
                newRoom.additionalRoomData);

        this.remoteProctoringRoomRecordMapper.insert(remoteProctoringRoomRecord);
        return remoteProctoringRoomRecord;
    }

    private RemoteProctoringRoomRecord updateCollectingRoom(final RemoteProctoringRoomRecord room) {
        final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                room.getId(), null, null,
                room.getSize() + 1, null, null, null, null, null);

        this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
        return this.remoteProctoringRoomRecordMapper
                .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
    }

}
