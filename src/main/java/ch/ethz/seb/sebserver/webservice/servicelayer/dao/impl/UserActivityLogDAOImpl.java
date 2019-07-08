/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
@WebServiceProfile
public class UserActivityLogDAOImpl implements UserActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogDAOImpl.class);

    private final UserActivityLogRecordMapper userLogRecordMapper;
    private final UserService userService;

    public UserActivityLogDAOImpl(
            final UserActivityLogRecordMapper userLogRecordMapper,
            final UserService userService) {

        this.userLogRecordMapper = userLogRecordMapper;
        this.userService = userService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER_ACTIVITY_LOG;
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logCreate(final E entity) {
        return log(UserLogActivityType.CREATE, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logImport(final E entity) {
        return log(UserLogActivityType.IMPORT, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logModify(final E entity) {
        return log(UserLogActivityType.MODIFY, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logActivate(final E entity) {
        return log(UserLogActivityType.ACTIVATE, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logDeactivate(final E entity) {
        return log(UserLogActivityType.DEACTIVATE, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logDelete(final E entity) {
        return log(UserLogActivityType.DELETE, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> log(
            final UserLogActivityType activityType,
            final E entity,
            final String message) {

        return log(this.userService.getCurrentUser(), activityType, entity, message);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> log(final UserLogActivityType activityType, final E entity) {
        return log(this.userService.getCurrentUser(), activityType, entity, null);
    }

    @Override
    @Transactional
    public void log(
            final UserLogActivityType activityType,
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
            final UserLogActivityType activityType,
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
            final UserLogActivityType activityType,
            final E entity,
            final String message) {

        return Result.tryCatch(() -> {
            log(user, activityType, entity.entityType(), entity.getModelId(), message);
            return entity;
        })
                .onError(TransactionHandler::rollback)
                .onError(t -> log.error(
                        "Unexpected error while trying to log user activity for user {}, action-type: {} entity-type: {} entity-id: {}",
                        user.getUserInfo().uuid,
                        activityType,
                        entity.entityType().name(),
                        entity.getModelId(),
                        t));
    }

    private void log(
            final SEBServerUser user,
            final UserLogActivityType activityType,
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
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            this.userLogRecordMapper.deleteByExample()
                    .where(UserActivityLogRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.USER_ACTIVITY_LOG))
                    .collect(Collectors.toList());
        });
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
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allMatching(final FilterMap filterMap,
            final Predicate<UserActivityLog> predicate) {
        return all(
                filterMap.getInstitutionId(),
                filterMap.getString(UserActivityLog.FILTER_ATTR_USER),
                filterMap.getLong(UserActivityLog.FILTER_ATTR_FROM),
                filterMap.getLong(UserActivityLog.FILTER_ATTR_TO),
                filterMap.getString(UserActivityLog.FILTER_ATTR_ACTIVITY_TYPES),
                filterMap.getString(UserActivityLog.FILTER_ATTR_ENTITY_TYPES),
                predicate);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> all(
            final Long institutionId,
            final String userId,
            final Long from,
            final Long to,
            final String activityTypes,
            final String entityTypes,
            final Predicate<UserActivityLog> predicate) {

        return Result.tryCatch(() -> {
            final List<String> _activityTypes = (activityTypes != null)
                    ? Arrays.asList(StringUtils.split(activityTypes, Constants.LIST_SEPARATOR))
                    : null;
            final List<String> _entityTypes = (entityTypes != null)
                    ? Arrays.asList(StringUtils.split(entityTypes, Constants.LIST_SEPARATOR))
                    : null;

            final Predicate<UserActivityLog> _predicate = (predicate != null)
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
                    .and(
                            UserActivityLogRecordDynamicSqlSupport.activityType,
                            SqlBuilder.isInCaseInsensitiveWhenPresent(_activityTypes))
                    .and(
                            UserActivityLogRecordDynamicSqlSupport.entityType,
                            SqlBuilder.isInCaseInsensitiveWhenPresent(_entityTypes))
                    .build()
                    .execute()
                    .stream()
                    .map(UserActivityLogDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(_predicate)
                    .collect(Collectors.toList());

        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {
            return this.userLogRecordMapper.selectByExample()
                    .where(UserActivityLogRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(UserActivityLogDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<UserActivityLog> save(final UserActivityLog modified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<UserActivityLog> createNew(final UserActivityLog data) {
        return log(
                data.activityType,
                data.entityType,
                data.entityId,
                data.message,
                data);
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
                    UserLogActivityType.valueOf(record.getActivityType()),
                    EntityType.valueOf(record.getEntityType()),
                    record.getEntityId(),
                    record.getMessage());
        });
    }

}
