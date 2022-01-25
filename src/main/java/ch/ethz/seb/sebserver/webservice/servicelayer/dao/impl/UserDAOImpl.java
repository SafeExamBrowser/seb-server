/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
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
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RoleRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RoleRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@Lazy
@Component
@WebServiceProfile
public class UserDAOImpl implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDAOImpl.class);

    private final UserRecordMapper userRecordMapper;
    private final RoleRecordMapper roleRecordMapper;
    private final PasswordEncoder userPasswordEncoder;

    public UserDAOImpl(
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
                .map(UserRecord::getId);
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
                .flatMap(this::sebServerUserFromRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SEBServerUser> sebServerAdminByUsername(final String username) {
        return getSingleResource(
                username,
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserRecordDynamicSqlSupport.active,
                                isEqualTo(BooleanUtils.toInteger(true)))
                        .build()
                        .execute())
                                .flatMap(this::sebServerUserFromRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> all(final Long institutionId, final Boolean active) {
        return Result.tryCatch(() -> {

            final List<UserRecord> records = (active != null)
                    ? this.userRecordMapper.selectByExample()
                            .where(
                                    UserRecordDynamicSqlSupport.institutionId,
                                    isEqualToWhenPresent(institutionId))
                            .and(
                                    UserRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : this.userRecordMapper.selectByExample()
                            .where(
                                    UserRecordDynamicSqlSupport.institutionId,
                                    isEqualToWhenPresent(institutionId))
                            .build()
                            .execute();

            return records.stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> allMatching(final FilterMap filterMap, final Predicate<UserInfo> predicate) {
        return Result.tryCatch(() -> {
            final String userRole = filterMap.getUserRole();
            final Predicate<UserInfo> _predicate = (StringUtils.isNotBlank(userRole))
                    ? predicate.and(ui -> ui.roles.contains(userRole))
                    : predicate;

            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserRecord>>>.QueryExpressionWhereBuilder sqlWhereClause =
                    (filterMap.getBoolean(FilterMap.ATTR_ADD_INSITUTION_JOIN))
                            ? this.userRecordMapper
                                    .selectByExample()
                                    .join(InstitutionRecordDynamicSqlSupport.institutionRecord)
                                    .on(InstitutionRecordDynamicSqlSupport.id,
                                            SqlBuilder.equalTo(UserRecordDynamicSqlSupport.institutionId))
                                    .where(
                                            UserRecordDynamicSqlSupport.active,
                                            isEqualToWhenPresent(filterMap.getActiveAsInt()))
                            : this.userRecordMapper
                                    .selectByExample()
                                    .where(
                                            UserRecordDynamicSqlSupport.active,
                                            isEqualToWhenPresent(filterMap.getActiveAsInt()));
            return sqlWhereClause
                    .and(
                            UserRecordDynamicSqlSupport.institutionId,
                            isEqualToWhenPresent(filterMap.getInstitutionId()))
                    .and(
                            UserRecordDynamicSqlSupport.name,
                            isLikeWhenPresent(filterMap.getName()))
                    .and(
                            UserRecordDynamicSqlSupport.surname,
                            isLikeWhenPresent(filterMap.getSurname()))
                    .and(
                            UserRecordDynamicSqlSupport.username,
                            isLikeWhenPresent(filterMap.getUserUsername()))
                    .and(
                            UserRecordDynamicSqlSupport.email,
                            isLikeWhenPresent(filterMap.getUserEmail()))
                    .and(
                            UserRecordDynamicSqlSupport.language,
                            isLikeWhenPresent(filterMap.getUserLanguage()))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(_predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<UserInfo> createNew(final UserMod userMod) {
        return Result.tryCatch(() -> {

            if (!userMod.newPasswordMatch()) {
                throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
            }

            checkUniqueUsername(userMod);
            checkUniqueMailAddress(userMod);

            final UserRecord recordToSave = new UserRecord(
                    null,
                    userMod.institutionId,
                    UUID.randomUUID().toString(),
                    DateTime.now(DateTimeZone.UTC),
                    userMod.name,
                    userMod.surname,
                    userMod.username,
                    this.userPasswordEncoder.encode(userMod.getNewPassword()),
                    userMod.email,
                    userMod.language.toLanguageTag(),
                    userMod.timeZone.getID(),
                    BooleanUtils.toInteger(false));

            this.userRecordMapper.insert(recordToSave);
            final Long newUserPK = recordToSave.getId();
            final UserRecord newRecord = this.userRecordMapper
                    .selectByPrimaryKey(newUserPK);
            insertRolesForUser(newUserPK, userMod.roles);
            return newRecord;

        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<UserInfo> changePassword(final String modelId, final CharSequence newPassword) {
        return recordByUUID(modelId)
                .map(record -> {
                    final UserRecord newRecord = new UserRecord(
                            record.getId(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            this.userPasswordEncoder.encode(newPassword),
                            null,
                            null,
                            null,
                            null);
                    this.userRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.userRecordMapper.selectByPrimaryKey(record.getId());
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<UserInfo> save(final UserInfo userInfo) {
        return recordByUUID(userInfo.uuid)
                .map(record -> {

                    checkUniqueUsername(userInfo);
                    checkUniqueMailAddress(userInfo);

                    final UserRecord newRecord = new UserRecord(
                            record.getId(),
                            null,
                            null,
                            null,
                            userInfo.name,
                            userInfo.surname,
                            userInfo.username,
                            null,
                            (userInfo.email == null) ? "" : userInfo.email,
                            userInfo.language.toLanguageTag(),
                            userInfo.timeZone.getID(),
                            null);

                    this.userRecordMapper.updateByPrimaryKeySelective(newRecord);
                    updateRolesForUser(record.getId(), userInfo.roles);
                    return this.userRecordMapper.selectByPrimaryKey(record.getId());
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            final UserRecord userRecord = new UserRecord(
                    null, null, null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toIntegerObject(active));

            this.userRecordMapper.updateByExampleSelective(userRecord)
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return this.userRecordMapper.selectByExample()
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(record -> new EntityKey(record.getUuid(), EntityType.USER))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(final String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return false;
        }

        return this.userRecordMapper.countByExample()
                .where(UserRecordDynamicSqlSupport.id, isEqualTo(Long.valueOf(modelId)))
                .and(UserRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute() > 0;
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // get all user records for later processing
            final List<UserRecord> users = this.userRecordMapper.selectByExample()
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            // first delete assigned user roles
            this.roleRecordMapper.deleteByExample()
                    .where(RoleRecordDynamicSqlSupport.userId, isIn(ids))
                    .build()
                    .execute();

            // then delete the user account
            this.userRecordMapper.deleteByExample()
                    .where(UserRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return users.stream()
                    .map(rec -> new EntityKey(rec.getUuid(), EntityType.USER))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EntityKey> getAllUserRelatedData(final String uuid) {

        // TODO get

        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // all of institution
        if (bulkAction.sourceType == EntityType.INSTITUTION &&
                (bulkAction.type == BulkActionType.DEACTIVATE || bulkAction.type == BulkActionType.HARD_DELETE)) {

            return getDependencies(bulkAction, this::allIdsOfInstitution);
        }

        return Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<UserInfo>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.userRecordMapper.selectByExample()
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Set<Long> extractPKsFromKeys(final Collection<EntityKey> keys) {
        if (keys == null || keys.isEmpty() || keys.iterator().next().entityType != EntityType.USER) {
            return UserDAO.super.extractPKsFromKeys(keys);
        } else {
            try {

                if (keys == null || keys.isEmpty()) {
                    return Collections.emptySet();
                }

                final List<String> uuids = keys.stream()
                        .map(key -> key.modelId)
                        .collect(Collectors.toList());

                return new HashSet<>(this.userRecordMapper.selectIdsByExample()
                        .where(UserRecordDynamicSqlSupport.uuid, isIn(uuids))
                        .build()
                        .execute());

            } catch (final Exception e) {
                log.error("Unexpected error: ", e);
                return Collections.emptySet();
            }
        }
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.userRecordMapper.selectByExample()
                .where(UserRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getUuid(), EntityType.USER),
                        rec.getName(),
                        rec.getSurname()))
                .collect(Collectors.toList()));
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
                .forEach(this.roleRecordMapper::insert);
    }

    private Result<UserRecord> recordByUsername(final String username) {
        return getSingleResource(
                username,
                this.userRecordMapper
                        .selectByExample()
                        .where(UserRecordDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserRecordDynamicSqlSupport.active,
                                isEqualTo(BooleanUtils.toInteger(true)))
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
        return this.roleRecordMapper.selectByExample()
                .where(RoleRecordDynamicSqlSupport.userId, isEqualTo(record.getId()))
                .build()
                .execute();
    }

    private Result<UserInfo> toDomainModel(final UserRecord record) {

        return Result.tryCatch(() -> {

            final List<RoleRecord> roles = getRoles(record);
            Set<String> userRoles = Collections.emptySet();
            if (roles != null) {
                userRoles = roles
                        .stream()
                        .map(RoleRecord::getRoleName)
                        .collect(Collectors.toSet());
            }

            return new UserInfo(
                    record.getUuid(),
                    record.getInstitutionId(),
                    record.getCreationDate(),
                    record.getName(),
                    record.getSurname(),
                    record.getUsername(),
                    record.getEmail(),
                    BooleanUtils.toBooleanObject(record.getActive()),
                    Locale.forLanguageTag(record.getLanguage()),
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

    private void checkUniqueUsername(final UserAccount userAccount) {
        // check same username already exists
        final Long otherUsersWithSameName = this.userRecordMapper
                .countByExample()
                .where(UserRecordDynamicSqlSupport.username, isEqualTo(userAccount.getUsername()))
                .and(UserRecordDynamicSqlSupport.uuid, isNotEqualToWhenPresent(userAccount.getModelId()))
                .build()
                .execute();

        if (otherUsersWithSameName != null && otherUsersWithSameName > 0) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    Domain.USER.ATTR_USERNAME,
                    "user:username:username.notunique"));
        }
    }

    private void checkUniqueMailAddress(final UserAccount userAccount) {
        if (StringUtils.isBlank(userAccount.getEmail())) {
            return;
        }

        // check same email already exists
        final Long otherUsersWithSameName = this.userRecordMapper
                .countByExample()
                .where(UserRecordDynamicSqlSupport.email, isEqualTo(userAccount.getEmail()))
                .and(UserRecordDynamicSqlSupport.uuid, isNotEqualToWhenPresent(userAccount.getModelId()))
                .build()
                .execute();

        if (otherUsersWithSameName != null && otherUsersWithSameName > 0) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    Domain.USER.ATTR_EMAIL,
                    "user:email:email.notunique"));
        }
    }

}
