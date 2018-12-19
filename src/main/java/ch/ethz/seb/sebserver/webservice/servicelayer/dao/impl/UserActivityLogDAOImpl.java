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
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
public class UserActivityLogDAOImpl implements UserActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogDAOImpl.class);

    private final UserActivityLogRecordMapper userLogRecordMapper;
    private final UserService userService;
    private final AuthorizationGrantService authorizationGrantService;

    public UserActivityLogDAOImpl(
            final UserActivityLogRecordMapper userLogRecordMapper,
            final UserService userService,
            final AuthorizationGrantService authorizationGrantService) {

        this.userLogRecordMapper = userLogRecordMapper;
        this.userService = userService;
        this.authorizationGrantService = authorizationGrantService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER_ACTIVITY_LOG;
    }

    @Override
    public <E extends Entity> Result<E> logUserActivity(
            final ActivityType actionType,
            final E entity,
            final String message) {

        return logUserActivity(this.userService.getCurrentUser(), actionType, entity, message);
    }

    @Override
    public <E extends Entity> Result<E> logUserActivity(final ActivityType actionType, final E entity) {
        return logUserActivity(this.userService.getCurrentUser(), actionType, entity, null);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logUserActivity(
            final SEBServerUser user,
            final ActivityType activityType,
            final E entity,
            final String message) {

        try {

            this.userLogRecordMapper.insertSelective(new UserActivityLogRecord(
                    null,
                    user.getUserInfo().uuid,
                    System.currentTimeMillis(),
                    activityType.name(),
                    entity.entityType().name(),
                    entity.getId(),
                    message));

            return Result.of(entity);

        } catch (final Throwable t) {

            log.error(
                    "Unexpected error while trying to log user activity for user {}, action-type: {} entity-type: {} entity-id: {}",
                    user.getUserInfo().uuid,
                    activityType,
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
    public Result<Collection<UserActivityLog>> getAllForUser(final String userUuid) {
        return all(userUuid, null, null, model -> true);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> all(
            final String userId,
            final Long from,
            final Long to,
            final Predicate<UserActivityLogRecord> predicate) {

        final Predicate<UserActivityLogRecord> _predicate = (predicate != null)
                ? predicate
                : model -> true;

        try {

            final boolean basePrivilege = this.authorizationGrantService.hasBasePrivilege(
                    EntityType.USER_ACTIVITY_LOG,
                    PrivilegeType.READ_ONLY);

            final Long institutionId = (basePrivilege)
                    ? null
                    : this.userService.getCurrentUser().institutionId();

            if (institutionId == null) {

                final List<UserActivityLogRecord> records = this.userLogRecordMapper.selectByExample()
                        .where(UserActivityLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualToWhenPresent(userId))
                        .and(UserActivityLogRecordDynamicSqlSupport.timestamp,
                                SqlBuilder.isGreaterThanOrEqualToWhenPresent(from))
                        .and(UserActivityLogRecordDynamicSqlSupport.timestamp, SqlBuilder.isLessThanWhenPresent(to))
                        .build()
                        .execute();

                return getAllFromRecords(_predicate, records);

            } else {

                final List<UserActivityLogRecord> records = this.userLogRecordMapper.selectByExample()
                        .join(UserRecordDynamicSqlSupport.userRecord)
                        .on(UserRecordDynamicSqlSupport.uuid,
                                SqlBuilder.equalTo(UserActivityLogRecordDynamicSqlSupport.userUuid))
                        .where(UserActivityLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualToWhenPresent(userId))
                        .and(UserRecordDynamicSqlSupport.institutionId, SqlBuilder.isEqualToWhenPresent(institutionId))
                        .and(UserActivityLogRecordDynamicSqlSupport.timestamp,
                                SqlBuilder.isGreaterThanOrEqualToWhenPresent(from))
                        .and(UserActivityLogRecordDynamicSqlSupport.timestamp, SqlBuilder.isLessThanWhenPresent(to))
                        .build()
                        .execute();

                return getAllFromRecords(_predicate, records);

            }

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to get all activity logs in the time-frame form: {} to: {}",
                    from,
                    to);
            return Result.ofError(t);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Integer> overwriteUserReferences(final String userUuid, final boolean deactivate) {
        try {

            final List<UserActivityLogRecord> records = this.userLogRecordMapper.selectByExample()
                    .where(UserActivityLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualTo(userUuid))
                    .build()
                    .execute();

            if (CollectionUtils.isEmpty(records)) {
                return Result.of(0);
            }

            records
                    .stream()
                    .forEach(this::overwriteUser);

            return Result.of(records.size());

        } catch (final Throwable t) {
            log.error(
                    "Unexpected error while trying to delete all user references form activity logs for user with id: {}",
                    userUuid);
            return Result.ofError(t);
        }
    }

    private void overwriteUser(final UserActivityLogRecord record) {
        final UserActivityLogRecord selective = new UserActivityLogRecord(
                record.getId(),
                this.userService.getAnonymousUser().getUsername(),
                null, null, null, null, null);

        this.userLogRecordMapper.updateByPrimaryKeySelective(selective);
    }

    @Override
    public Result<Integer> deleteUserEnities(final String userUuid) {
        try {

            return Result.of(this.userLogRecordMapper.deleteByExample()
                    .where(UserActivityLogRecordDynamicSqlSupport.userUuid, SqlBuilder.isEqualToWhenPresent(userUuid))
                    .build()
                    .execute());

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to delete all activity logs for user with id: {}", userUuid);
            return Result.ofError(t);
        }
    }

    private Result<Collection<UserActivityLog>> getAllFromRecords(
            final Predicate<UserActivityLogRecord> predicate,
            final List<UserActivityLogRecord> records) {

        if (CollectionUtils.isEmpty(records)) {
            return Result.of(Collections.emptyList());
        }

        return Result.of(records.stream()
                .filter(predicate)
                .flatMap(record -> fromRecord(record).stream())
                .collect(Collectors.toList()));
    }

    private Result<UserActivityLog> fromRecord(final UserActivityLogRecord record) {
        try {

            return Result.of(new UserActivityLog(
                    record.getId(),
                    record.getUserUuid(),
                    record.getTimestamp(),
                    ActivityType.valueOf(record.getActivityType()),
                    EntityType.valueOf(record.getEntityType()),
                    record.getEntityId(),
                    record.getMessage()));

        } catch (final Throwable t) {
            log.error("Unexpected error while trying to convert UserActivityLogRecord to UserActivityLog: ", t);
            return Result.ofError(t);
        }
    }

}
