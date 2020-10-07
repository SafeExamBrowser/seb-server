/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

@Lazy
@Component
@WebServiceProfile
public class RemoteProctoringRoomDAOImpl implements RemoteProctoringRoomDAO {

    private final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper;

    protected RemoteProctoringRoomDAOImpl(
            final RemoteProctoringRoomRecordMapper remoteProctoringRoomRecordMapper) {

        super();
        this.remoteProctoringRoomRecordMapper = remoteProctoringRoomRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<RemoteProctoringRoom>> getRoomsForExam(final Long examId) {
        return Result.tryCatch(() -> {
            return this.remoteProctoringRoomRecordMapper.selectByExample()
                    .where(RemoteProctoringRoomRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_PROCTORING_ROOM,
            key = "#examId")
    public Result<RemoteProctoringRoom> createNewRoom(final Long examId, final RemoteProctoringRoom room) {
        return Result.tryCatch(() -> {
            final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                    null,
                    examId,
                    room.name,
                    (room.roomSize != null) ? room.roomSize : 0,
                    room.subject,
                    room.token);

            this.remoteProctoringRoomRecordMapper.insert(remoteProctoringRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_PROCTORING_ROOM,
            key = "#examId")
    public Result<RemoteProctoringRoom> saveRoom(final Long examId, final RemoteProctoringRoom room) {
        return Result.tryCatch(() -> {
            final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord(
                    room.id,
                    examId,
                    room.name,
                    room.roomSize,
                    room.subject,
                    room.token);

            this.remoteProctoringRoomRecordMapper.updateByPrimaryKeySelective(remoteProctoringRoomRecord);
            return this.remoteProctoringRoomRecordMapper
                    .selectByPrimaryKey(remoteProctoringRoomRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_PROCTORING_ROOM,
            key = "#examId")
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
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_PROCTORING_ROOM,
            key = "#examId")
    public Result<RemoteProctoringRoom> reservePlaceInRoom(final Long examId, final int roomMaxSize) {
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
                throw new NoSuchElementException("No free room available");
            }
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_PROCTORING_ROOM,
            key = "#examId")
    public Result<RemoteProctoringRoom> releasePlaceInRoom(final Long examId, final Long roomId) {
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
                record.getToken());
    }

}
