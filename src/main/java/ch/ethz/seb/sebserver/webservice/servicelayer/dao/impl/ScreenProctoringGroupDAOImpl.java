/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.AdditionalAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.AdditionalAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScreenProctoringGroopRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScreenProctoringGroopRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ScreenProctoringGroopRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ScreenProctoringGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ScreenProctoringGroupDAOImpl implements ScreenProctoringGroupDAO {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringGroupDAOImpl.class);

    private final ScreenProctoringGroopRecordMapper screenProctoringGroopRecordMapper;
    private final AdditionalAttributeRecordMapper additionalAttributeRecordMapper;

    public ScreenProctoringGroupDAOImpl(
            final ScreenProctoringGroopRecordMapper screenProctoringGroopRecordMapper,
            final AdditionalAttributeRecordMapper additionalAttributeRecordMapper) {

        this.screenProctoringGroopRecordMapper = screenProctoringGroopRecordMapper;
        this.additionalAttributeRecordMapper = additionalAttributeRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> isServiceInUse(final Long examId) {
        return Result.tryCatch(() -> {
            return this.additionalAttributeRecordMapper.countByExample()
                    .where(
                            AdditionalAttributeRecordDynamicSqlSupport.entityType,
                            SqlBuilder.isEqualTo(EntityType.EXAM.name()))
                    .and(
                            AdditionalAttributeRecordDynamicSqlSupport.entityId,
                            SqlBuilder.isEqualTo(examId))
                    .and(
                            AdditionalAttributeRecordDynamicSqlSupport.name,
                            SqlBuilder.isEqualTo(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING))
                    .and(
                            AdditionalAttributeRecordDynamicSqlSupport.value,
                            SqlBuilder.isEqualTo(String.valueOf(Boolean.TRUE)))
                    .build().execute() > 0;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScreenProctoringGroup> getScreenProctoringGroup(final Long pk) {
        return Result.tryCatch(() -> this.screenProctoringGroopRecordMapper
                .selectByPrimaryKey(pk))
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScreenProctoringGroup> getScreenProctoringGroup(final String uuid) {
        return Result.tryCatch(() -> {
            return this.screenProctoringGroopRecordMapper.selectByExample()
                    .where(ScreenProctoringGroopRecordDynamicSqlSupport.uuid, isEqualTo(uuid))
                    .build()
                    .execute()
                    .get(0);
        })
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScreenProctoringGroup> getGroupByName(final Long examId, final String groupName) {
        return Result.tryCatch(() -> {
            return this.screenProctoringGroopRecordMapper.selectByExample()
                    .where(ScreenProctoringGroopRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .and(ScreenProctoringGroopRecordDynamicSqlSupport.name, isEqualTo(groupName))
                    .build()
                    .execute()
                    .get(0);
        })
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ScreenProctoringGroup>> getCollectingGroups(final Long examId) {
        return Result.tryCatch(() -> this.screenProctoringGroopRecordMapper
                .selectByExample()
                .where(ScreenProctoringGroopRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ScreenProctoringGroup> reservePlaceInCollectingGroup(final Long examId, final int maxSize) {

        return Result.tryCatch(() -> {
            final Optional<ScreenProctoringGroopRecord> room =
                    this.screenProctoringGroopRecordMapper
                            .selectByExample()
                            .where(ScreenProctoringGroopRecordDynamicSqlSupport.examId, isEqualTo(examId))
                            .build()
                            .execute()
                            .stream()
                            .filter(r -> maxSize <= 0 || r.getSize() < maxSize)
                            .findFirst();

            if (room.isPresent()) {
                return updateCollectingGroup(room.get());
            } else {
                throw new AllGroupsFullException();
            }
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ScreenProctoringGroup> releasePlaceInCollectingGroup(final Long examId, final Long groupId) {
        return Result.tryCatch(() -> {
            final ScreenProctoringGroopRecord record =
                    this.screenProctoringGroopRecordMapper.selectByPrimaryKey(groupId);

            UpdateDSL.updateWithMapper(
                    this.screenProctoringGroopRecordMapper::update,
                    ScreenProctoringGroopRecordDynamicSqlSupport.screenProctoringGroopRecord)
                    .set(ScreenProctoringGroopRecordDynamicSqlSupport.size)
                    .equalTo(record.getSize() - 1)
                    .where(ScreenProctoringGroopRecordDynamicSqlSupport.id, isEqualTo(groupId))
                    .build()
                    .execute();

            return this.screenProctoringGroopRecordMapper.selectByPrimaryKey(groupId);
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ScreenProctoringGroup> createNewGroup(final ScreenProctoringGroup group) {
        return Result.tryCatch(() -> {

            final ScreenProctoringGroopRecord screenProctoringGroopRecord = new ScreenProctoringGroopRecord(
                    null, group.examId, group.uuid, group.name, 0, group.additionalData);

            this.screenProctoringGroopRecordMapper.insert(screenProctoringGroopRecord);

            return this.screenProctoringGroopRecordMapper
                    .selectByPrimaryKey(screenProctoringGroopRecord.getId());
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteRoom(final Long pk) {
        return Result.tryCatch(() -> {

            this.screenProctoringGroopRecordMapper
                    .deleteByPrimaryKey(pk);

            return new EntityKey(pk, EntityType.SCREEN_PROCTORING_GROUP);
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteGroups(final Long examId) {
        final Result<Collection<EntityKey>> tryCatch = Result.tryCatch(() -> {

            final List<Long> ids = this.screenProctoringGroopRecordMapper
                    .selectIdsByExample()
                    .where(ScreenProctoringGroopRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute();

            if (ids == null || ids.isEmpty()) {
                log.info("No screen proctoring groups found for exam to delete: {}", examId);
                return Collections.emptyList();
            }

            this.screenProctoringGroopRecordMapper.deleteByExample()
                    .where(ScreenProctoringGroopRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(roomId -> new EntityKey(
                            String.valueOf(roomId),
                            EntityType.REMOTE_PROCTORING_ROOM))
                    .collect(Collectors.toList());

        });
        return tryCatch.onError(TransactionHandler::rollback);
    }

    private ScreenProctoringGroup toDomainModel(final ScreenProctoringGroopRecord record) {
        return new ScreenProctoringGroup(
                record.getId(),
                record.getExamId(),
                record.getUuid(),
                record.getName(),
                record.getSize(),
                record.getData());
    }

    private ScreenProctoringGroopRecord updateCollectingGroup(
            final ScreenProctoringGroopRecord screenProctoringGroopRecord) {

        final Long id = screenProctoringGroopRecord.getId();

        UpdateDSL.updateWithMapper(
                this.screenProctoringGroopRecordMapper::update,
                ScreenProctoringGroopRecordDynamicSqlSupport.screenProctoringGroopRecord)
                .set(ScreenProctoringGroopRecordDynamicSqlSupport.size)
                .equalTo(screenProctoringGroopRecord.getSize() + 1)
                .where(ScreenProctoringGroopRecordDynamicSqlSupport.id, isEqualTo(id))
                .build()
                .execute();

        return this.screenProctoringGroopRecordMapper.selectByPrimaryKey(id);
    }

    public static final class AllGroupsFullException extends RuntimeException {
        private static final long serialVersionUID = 3283129187819160485L;
    }

}
