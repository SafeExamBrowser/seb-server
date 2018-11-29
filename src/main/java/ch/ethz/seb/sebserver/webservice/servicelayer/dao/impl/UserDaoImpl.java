/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isNotEqualTo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.webservice.datalayer.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.webservice.datalayer.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RoleRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@Lazy
@Component
public class UserDaoImpl implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    private final UserRecordMapper userRecordMapper;
    private final RoleRecordMapper roleRecordMapper;

    public UserDaoImpl(
            final UserRecordMapper userRecordMapper,
            final RoleRecordMapper roleRecordMapper) {

        this.userRecordMapper = userRecordMapper;
        this.roleRecordMapper = roleRecordMapper;
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
        return recordByUsername(username)
                .map(rec -> SEBServerUser.fromRecord(
                        rec,
                        getRoles(rec)));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserInfo> allActive() {
        final List<UserRecord> records = this.userRecordMapper
                .selectByExample()
                .where(UserRecordDynamicSqlSupport.active, isNotEqualTo(0))
                .build()
                .execute();
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(record -> UserInfo.fromRecord(record, getRoles(record)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserInfo> all(final Predicate<UserInfo> predicate) {
        final List<UserRecord> records = this.userRecordMapper
                .selectByExample()
                .build()
                .execute();
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(record -> UserInfo.fromRecord(record, getRoles(record)))
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserInfo> all(final UserFilter filter) {
        // TODO Auto-generated method stub
        return null;
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
    public Result<UserInfo> deleteById(final SEBServerUser principal, final Long id) {
        // TODO clarify within discussion about inactivate, archive and delete user related data
        return Result.ofError(new RuntimeException("TODO"));
    }

    @Override
    @Transactional
    public Result<UserInfo> deleteByUsername(final SEBServerUser principal, final String username) {
        return recordByUsername(username)
                .flatMap(record -> deleteById(principal, record.getId()));

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
                            userInfo.username,
                            (changePWD) ? userMod.getNewPassword() : null,
                            userInfo.email,
                            BooleanUtils.toIntegerObject(userInfo.active),
                            userInfo.locale.toLanguageTag(),
                            userInfo.timeZone.getID());

                    this.userRecordMapper.updateByPrimaryKeySelective(newRecord);
                    updateRolesForUser(record.getId(), userInfo.roles);

                    return byId(record.getId());
                });
    }

    private Result<UserInfo> createNewUser(final SEBServerUser principal, final UserMod userMod) {
        final UserInfo userInfo = userMod.getUserInfo();
        if (userInfo.institutionId == null) {
            return Result.ofError(new IllegalArgumentException("The users institution cannot be null"));
        }

        if (userMod.newPasswordMatch()) {
            return Result.ofError(new APIMessageException(ErrorMessage.PASSWORD_MISSMATCH));
        }

        final UserRecord newRecord = new UserRecord(
                null,
                userInfo.institutionId,
                UUID.randomUUID().toString(),
                userInfo.name,
                userInfo.username,
                userMod.getNewPassword(),
                userInfo.email,
                BooleanUtils.toIntegerObject(userInfo.active),
                userInfo.locale.toLanguageTag(),
                userInfo.timeZone.getID());

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

    private Result<UserRecord> recordByUsername(final String username) {
        return Utils.getSingle(
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.userName, isEqualTo(username))
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
            Result.ofError(new ResourceNotFoundException(
                    Domain.USER.ENITIY_NAME,
                    String.valueOf(nameId)));
        }

        return Result.of(UserInfo.fromRecord(record, getRoles(record)));
    }

    private List<RoleRecord> getRoles(final UserRecord record) {
        final List<RoleRecord> roles = this.roleRecordMapper.selectByExample()
                .where(RoleRecordDynamicSqlSupport.userId, isEqualTo(record.getId()))
                .build()
                .execute();
        return roles;
    }

}
