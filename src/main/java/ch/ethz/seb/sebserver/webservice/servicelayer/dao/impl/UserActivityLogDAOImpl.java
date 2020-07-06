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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
@WebServiceProfile
public class UserActivityLogDAOImpl implements UserActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogDAOImpl.class);

    private final UserActivityLogRecordMapper userLogRecordMapper;
    private final UserRecordMapper userRecordMapper;
    private final UserService userService;
    private final JSONMapper jsonMapper;

    public UserActivityLogDAOImpl(
            final UserActivityLogRecordMapper userLogRecordMapper,
            final UserRecordMapper userRecordMapper,
            final UserService userService,
            final JSONMapper jsonMapper) {

        this.userLogRecordMapper = userLogRecordMapper;
        this.userRecordMapper = userRecordMapper;
        this.userService = userService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER_ACTIVITY_LOG;
    }

    @Override
    public Result<UserInfo> logLogin(final UserInfo user) {
        return log(UserLogActivityType.LOGIN, user);
    }

    @Override
    public Result<UserInfo> logLogout(final UserInfo user) {
        return log(UserLogActivityType.LOGOUT, user);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logCreate(final E entity) {
        return log(UserLogActivityType.CREATE, entity);
    }

    @Override
    @Transactional
    public Result<UserAccount> logRegisterAccount(final UserAccount account) {
        return Result.tryCatch(() -> {

            this.userLogRecordMapper.insertSelective(new UserActivityLogRecord(
                    null,
                    account.getModelId(),
                    System.currentTimeMillis(),
                    UserLogActivityType.REGISTER.name(),
                    EntityType.USER.name(),
                    account.getModelId(),
                    toMessage(account)));

            return account;
        });
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logSaveToHistory(final E entity) {
        return log(
                UserLogActivityType.MODIFY,
                entity,
                "SEB Exam Configuration : Save To History : " + toMessage(entity));
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logUndo(final E entity) {
        return log(
                UserLogActivityType.MODIFY,
                entity,
                "SEB Exam Configuration : Undo : " + toMessage(entity));
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logImport(final E entity) {
        return log(UserLogActivityType.IMPORT, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logExport(final E entity) {
        return log(UserLogActivityType.EXPORT, entity);
    }

    @Override
    @Transactional
    public <E extends Entity> Result<E> logModify(final E entity) {
        return log(UserLogActivityType.MODIFY, entity);
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
        return log(this.userService.getCurrentUser(), activityType, entity, toMessage(entity));
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
            String _message = message;
            if (message == null) {
                _message = "Entity details: " + entity;
            }

            log(user, activityType, entity.entityType(), entity.getModelId(), _message);
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
                .flatMap(this::toDomainModel);
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
        return Result.tryCatch(() -> this.toDomainModel(
                this.userService.getCurrentUser().institutionId(),
                this.userLogRecordMapper.selectByExample()
                        .where(
                                UserActivityLogRecordDynamicSqlSupport.userUuid,
                                SqlBuilder.isEqualTo(userUuid))
                        .build()
                        .execute()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allMatching(
            final FilterMap filterMap,
            final Predicate<UserActivityLog> predicate) {

        return all(
                filterMap.getInstitutionId(),
                filterMap.getSQLWildcard(UserActivityLog.FILTER_ATTR_USER_NAME),
                filterMap.getUserLogFrom(),
                filterMap.getUserLofTo(),
                filterMap.getString(UserActivityLog.FILTER_ATTR_ACTIVITY_TYPES),
                filterMap.getString(UserActivityLog.FILTER_ATTR_ENTITY_TYPES),
                predicate);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> all(
            final Long institutionId,
            final String userName,
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

            final List<UserActivityLogRecord> records = this.userLogRecordMapper
                    .selectByExample()
                    .leftJoin(UserRecordDynamicSqlSupport.userRecord)
                    .on(
                            UserRecordDynamicSqlSupport.uuid,
                            SqlBuilder.equalTo(UserActivityLogRecordDynamicSqlSupport.userUuid))
                    .where(
                            UserRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualToWhenPresent(institutionId))
                    .and(
                            UserRecordDynamicSqlSupport.username,
                            SqlBuilder.isLikeWhenPresent(userName))
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
                    .execute();

            return this.toDomainModel(institutionId, records)
                    .stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserActivityLog>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> this.toDomainModel(
                this.userService.getCurrentUser().institutionId(),
                this.userLogRecordMapper.selectByExample()
                        .where(UserActivityLogRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                        .build()
                        .execute()));
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

            records.forEach(this::overwriteUser);

            return records.size();
        });
    }

    @Override
    @Transactional
    public Result<Integer> deleteUserEntities(final String userUuid) {
        return Result.tryCatch(() -> this.userLogRecordMapper.deleteByExample()
                .where(
                        UserActivityLogRecordDynamicSqlSupport.userUuid,
                        SqlBuilder.isEqualToWhenPresent(userUuid))
                .build()
                .execute());
    }

    private void overwriteUser(final UserActivityLogRecord record) {
        final UserActivityLogRecord selective = new UserActivityLogRecord(
                record.getId(),
                this.userService.getAnonymousUser().getUsername(),
                null, null, null, null, null);

        this.userLogRecordMapper.updateByPrimaryKeySelective(selective);
    }

    private Collection<UserActivityLog> toDomainModel(
            final Long institutionId,
            final List<UserActivityLogRecord> records) {

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        final Set<String> user_uuids = records
                .stream()
                .map(UserActivityLogRecord::getUserUuid)
                .collect(Collectors.toSet());

        final Map<String, String> userMapping = this.userRecordMapper
                .selectByExample()
                .where(
                        UserRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(institutionId))
                .and(
                        UserRecordDynamicSqlSupport.uuid,
                        SqlBuilder.isIn(new ArrayList<>(user_uuids)))
                .build()
                .execute()
                .stream()
                .collect(Collectors.toMap(UserRecord::getUuid, UserRecord::getUsername));

        return records
                .stream()
                .map(record -> new UserActivityLog(
                        record.getId(),
                        record.getUserUuid(),
                        userMapping.get(record.getUserUuid()),
                        record.getTimestamp(),
                        UserLogActivityType.valueOf(record.getActivityType()),
                        EntityType.valueOf(record.getEntityType()),
                        record.getEntityId(),
                        record.getMessage()))
                .collect(Collectors.toList());
    }

    private Result<UserActivityLog> toDomainModel(final UserActivityLogRecord record) {
        return Result.tryCatch(() -> {

            final List<UserRecord> user = this.userRecordMapper.selectByExample()
                    .where(
                            UserRecordDynamicSqlSupport.uuid,
                            SqlBuilder.isEqualTo(record.getUserUuid()))
                    .build()
                    .execute();

            String username = record.getUserUuid();
            if (CollectionUtils.isEmpty(user) || user.size() > 1) {
                log.error("To many user found for user uuid: {}", record.getUserUuid());
            } else {
                username = user.get(0).getUsername();
            }

            return new UserActivityLog(
                    record.getId(),
                    record.getUserUuid(),
                    username,
                    record.getTimestamp(),
                    UserLogActivityType.valueOf(record.getActivityType()),
                    EntityType.valueOf(record.getEntityType()),
                    record.getEntityId(),
                    record.getMessage());
        });
    }

    private String toMessage(final Entity entity) {
        if (entity == null) {
            return Constants.EMPTY_NOTE;
        }

        String entityAsString;
        try {
            entityAsString = entity.getName() + " = " + this.jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(entity.printSecureCopy());
        } catch (final JsonProcessingException e) {
            entityAsString = entity.toString();
        }
        return entityAsString;
    }

}
