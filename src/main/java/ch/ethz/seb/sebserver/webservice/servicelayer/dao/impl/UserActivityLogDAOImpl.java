/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.CollectionUtils;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserLogRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
public class UserActivityLogDAOImpl implements UserActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogDAOImpl.class);

    private final UserLogRecordMapper userLogRecordMapper;
    private final UserService currentUserService;

    public UserActivityLogDAOImpl(
            final UserLogRecordMapper userLogRecordMapper,
            final UserService currentUserService) {

        this.userLogRecordMapper = userLogRecordMapper;
        this.currentUserService = currentUserService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER_LOG;
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logUserActivity(
            final SEBServerUser user,
            final ActionType actionType,
            final E entity,
            final String message) {

        try {

            this.userLogRecordMapper.insert(new UserLogRecord(
                    null,
                    user.getUserInfo().uuid,
                    System.currentTimeMillis(),
                    actionType.name(),
                    entity.entityType().name(),
                    entity.getId(),
                    message));

            return Result.of(entity);

        } catch (final Throwable t) {

            log.error(
                    "Unexpected error while trying to log user activity for user {}, action-type: {} entity-type: {} entity-id: {}",
                    user.getUserInfo().uuid,
                    actionType,
                    entity.entityType().name(),
                    entity.getId(),
                    t);
            TransactionInterceptor
                    .currentTransactionStatus()
                    .setRollbackOnly();
            return Result.ofError(t);

        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> getAllForUser(final String userId) {
        return allForUser(userId, model -> true);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allForUser(
            final String userId,
            final Predicate<UserLogRecord> filter) {

        try {

            final List<UserLogRecord> records = this.userLogRecordMapper.selectByExample()
                    .where(UserLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualTo(userId))
                    .build()
                    .execute();

            return getAllFromRecords(filter, records);

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to get all activity logs for user with id: {}", userId);
            return Result.ofError(t);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allBetween(
            final Long from,
            final Long to,
            final Predicate<UserLogRecord> predicate) {

        return allForBetween(null, from, to, predicate);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allForBetween(
            final String userId,
            final Long from,
            final Long to,
            final Predicate<UserLogRecord> predicate) {

        try {

            final List<UserLogRecord> records = this.userLogRecordMapper.selectByExample()
                    .where(UserLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualToWhenPresent(userId))
                    .and(UserLogRecordDynamicSqlSupport.timestamp, SqlBuilder.isGreaterThanOrEqualToWhenPresent(from))
                    .and(UserLogRecordDynamicSqlSupport.timestamp, SqlBuilder.isLessThanWhenPresent(to))
                    .build()
                    .execute();

            return getAllFromRecords(predicate, records);

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to get all activity logs in the time-frame form: {} to: {}", from,
                    to);
            return Result.ofError(t);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Integer> deleteUserReferences(final String userId) {
        try {

            final List<UserLogRecord> records = this.userLogRecordMapper.selectByExample()
                    .where(UserLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualTo(userId))
                    .build()
                    .execute();

            if (CollectionUtils.isEmpty(records)) {
                return Result.of(0);
            }

            records
                    .stream()
                    .forEach(this::overrrideUser);

            return Result.of(records.size());

        } catch (final Throwable t) {
            log.error(
                    "Unexpected error while trying to delete all user references form activity logs for user with id: {}",
                    userId);
            return Result.ofError(t);
        }
    }

    private void overrrideUser(final UserLogRecord record) {
        final UserLogRecord selective = new UserLogRecord(
                record.getId(),
                this.currentUserService.getAnonymousUser().getUsername(),
                null, null, null, null, null);

        this.userLogRecordMapper.updateByPrimaryKeySelective(selective);
    }

    @Override
    public Result<Integer> deleteUserEnities(final String userId) {
        try {

            return Result.of(this.userLogRecordMapper.deleteByExample()
                    .where(UserLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualToWhenPresent(userId))
                    .build()
                    .execute());

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to delete all activity logs for user with id: {}", userId);
            return Result.ofError(t);
        }
    }

    private Result<Collection<UserActivityLog>> getAllFromRecords(
            final Predicate<UserLogRecord> predicate,
            final List<UserLogRecord> records) {

        if (CollectionUtils.isEmpty(records)) {
            return Result.of(Collections.emptyList());
        }

        return Result.of(records.stream()
                .filter(predicate)
                .flatMap(record -> fromRecord(record).stream())
                .collect(Collectors.toList()));
    }

    private Result<UserActivityLog> fromRecord(final UserLogRecord record) {
        try {

            return Result.of(new UserActivityLog(
                    record.getId(),
                    record.getUserUuid(),
                    record.getTimestamp(),
                    ActionType.valueOf(record.getActionType()),
                    EntityType.valueOf(record.getEntityType()),
                    record.getEntityId(),
                    record.getMessage()));

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to convert UserLogRecord to UserActivityLog: ", t);
            return Result.ofError(t);
        }
    }

}
