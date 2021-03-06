/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RemoteProctoringRoomRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class RemoteProctoringRoomDAOImpl implements RemoteProctoringRoomDAO {

    private static final Logger log = LoggerFactory.getLogger(RemoteProctoringRoomDAOImpl.class);

    private final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper;

    protected RemoteProctoringRoomDAOImpl(
            final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper) {

        this.remoteProctoringRoomRecordMapper = remoteProctoringRoomRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<RemoteProctoringRoom>> getCollectingRoomsForExam(final Long examId) {
        return Result.tryCatch(() -> this.remoteProctoringRoomRecordMapper.selectByExample()
                .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.townhallRoom, isEqualTo(0))
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
    public Result<RemoteProctoringRoom> createTownhallRoom(final Long examId, final String subject) {
        return Result.tryCatch(() -> {

            // Check first if town-hall room is not already active
            if (isTownhallRoomActive(examId)) {
                throw new IllegalStateException("Townhall, for exam: " + examId + " already exists");
            }

            final String newCollectingRoomName = UUID.randomUUID().toString();
            final RemoteProctoringRoomRecord townhallRoomRecord = new RemoteProctoringRoomRecord(
                    null,
                    examId,
                    newCollectingRoomName,
                    0,
                    StringUtils.isNotBlank(subject) ? subject : newCollectingRoomName,
                    1);

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
    public Result<RemoteProctoringRoom> saveRoom(final Long examId, final RemoteProctoringRoom room) {
        return Result.tryCatch(() -> {
            final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                    room.id,
                    examId,
                    room.name,
                    room.roomSize,
                    room.subject,
                    BooleanUtils.toInteger(room.townhallRoom, 1, 0, 0));

            this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteTownhallRoom(final Long examId) {
        return getTownhallRoom(examId)
                .map(room -> {
                    this.remoteProctoringRoomRecordMapper.deleteByPrimaryKey(room.id);
                    return new EntityKey(room.id, EntityType.REMOTE_PROCTORING_ROOM);
                });
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteRooms(final Long examId) {
        final Result<Collection<EntityKey>> tryCatch = Result.tryCatch(() -> {
            final List<Long> ids = this.remoteProctoringRoomRecordMapper.selectIdsByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute();

            this.remoteProctoringRoomRecordMapper.deleteByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(String.valueOf(id), EntityType.REMOTE_PROCTORING_ROOM))
                    .collect(Collectors.toList());

        });
        return tryCatch.onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public synchronized Result<RemoteProctoringRoom> reservePlaceInCollectingRoom(
            final Long examId,
            final int roomMaxSize,
            final Function<Long, String> newRoomNameFunction,
            final Function<Long, String> newRommSubjectFunction) {

        return Result.tryCatch(() -> {
            final Optional<RemoteProctoringRoomRecord> room = this.remoteProctoringRoomRecordMapper.selectByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute()
                    .stream()
                    .filter(r -> r.getSize() < roomMaxSize)
                    .findFirst();

            if (room.isPresent()) {
                return room.map(r -> {
                    final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                            r.getId(),
                            null,
                            null,
                            r.getSize() + 1,
                            null,
                            null);

                    this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
                    return this.remoteProctoringRoomRecordMapper
                            .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
                }).get();

            } else {
                // create new room
                final Long roomNumber = this.remoteProctoringRoomRecordMapper.countByExample()
                        .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                        .build()
                        .execute();
                final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                        null,
                        examId,
                        newRoomNameFunction.apply(roomNumber),
                        1,
                        newRommSubjectFunction.apply(roomNumber),
                        0);
                this.remoteProctoringRoomRecordMapper.insert(remoteProctoringRoomRecord);
                return remoteProctoringRoomRecord;
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

            final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                    record.getId(),
                    null,
                    null,
                    record.getSize() - 1,
                    null,
                    null);

            this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(remoteProctoringRoomRecord.getId());

        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    private RemoteProctoringRoom toDomainModel(final RemoteProctoringRoomRecord record) {
        return new RemoteProctoringRoom(
                record.getId(),
                record.getExamId(),
                record.getName(),
                record.getSize(),
                record.getSubject(),
                BooleanUtils.toBooleanObject(record.getTownhallRoom()));
    }

}
