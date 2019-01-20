/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.gbl.util.Utils.toSQLWildcard;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.model.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RoleRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@Lazy
@Component
public class UserDaoImpl implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);
    private static final UserFilter ALL_ACTIVE_ONLY_FILTER = new UserFilter(null, null, null, null, true, null);

    private final UserRecordMapper userRecordMapper;
    private final RoleRecordMapper roleRecordMapper;
    private final PasswordEncoder userPasswordEncoder;

    public UserDaoImpl(
            final UserRecordMapper userRecordMapper,
            final RoleRecordMapper roleRecordMapper,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder userPasswordEncoder) {

        this.userRecordMapper = userRecordMapper;
        this.roleRecordMapper = roleRecordMapper;
        this.userPasswordEncoder = userPasswordEncoder;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserInfo> byPK(final Long id) {
        return Result.tryCatch(() -> this.userRecordMapper.selectByPrimaryKey(id))
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserInfo> byModelId(final String modelId) {
        return recordByUUID(modelId)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long> pkForModelId(final String modelId) {
        return recordByUUID(modelId)
                .map(r -> r.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserInfo> byUsername(final String username) {
        return recordByUsername(username)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SEBServerUser> sebServerUserByUsername(final String username) {
        return recordByUsername(username)
                .flatMap(rec -> sebServerUserFromRecord(rec));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> allActive() {
        return all(ALL_ACTIVE_ONLY_FILTER);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> all(final Predicate<UserInfo> predicate, final Boolean active) {
        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserRecord>>> example =
                    this.userRecordMapper.selectByExample();

            final List<UserRecord> records = (active != null)
                    ? example
                            .where(
                                    UserRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : example.build().execute();

            return records.stream()
                    .map(this::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> all(final UserFilter filter, final Predicate<UserInfo> predicate) {
        if (filter == null) {
            return (predicate == null)
                    ? all()
                    : all(predicate);
        }

        return Result.tryCatch(() -> this.userRecordMapper.selectByExample().where(
                UserRecordDynamicSqlSupport.active,
                isEqualToWhenPresent(BooleanUtils.toIntegerObject(filter.active)))
                .and(UserRecordDynamicSqlSupport.institutionId, isEqualToWhenPresent(filter.institutionId))
                .and(UserRecordDynamicSqlSupport.name, isLikeWhenPresent(toSQLWildcard(filter.name)))
                .and(UserRecordDynamicSqlSupport.username, isLikeWhenPresent(toSQLWildcard(filter.username)))
                .and(UserRecordDynamicSqlSupport.email, isLikeWhenPresent(toSQLWildcard(filter.email)))
                .and(UserRecordDynamicSqlSupport.locale, isLikeWhenPresent(filter.locale))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(Result::skipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<UserInfo> save(final UserMod userMod) {
        if (userMod == null) {
            return Result.ofError(new NullPointerException("userMod has null-reference"));
        }

        return (userMod.uuid != null)
                ? updateUser(userMod)
                        .flatMap(this::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback)
                : createNewUser(userMod)
                        .flatMap(this::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        final List<Long> ids = extractPKsFromKeys(all);
        final UserRecord userRecord = new UserRecord(
                null, null, null, null, null, null, null, null, null,
                BooleanUtils.toIntegerObject(active));

        try {
            this.userRecordMapper.updateByExampleSelective(userRecord)
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.USER)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Activation failed on unexpected exception for User of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> delete(final Set<EntityKey> all) {
        final List<Long> ids = extractPKsFromKeys(all);

        try {
            this.userRecordMapper.deleteByExample()
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.USER)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Deletion failed on unexpected exception for User of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EntityKey> getAllUserRelatedData(final String uuid) {

        // TODO get

        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        // all of institution
        if (bulkAction.sourceType == EntityType.INSTITUTION) {
            return getDependencies(bulkAction, this::allIdsOfInstitution);
        }

        return Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> bulkLoadEntities(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            final List<Long> ids = extractPKsFromKeys(keys);

            return this.userRecordMapper.selectByExample()
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .map(res -> res.getOrThrow())
                    .collect(Collectors.toList());
        });
    }

    @Override
    public List<Long> extractPKsFromKeys(final Collection<EntityKey> keys) {
        if (keys == null || keys.isEmpty() || keys.iterator().next().isIdPK) {
            return UserDAO.super.extractPKsFromKeys(keys);
        } else {
            final List<String> uuids = keys.stream()
                    .map(key -> key.modelId)
                    .collect(Collectors.toList());

            try {

                return this.userRecordMapper.selectIdsByExample()
                        .where(UserRecordDynamicSqlSupport.uuid, isIn(uuids))
                        .build()
                        .execute();

            } catch (final Exception e) {
                log.error("Unexpected error: ", e);
                return Collections.emptyList();
            }
        }
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> {
            return this.userRecordMapper.selectIdsByExample()
                    .where(UserRecordDynamicSqlSupport.institutionId,
                            isEqualTo(Long.valueOf(institutionKey.modelId)))
                    .build()
                    .execute()
                    .stream()
                    .map(id -> new EntityKey(id, EntityType.USER))
                    .collect(Collectors.toList());
        });
    }

    private Result<UserRecord> updateUser(final UserMod userMod) {
        return recordByUUID(userMod.uuid)
                .map(record -> {
                    final boolean changePWD = userMod.passwordChangeRequest();
                    if (changePWD && !userMod.newPasswordMatch()) {
                        throw new APIMessageException(ErrorMessage.PASSWORD_MISSMATCH);
                    }

                    final UserRecord newRecord = new UserRecord(
                            record.getId(),
                            null,
                            null,
                            userMod.name,
                            userMod.username,
                            (changePWD) ? this.userPasswordEncoder.encode(userMod.getNewPassword()) : null,
                            userMod.email,
                            userMod.locale.toLanguageTag(),
                            userMod.timeZone.getID(),
                            null);

                    this.userRecordMapper.updateByPrimaryKeySelective(newRecord);
                    updateRolesForUser(record.getId(), userMod.roles);
                    return this.userRecordMapper.selectByPrimaryKey(record.getId());
                });
    }

    private Result<UserRecord> createNewUser(final UserMod userMod) {
        return Result.tryCatch(() -> {

            if (!userMod.newPasswordMatch()) {
                throw new APIMessageException(ErrorMessage.PASSWORD_MISSMATCH);
            }

            final UserRecord newRecord = new UserRecord(
                    null,
                    userMod.institutionId,
                    UUID.randomUUID().toString(),
                    userMod.name,
                    userMod.username,
                    this.userPasswordEncoder.encode(userMod.getNewPassword()),
                    userMod.email,
                    userMod.locale.toLanguageTag(),
                    userMod.timeZone.getID(),
                    BooleanUtils.toInteger(false));

            this.userRecordMapper.insert(newRecord);
            final Long newUserId = newRecord.getId();
            insertRolesForUser(newUserId, userMod.roles);
            return newRecord;

        });
    }

    private void updateRolesForUser(final Long userId, @NotNull final Set<String> roles) {
        // first delete old roles
        this.roleRecordMapper.deleteByExample()
                .where(RoleRecordDynamicSqlSupport.userId, isEqualTo(userId))
                .build()
                .execute();

        insertRolesForUser(userId, roles);
    }

    private void insertRolesForUser(final Long userId, final Set<String> roles) {
        roles.stream()
                .map(roleName -> new RoleRecord(null, userId, roleName))
                .forEach(roleRecord -> this.roleRecordMapper.insert(roleRecord));
    }

    private Result<UserRecord> recordByUsername(final String username) {
        return getSingleResource(
                username,
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserRecordDynamicSqlSupport.active,
                                isEqualToWhenPresent(BooleanUtils.toInteger(true)))
                        .build()
                        .execute());
    }

    private Result<UserRecord> recordByUUID(final String uuid) {
        return getSingleResource(
                uuid,
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.uuid, isEqualTo(uuid))
                        .build()
                        .execute());
    }

    private List<RoleRecord> getRoles(final UserRecord record) {
        final List<RoleRecord> roles = this.roleRecordMapper.selectByExample()
                .where(RoleRecordDynamicSqlSupport.userId, isEqualTo(record.getId()))
                .build()
                .execute();
        return roles;
    }

    private Result<UserInfo> toDomainModel(final UserRecord record) {

        return Result.tryCatch(() -> {

            final List<RoleRecord> roles = getRoles(record);
            Set<String> userRoles = Collections.emptySet();
            if (roles != null) {
                userRoles = roles
                        .stream()
                        .map(r -> r.getRoleName())
                        .collect(Collectors.toSet());
            }

            return new UserInfo(
                    record.getUuid(),
                    record.getInstitutionId(),
                    record.getName(),
                    record.getUsername(),
                    record.getEmail(),
                    BooleanUtils.toBooleanObject(record.getActive()),
                    Locale.forLanguageTag(record.getLocale()),
                    DateTimeZone.forID(record.getTimezone()),
                    userRoles);
        });
    }

    private Result<SEBServerUser> sebServerUserFromRecord(final UserRecord record) {
        return toDomainModel(record)
                .map(userInfo -> new SEBServerUser(
                        record.getId(),
                        userInfo,
                        record.getPassword()));
    }

}
