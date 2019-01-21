/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
public class UserActivityLogDAOImpl implements UserActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogDAOImpl.class);

    private final UserActivityLogRecordMapper userLogRecordMapper;
    private final UserService userService;
    private final PaginationService paginationService;

    public UserActivityLogDAOImpl(
            final UserActivityLogRecordMapper userLogRecordMapper,
            final UserService userService,
            final PaginationService paginationService) {

        this.userLogRecordMapper = userLogRecordMapper;
        this.userService = userService;
        this.paginationService = paginationService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER_ACTIVITY_LOG;
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> log(
            final ActivityType activityType,
            final E entity,
            final String message) {

        return log(this.userService.getCurrentUser(), activityType, entity, message);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> log(final ActivityType activityType, final E entity) {
        return log(this.userService.getCurrentUser(), activityType, entity, null);
    }

    @Override
    @Transactional
    public void log(
            final ActivityType activityType,
            final EntityType entityType,
            final String entityId,
            final String message) {

        try {
            log(
                    this.userService.getCurrentUser(),
                    activityType,
                    entityType,
                    entityId,
                    message);
        } catch (final Exception e) {
            log.error(
                    "Unexpected error while trying to log user activity for user {}, action-type: {} entity-type: {} entity-id: {}",
                    this.userService.getCurrentUser(),
                    activityType,
                    entityType,
                    entityId,
                    e);
            TransactionHandler.rollback();
        }
    }

    @Override
    public <T> Result<T> log(
            final ActivityType activityType,
            final EntityType entityType,
            final String entityId,
            final String message,
            final T data) {

        return Result.tryCatch(() -> {
            log(
                    this.userService.getCurrentUser(),
                    activityType,
                    entityType,
                    entityId,
                    message);
            return data;
        });
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> log(
            final SEBServerUser user,
            final ActivityType activityType,
            final E entity,
            final String message) {

        return Result.tryCatch(() -> {
            log(user, activityType, entity.entityType(), entity.getModelId(), message);
            return entity;
        })
                .onErrorDo(TransactionHandler::rollback)
                .onErrorDo(t -> log.error(
                        "Unexpected error while trying to log user activity for user {}, action-type: {} entity-type: {} entity-id: {}",
                        user.getUserInfo().uuid,
                        activityType,
                        entity.entityType().name(),
                        entity.getModelId(),
                        t));
    }

    private void log(
            final SEBServerUser user,
            final ActivityType activityType,
            final EntityType entityType,
            final String entityId,
            final String message) {

        this.userLogRecordMapper.insertSelective(new UserActivityLogRecord(
                null,
                user.getUserInfo().uuid,
                System.currentTimeMillis(),
                activityType.name(),
                entityType.name(),
                entityId,
                message));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserActivityLog> byPK(final Long id) {
        return Result.tryCatch(() -> this.userLogRecordMapper.selectByPrimaryKey(id))
                .flatMap(UserActivityLogDAOImpl::toDomainModel);
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> delete(final Set<EntityKey> all) {
        final List<Long> ids = extractPKsFromKeys(all);

        try {
            this.userLogRecordMapper.deleteByExample()
                    .where(UserActivityLogRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.USER_ACTIVITY_LOG)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Deletion failed on unexpected exception for UserActivityLog of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> getAllForUser(final String userUuid) {
        return Result.tryCatch(() -> {
            return this.userLogRecordMapper.selectByExample()
                    .where(
                            UserActivityLogRecordDynamicSqlSupport.userUuid,
                            SqlBuilder.isEqualTo(userUuid))
                    .build()
                    .execute()
                    .stream()
                    .map(UserActivityLogDAOImpl::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> all(
            final Long institutionId,
            final String userId,
            final Long from,
            final Long to,
            final Predicate<UserActivityLogRecord> predicate) {

        return Result.tryCatch(() -> {
            final Predicate<UserActivityLogRecord> _predicate = (predicate != null)
                    ? predicate
                    : model -> true;

            return this.userLogRecordMapper.selectByExample()
                    .join(UserRecordDynamicSqlSupport.userRecord)
                    .on(
                            UserRecordDynamicSqlSupport.uuid,
                            SqlBuilder.equalTo(UserActivityLogRecordDynamicSqlSupport.userUuid))
                    .where(
                            UserRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualTo(institutionId))
                    .and(
                            UserActivityLogRecordDynamicSqlSupport.userUuid,
                            SqlBuilder.isEqualToWhenPresent(userId))
                    .and(
                            UserActivityLogRecordDynamicSqlSupport.timestamp,
                            SqlBuilder.isGreaterThanOrEqualToWhenPresent(from))
                    .and(
                            UserActivityLogRecordDynamicSqlSupport.timestamp,
                            SqlBuilder.isLessThanWhenPresent(to))
                    .build()
                    .execute()
                    .stream()
                    .filter(_predicate)
                    .map(UserActivityLogDAOImpl::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .collect(Collectors.toList());

        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> all(final Long institutionId) {

        return Result.tryCatch(() -> {
            // first check if there is a page limitation set. Otherwise set the default
            // to not pollute the memory with log data
            this.paginationService.setDefaultLimitOfNotSet(
                    UserActivityLogRecordDynamicSqlSupport.userActivityLogRecord);

            if (institutionId == null) {
                return this.userLogRecordMapper
                        .selectByExample()
                        .build()
                        .execute()
                        .stream()
                        .map(UserActivityLogDAOImpl::toDomainModel)
                        .flatMap(Result::skipOnError)
                        .collect(Collectors.toList());
            } else {
                return this.userLogRecordMapper
                        .selectByExample()
                        .join(UserRecordDynamicSqlSupport.userRecord)
                        .on(
                                UserRecordDynamicSqlSupport.uuid,
                                SqlBuilder.equalTo(UserActivityLogRecordDynamicSqlSupport.userUuid))
                        .where(
                                UserRecordDynamicSqlSupport.institutionId,
                                SqlBuilder.isEqualTo(institutionId))
                        .build()
                        .execute()
                        .stream()
                        .map(UserActivityLogDAOImpl::toDomainModel)
                        .flatMap(Result::skipOnError)
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> loadEntities(final Collection<EntityKey> keys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional
    public Result<UserActivityLog> save(final UserActivityLog modified) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional
    public Result<Integer> overwriteUserReferences(final String userUuid, final boolean deactivate) {
        return Result.tryCatch(() -> {
            final List<UserActivityLogRecord> records = this.userLogRecordMapper.selectByExample()
                    .where(
                            UserActivityLogRecordDynamicSqlSupport.userUuid,
                            SqlBuilder.isEqualTo(userUuid))
                    .build()
                    .execute();

            if (CollectionUtils.isEmpty(records)) {
                return 0;
            }

            records
                    .stream()
                    .forEach(this::overwriteUser);

            return records.size();
        });
    }

    @Override
    @Transactional
    public Result<Integer> deleteUserEnities(final String userUuid) {
        return Result.tryCatch(() -> {
            return this.userLogRecordMapper.deleteByExample()
                    .where(
                            UserActivityLogRecordDynamicSqlSupport.userUuid,
                            SqlBuilder.isEqualToWhenPresent(userUuid))
                    .build()
                    .execute();
        });
    }

    private void overwriteUser(final UserActivityLogRecord record) {
        final UserActivityLogRecord selective = new UserActivityLogRecord(
                record.getId(),
                this.userService.getAnonymousUser().getUsername(),
                null, null, null, null, null);

        this.userLogRecordMapper.updateByPrimaryKeySelective(selective);
    }

    private static Result<UserActivityLog> toDomainModel(final UserActivityLogRecord record) {
        return Result.tryCatch(() -> {
            return new UserActivityLog(
                    record.getId(),
                    record.getUserUuid(),
                    record.getTimestamp(),
                    ActivityType.valueOf(record.getActivityType()),
                    EntityType.valueOf(record.getEntityType()),
                    record.getEntityId(),
                    record.getMessage());
        });
    }

}
