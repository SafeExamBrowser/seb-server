/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.CollectionUtils;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.webservice.datalayer.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RoleRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@Lazy
@Component
public class UserDaoImpl implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

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
    public Result<UserInfo> byId(final Long id) {
        return toDomainModel(
                String.valueOf(id),
                this.userRecordMapper.selectByPrimaryKey(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserInfo> byUuid(final String uuid) {
        return recordByUUID(uuid)
                .flatMap(rec -> toDomainModel(uuid, rec));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserInfo> byUsername(final String username) {
        return recordByUUID(username)
                .flatMap(rec -> toDomainModel(username, rec));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SEBServerUser> sebServerUserByUsername(final String username) {
        return recordByUsername(username, true)
                .flatMap(rec -> sebServerUserFromRecord(rec));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> allActive() {
        return all(UserFilter.ofActive());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> all(final Predicate<UserInfo> predicate) {
        try {

            final List<UserRecord> records = this.userRecordMapper
                    .selectByExample()
                    .build()
                    .execute();

            if (records == null) {
                return Result.of(Collections.emptyList());
            }

            return fromRecords(records, predicate);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get all users: ", e);
            return Result.ofError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> all(final UserFilter filter) {
        try {

            final List<UserRecord> records = this.userRecordMapper.selectByExample().where(
                    UserRecordDynamicSqlSupport.active,
                    isEqualTo(BooleanUtils.toInteger(filter.active)))
                    .and(UserRecordDynamicSqlSupport.institutionId, isEqualToWhenPresent(filter.institutionId))
                    .and(UserRecordDynamicSqlSupport.name, isLikeWhenPresent(filter.getNameLike()))
                    .and(UserRecordDynamicSqlSupport.userName, isLikeWhenPresent(filter.getUserNameLike()))
                    .and(UserRecordDynamicSqlSupport.email, isLikeWhenPresent(filter.getEmailLike()))
                    .and(UserRecordDynamicSqlSupport.locale, isLikeWhenPresent(filter.locale))
                    .build()
                    .execute();

            return fromRecords(records, record -> true);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get fitered users, filter: {}", filter, e);
            return Result.ofError(e);
        }
    }

    @Override
    @Transactional
    public Result<UserInfo> save(final SEBServerUser principal, final UserMod userMod) {
        if (userMod == null) {
            return Result.ofError(new NullPointerException("userMod has null-reference"));
        }

        try {

            final UserInfo userInfo = userMod.getUserInfo();
            if (userInfo.uuid != null) {
                return updateUser(userMod);
            } else {
                return createNewUser(principal, userMod);
            }

        } catch (final Throwable t) {
            log.error("Unexpected error while saving User data: ", t);
            TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
            return Result.ofError(t);
        }
    }

    @Override
    @Transactional
    public Result<UserInfo> delete(final SEBServerUser principal, final Long id) {
        // TODO clarify within discussion about inactivate, archive and delete user related data
        return Result.ofError(new RuntimeException("TODO"));
    }

    private Result<Collection<UserInfo>> fromRecords(
            final List<UserRecord> records,
            final Predicate<UserInfo> predicate) {

        if (CollectionUtils.isEmpty(records)) {
            return Result.of(Collections.emptyList());
        }

        return Result.of(records.stream()
                .flatMap(record -> fromRecord(record).stream())
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    private Result<UserInfo> updateUser(final UserMod userMod) {
        final UserInfo userInfo = userMod.getUserInfo();
        return recordByUUID(userInfo.uuid)
                .flatMap(record -> {
                    if (record.getInstitutionId().longValue() != userInfo.institutionId.longValue()) {
                        return Result.ofError(new IllegalArgumentException("The users institution cannot be changed"));
                    }

                    final boolean changePWD = userMod.passwordChangeRequest();
                    if (changePWD && !userMod.newPasswordMatch()) {
                        return Result.ofError(new APIMessageException(ErrorMessage.PASSWORD_MISSMATCH));
                    }

                    final UserRecord newRecord = new UserRecord(
                            record.getId(),
                            null,
                            null,
                            userInfo.name,
                            userInfo.userName,
                            (changePWD) ? this.userPasswordEncoder.encode(userMod.getNewPassword()) : null,
                            userInfo.email,
                            userInfo.locale.toLanguageTag(),
                            userInfo.timeZone.getID(),
                            BooleanUtils.toIntegerObject(userInfo.active));

                    this.userRecordMapper.updateByPrimaryKeySelective(newRecord);
                    updateRolesForUser(record.getId(), userInfo.roles);

                    return byId(record.getId());
                });
    }

    private Result<UserInfo> createNewUser(final SEBServerUser principal, final UserMod userMod) {
        final UserInfo userInfo = userMod.getUserInfo();

        if (!userMod.newPasswordMatch()) {
            return Result.ofError(new APIMessageException(ErrorMessage.PASSWORD_MISSMATCH));
        }

        final UserRecord newRecord = new UserRecord(
                null,
                userInfo.institutionId,
                UUID.randomUUID().toString(),
                userInfo.name,
                userInfo.userName,
                this.userPasswordEncoder.encode(userMod.getNewPassword()),
                userInfo.email,
                userInfo.locale.toLanguageTag(),
                userInfo.timeZone.getID(),
                BooleanUtils.toIntegerObject(userInfo.active));

        this.userRecordMapper.insert(newRecord);
        final Long newUserId = newRecord.getId();
        insertRolesForUser(newUserId, userInfo.roles);
        return byId(newUserId);
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

    private Result<UserRecord> recordByUsername(final String username, final Boolean active) {
        return Utils.getSingle(
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.userName, isEqualTo(username))
                        .and(UserRecordDynamicSqlSupport.active, isEqualToWhenPresent(BooleanUtils.toInteger(active)))
                        .build()
                        .execute());
    }

    private Result<UserRecord> recordByUUID(final String uuid) {
        return Utils.getSingle(
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.uuid, isEqualTo(uuid))
                        .build()
                        .execute());
    }

    private Result<UserInfo> toDomainModel(final String nameId, final UserRecord record) {
        if (record == null) {
            return Result.ofError(new ResourceNotFoundException(
                    EntityType.USER,
                    String.valueOf(nameId)));
        }

        return fromRecord(record);
    }

    private List<RoleRecord> getRoles(final UserRecord record) {
        final List<RoleRecord> roles = this.roleRecordMapper.selectByExample()
                .where(RoleRecordDynamicSqlSupport.userId, isEqualTo(record.getId()))
                .build()
                .execute();
        return roles;
    }

    private Result<UserInfo> fromRecord(final UserRecord record) {

        try {

            final List<RoleRecord> roles = getRoles(record);
            Set<String> userRoles = Collections.emptySet();
            if (roles != null) {
                userRoles = roles
                        .stream()
                        .map(r -> r.getRoleName())
                        .collect(Collectors.toSet());
            }

            return Result.of(new UserInfo(
                    record.getUuid(),
                    record.getInstitutionId(),
                    record.getName(),
                    record.getUserName(),
                    record.getEmail(),
                    BooleanUtils.toBooleanObject(record.getActive()),
                    Locale.forLanguageTag(record.getLocale()),
                    DateTimeZone.forID(record.getTimezone()),
                    userRoles));

        } catch (final Exception e) {
            return Result.ofError(e);
        }
    }

    private Result<SEBServerUser> sebServerUserFromRecord(final UserRecord record) {
        return fromRecord(record)
                .map(userInfo -> new SEBServerUser(
                        record.getId(),
                        userInfo,
                        record.getPassword()));
    }
}
