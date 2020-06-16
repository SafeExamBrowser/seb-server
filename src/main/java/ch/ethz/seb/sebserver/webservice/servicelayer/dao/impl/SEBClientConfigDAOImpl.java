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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.ConfigPurpose;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SebClientConfigRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class SEBClientConfigDAOImpl implements SEBClientConfigDAO {

    private final SebClientConfigRecordMapper sebClientConfigRecordMapper;
    private final ClientCredentialService clientCredentialService;
    private final AdditionalAttributesDAOImpl additionalAttributesDAO;

    protected SEBClientConfigDAOImpl(
            final SebClientConfigRecordMapper sebClientConfigRecordMapper,
            final ClientCredentialService clientCredentialService,
            final AdditionalAttributesDAOImpl additionalAttributesDAO) {

        this.sebClientConfigRecordMapper = sebClientConfigRecordMapper;
        this.clientCredentialService = clientCredentialService;
        this.additionalAttributesDAO = additionalAttributesDAO;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_CLIENT_CONFIGURATION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SEBClientConfig> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SEBClientConfig>> all(final Long institutionId, final Boolean active) {
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
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SEBClientConfig>> allMatching(
            final FilterMap filterMap,
            final Predicate<SEBClientConfig> predicate) {

        return Result.tryCatch(() -> this.sebClientConfigRecordMapper
                .selectByExample()
                .where(
                        SebClientConfigRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        SebClientConfigRecordDynamicSqlSupport.name,
                        isLikeWhenPresent(filterMap.getName()))
                .and(
                        SebClientConfigRecordDynamicSqlSupport.date,
                        isGreaterThanOrEqualToWhenPresent(filterMap.getSEBClientConfigFromTime()))
                .and(
                        SebClientConfigRecordDynamicSqlSupport.active,
                        isEqualToWhenPresent(filterMap.getActiveAsInt()))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    public Result<SEBClientConfig> byClientName(final String clientName) {
        return Result.tryCatch(() -> this.sebClientConfigRecordMapper
                .selectByExample()
                .where(
                        SebClientConfigRecordDynamicSqlSupport.clientName,
                        isEqualTo(clientName))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Utils.toSingleton()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CharSequence> getConfigPasswordCipherByClientName(final String clientName) {
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
                .execute() > 0;
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
    public Result<SEBClientConfig> createNew(final SEBClientConfig sebClientConfig) {
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

                    this.sebClientConfigRecordMapper
                            .insert(newRecord);

                    saveAdditionalAttributes(sebClientConfig, newRecord.getId());

                    return newRecord;
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<SEBClientConfig> save(final SEBClientConfig sebClientConfig) {
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

            this.sebClientConfigRecordMapper
                    .updateByPrimaryKeySelective(newRecord);

            saveAdditionalAttributes(sebClientConfig, newRecord.getId());

            return this.sebClientConfigRecordMapper
                    .selectByPrimaryKey(sebClientConfig.id);
        })
                .flatMap(this::toDomainModel)
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
    public Result<Collection<SEBClientConfig>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> this.sebClientConfigRecordMapper.selectByExample()
                .where(SebClientConfigRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
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
    public Result<ClientCredentials> getSEBClientCredentials(final String modelId) {
        return recordByModelId(modelId)
                .map(rec -> new ClientCredentials(
                        rec.getClientName(),
                        rec.getClientSecret(),
                        null));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CharSequence> getConfigPasswordCipher(final String modelId) {
        return recordByModelId(modelId)
                .map(SebClientConfigRecord::getEncryptSecret);
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.sebClientConfigRecordMapper.selectIdsByExample()
                .where(SebClientConfigRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.SEB_CLIENT_CONFIGURATION))
                .collect(Collectors.toList()));
    }

    private Result<SebClientConfigRecord> recordByModelId(final String modelId) {
        return Result.tryCatch(() -> recordById(Long.parseLong(modelId)).getOrThrow());
    }

    private Result<SebClientConfigRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final SebClientConfigRecord record = this.sebClientConfigRecordMapper
                    .selectByPrimaryKey(id);

            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.SEB_CLIENT_CONFIGURATION,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<SEBClientConfig> toDomainModel(final SebClientConfigRecord record) {

        final Map<String, AdditionalAttributeRecord> additionalAttributes = this.additionalAttributesDAO
                .getAdditionalAttributes(
                        EntityType.SEB_CLIENT_CONFIGURATION,
                        record.getId())
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(
                        AdditionalAttributeRecord::getName,
                        Function.identity()));

        additionalAttributes.get(SEBClientConfig.ATTR_CONFIG_PURPOSE);

        return Result.tryCatch(() -> new SEBClientConfig(
                record.getId(),
                record.getInstitutionId(),
                record.getName(),
                additionalAttributes.containsKey(SEBClientConfig.ATTR_CONFIG_PURPOSE)
                        ? ConfigPurpose
                                .valueOf(additionalAttributes.get(SEBClientConfig.ATTR_CONFIG_PURPOSE).getValue())
                        : ConfigPurpose.START_EXAM,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK) &&
                        BooleanUtils.toBoolean(additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK).getValue()),
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK_START_URL)
                        ? additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK_START_URL).getValue()
                        : null,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK_TIMEOUT)
                        ? Long.parseLong(additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK_TIMEOUT).getValue())
                        : null,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK_ATTEMPTS)
                        ? Short.parseShort(additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK_ATTEMPTS).getValue())
                        : null,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL)
                        ? Short.parseShort(
                                additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL).getValue())
                        : null,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_FALLBACK_PASSWORD)
                        ? additionalAttributes.get(SEBClientConfig.ATTR_FALLBACK_PASSWORD).getValue()
                        : null,
                null,
                additionalAttributes.containsKey(SEBClientConfig.ATTR_QUIT_PASSWORD)
                        ? additionalAttributes.get(SEBClientConfig.ATTR_QUIT_PASSWORD).getValue()
                        : null,
                null,
                record.getDate(),
                record.getEncryptSecret(),
                null,
                BooleanUtils.toBooleanObject(record.getActive())));
    }

    private String getEncryptionPassword(final SEBClientConfig sebClientConfig) {
        if (sebClientConfig.hasEncryptionSecret() &&
                !sebClientConfig.encryptSecret.equals(sebClientConfig.encryptSecretConfirm)) {
            throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
        }

        final CharSequence encrypted_encrypt_secret = sebClientConfig.hasEncryptionSecret()
                ? this.clientCredentialService.encrypt(sebClientConfig.encryptSecret)
                : null;
        return (encrypted_encrypt_secret != null) ? encrypted_encrypt_secret.toString() : null;
    }

    // check if same name already exists for the same institution
    // if true an APIMessageException with a field validation error is thrown
    private void checkUniqueName(final SEBClientConfig sebClientConfig) {

        final Long otherWithSameName = this.sebClientConfigRecordMapper
                .countByExample()
                .where(SebClientConfigRecordDynamicSqlSupport.name, isEqualTo(sebClientConfig.name))
                .and(SebClientConfigRecordDynamicSqlSupport.institutionId, isEqualTo(sebClientConfig.institutionId))
                .and(SebClientConfigRecordDynamicSqlSupport.id, isNotEqualToWhenPresent(sebClientConfig.id))
                .build()
                .execute();

        if (otherWithSameName != null && otherWithSameName > 0) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                    "clientconfig:name:name.notunique"));
        }
    }

    private void saveAdditionalAttributes(final SEBClientConfig sebClientConfig, final Long configId) {
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.SEB_CLIENT_CONFIGURATION,
                configId,
                SEBClientConfig.ATTR_CONFIG_PURPOSE,
                (sebClientConfig.configPurpose != null)
                        ? sebClientConfig.configPurpose.name()
                        : ConfigPurpose.CONFIGURE_CLIENT.name());

        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.SEB_CLIENT_CONFIGURATION,
                configId,
                SEBClientConfig.ATTR_FALLBACK,
                String.valueOf(BooleanUtils.isTrue(sebClientConfig.fallback)));

        if (BooleanUtils.isTrue(sebClientConfig.fallback)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_START_URL,
                    sebClientConfig.fallbackStartURL);
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_START_URL);
        }

        if (BooleanUtils.isTrue(sebClientConfig.fallback)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_TIMEOUT,
                    sebClientConfig.fallbackTimeout.toString());
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_TIMEOUT);
        }

        if (BooleanUtils.isTrue(sebClientConfig.fallback)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPTS,
                    sebClientConfig.fallbackAttempts.toString());
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPTS);
        }

        if (BooleanUtils.isTrue(sebClientConfig.fallback)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL,
                    sebClientConfig.fallbackAttemptInterval.toString());
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL);
        }

        if (BooleanUtils.isTrue(sebClientConfig.fallback) && StringUtils.isNotBlank(sebClientConfig.fallbackPassword)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_PASSWORD,
                    this.clientCredentialService.encrypt(sebClientConfig.fallbackPassword).toString());
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_FALLBACK_PASSWORD);
        }

        if (BooleanUtils.isTrue(sebClientConfig.fallback) && StringUtils.isNotBlank(sebClientConfig.quitPassword)) {
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.SEB_CLIENT_CONFIGURATION,
                    configId,
                    SEBClientConfig.ATTR_QUIT_PASSWORD,
                    this.clientCredentialService.encrypt(sebClientConfig.quitPassword).toString());
        } else {
            this.additionalAttributesDAO.delete(
                    configId,
                    SEBClientConfig.ATTR_QUIT_PASSWORD);
        }
    }

}
