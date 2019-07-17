/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SebClientConfigRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SebClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class SebClientConfigDAOImpl implements SebClientConfigDAO {

    private final SebClientConfigRecordMapper sebClientConfigRecordMapper;
    private final ClientCredentialService clientCredentialService;

    protected SebClientConfigDAOImpl(
            final SebClientConfigRecordMapper sebClientConfigRecordMapper,
            final ClientCredentialService clientCredentialService) {

        this.sebClientConfigRecordMapper = sebClientConfigRecordMapper;
        this.clientCredentialService = clientCredentialService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_CLIENT_CONFIGURATION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SebClientConfig> byPK(final Long id) {
        return recordById(id)
                .flatMap(SebClientConfigDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SebClientConfig>> all(final Long institutionId, final Boolean active) {
        return Result.tryCatch(() -> {

            final List<SebClientConfigRecord> records = (active != null)
                    ? this.sebClientConfigRecordMapper.selectByExample()
                            .where(
                                    SebClientConfigRecordDynamicSqlSupport.institutionId,
                                    isEqualToWhenPresent(institutionId))
                            .and(
                                    SebClientConfigRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : this.sebClientConfigRecordMapper.selectByExample().build().execute();

            return records.stream()
                    .map(SebClientConfigDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SebClientConfig>> allMatching(
            final FilterMap filterMap,
            final Predicate<SebClientConfig> predicate) {

        return Result.tryCatch(() -> {

            return this.sebClientConfigRecordMapper
                    .selectByExample()
                    .where(
                            SebClientConfigRecordDynamicSqlSupport.institutionId,
                            isEqualToWhenPresent(filterMap.getInstitutionId()))
                    .and(
                            SebClientConfigRecordDynamicSqlSupport.name,
                            isLikeWhenPresent(filterMap.getName()))
                    .and(
                            SebClientConfigRecordDynamicSqlSupport.date,
                            isGreaterThanOrEqualToWhenPresent(filterMap.getSebClientConfigFromTime()))
                    .and(
                            SebClientConfigRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(filterMap.getActiveAsInt()))
                    .build()
                    .execute()
                    .stream()
                    .map(SebClientConfigDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Result<SebClientConfig> byClientName(final String clientName) {
        return Result.tryCatch(() -> {

            return this.sebClientConfigRecordMapper
                    .selectByExample()
                    .where(
                            SebClientConfigRecordDynamicSqlSupport.clientName,
                            isEqualTo(clientName))
                    .build()
                    .execute()
                    .stream()
                    .map(SebClientConfigDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Utils.toSingleton());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CharSequence> getConfigPasswortCipherByClientName(final String clientName) {
        return Result.tryCatch(() -> {

            final SebClientConfigRecord record = this.sebClientConfigRecordMapper
                    .selectByExample()
                    .where(
                            SebClientConfigRecordDynamicSqlSupport.clientName,
                            isEqualTo(clientName))
                    .and(
                            SebClientConfigRecordDynamicSqlSupport.active,
                            isNotEqualTo(0))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());

            return record.getClientSecret();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(final String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return false;
        }

        return this.sebClientConfigRecordMapper.countByExample()
                .where(SebClientConfigRecordDynamicSqlSupport.id, isEqualTo(Long.valueOf(modelId)))
                .and(SebClientConfigRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute()
                .longValue() > 0;
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            final SebClientConfigRecord record = new SebClientConfigRecord(
                    null, null, null, null, null, null, null,
                    BooleanUtils.toIntegerObject(active));

            this.sebClientConfigRecordMapper.updateByExampleSelective(record)
                    .where(SebClientConfigRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.SEB_CLIENT_CONFIGURATION))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<SebClientConfig> createNew(final SebClientConfig sebClientConfig) {
        return this.clientCredentialService
                .generatedClientCredentials()
                .map(cc -> {

                    checkUniqueName(sebClientConfig);

                    final SebClientConfigRecord newRecord = new SebClientConfigRecord(
                            null,
                            sebClientConfig.institutionId,
                            sebClientConfig.name,
                            DateTime.now(DateTimeZone.UTC),
                            cc.clientIdAsString(),
                            cc.secretAsString(),
                            getEncryptionPassword(sebClientConfig),
                            BooleanUtils.toInteger(BooleanUtils.isTrue(sebClientConfig.active)));

                    this.sebClientConfigRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .flatMap(SebClientConfigDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<SebClientConfig> save(final SebClientConfig sebClientConfig) {
        return Result.tryCatch(() -> {

            checkUniqueName(sebClientConfig);

            final SebClientConfigRecord newRecord = new SebClientConfigRecord(
                    sebClientConfig.id,
                    null,
                    sebClientConfig.name,
                    null,
                    null,
                    null,
                    getEncryptionPassword(sebClientConfig),
                    null);

            this.sebClientConfigRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.sebClientConfigRecordMapper.selectByPrimaryKey(sebClientConfig.id);
        })
                .flatMap(SebClientConfigDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            this.sebClientConfigRecordMapper.deleteByExample()
                    .where(SebClientConfigRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.SEB_CLIENT_CONFIGURATION))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SebClientConfig>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            return this.sebClientConfigRecordMapper.selectByExample()
                    .where(SebClientConfigRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(SebClientConfigDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
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
    public Result<ClientCredentials> getSebClientCredentials(final String modelId) {
        return recordByModelId(modelId)
                .map(rec -> new ClientCredentials(
                        rec.getClientName(),
                        rec.getClientSecret(),
                        null));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CharSequence> getConfigPasswortCipher(final String modelId) {
        return recordByModelId(modelId)
                .map(rec -> rec.getEncryptSecret());
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> {
            return this.sebClientConfigRecordMapper.selectIdsByExample()
                    .where(SebClientConfigRecordDynamicSqlSupport.institutionId,
                            isEqualTo(Long.valueOf(institutionKey.modelId)))
                    .build()
                    .execute()
                    .stream()
                    .map(id -> new EntityKey(id, EntityType.SEB_CLIENT_CONFIGURATION))
                    .collect(Collectors.toList());
        });
    }

    private Result<SebClientConfigRecord> recordByModelId(final String modelId) {
        return Result.tryCatch(() -> {
            return recordById(Long.parseLong(modelId)).getOrThrow();
        });
    }

    private Result<SebClientConfigRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final SebClientConfigRecord record = this.sebClientConfigRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.SEB_CLIENT_CONFIGURATION,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private static Result<SebClientConfig> toDomainModel(final SebClientConfigRecord record) {
        return Result.tryCatch(() -> new SebClientConfig(
                record.getId(),
                record.getInstitutionId(),
                record.getName(),
                record.getDate(),
                null,
                null,
                BooleanUtils.toBooleanObject(record.getActive())));
    }

    private String getEncryptionPassword(final SebClientConfig sebClientConfig) {
        if (sebClientConfig.hasEncryptionSecret() &&
                !sebClientConfig.encryptSecret.equals(sebClientConfig.confirmEncryptSecret)) {
            throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
        }

        final CharSequence encrypted_encrypt_secret = sebClientConfig.hasEncryptionSecret()
                ? this.clientCredentialService.encrypt(sebClientConfig.encryptSecret)
                : null;
        return (encrypted_encrypt_secret != null) ? encrypted_encrypt_secret.toString() : null;
    }

    // check if same name already exists for the same institution
    // if true an APIMessageException with a field validation error is thrown
    private void checkUniqueName(final SebClientConfig sebClientConfig) {

        final Long otherWithSameName = this.sebClientConfigRecordMapper
                .countByExample()
                .where(SebClientConfigRecordDynamicSqlSupport.name, isEqualTo(sebClientConfig.name))
                .and(SebClientConfigRecordDynamicSqlSupport.institutionId, isEqualTo(sebClientConfig.institutionId))
                .and(SebClientConfigRecordDynamicSqlSupport.id, isNotEqualToWhenPresent(sebClientConfig.id))
                .build()
                .execute();

        if (otherWithSameName != null && otherWithSameName.longValue() > 0) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                    "clientconfig:name:name.notunique"));
        }
    }

}
