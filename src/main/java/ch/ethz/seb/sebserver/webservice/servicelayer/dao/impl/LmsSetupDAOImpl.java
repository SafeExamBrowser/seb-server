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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.LmsSetupRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class LmsSetupDAOImpl implements LmsSetupDAO {

    private final LmsSetupRecordMapper lmsSetupRecordMapper;
    private final ClientCredentialService clientCredentialService;

    protected LmsSetupDAOImpl(
            final LmsSetupRecordMapper lmsSetupRecordMapper,
            final ClientCredentialService clientCredentialService) {

        this.lmsSetupRecordMapper = lmsSetupRecordMapper;
        this.clientCredentialService = clientCredentialService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.LMS_SETUP;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<LmsSetup> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<LmsSetup>> all(final Long institutionId, final Boolean active) {

        return Result.tryCatch(() -> {
            final List<LmsSetupRecord> records = (active != null)
                    ? this.lmsSetupRecordMapper.selectByExample()
                            .where(
                                    LmsSetupRecordDynamicSqlSupport.institutionId,
                                    isEqualToWhenPresent(institutionId))
                            .and(
                                    LmsSetupRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : this.lmsSetupRecordMapper.selectByExample()
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
    public Result<Collection<LmsSetup>> allMatching(
            final FilterMap filterMap,
            final Predicate<LmsSetup> predicate) {

        return Result.tryCatch(() -> this.lmsSetupRecordMapper
                .selectByExample()
                .where(
                        LmsSetupRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        LmsSetupRecordDynamicSqlSupport.name,
                        isLikeWhenPresent(filterMap.getName()))
                .and(
                        LmsSetupRecordDynamicSqlSupport.lmsType,
                        isEqualToWhenPresent(filterMap.getLmsSetupType()))
                .and(
                        LmsSetupRecordDynamicSqlSupport.active,
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
    @Transactional
    public Result<LmsSetup> save(final LmsSetup lmsSetup) {
        return Result.tryCatch(() -> {

            checkUniqueName(lmsSetup);

            LmsSetupRecord savedRecord = recordById(lmsSetup.id)
                    .getOrThrow();

            final ClientCredentials lmsCredentials = createAPIClientCredentials(lmsSetup);
            final ClientCredentials proxyCredentials = createProxyClientCredentials(lmsSetup);
            final LmsSetupRecord newRecord = new LmsSetupRecord(
                    lmsSetup.id,
                    lmsSetup.institutionId,
                    lmsSetup.name,
                    (lmsSetup.lmsType != null) ? lmsSetup.lmsType.name() : null,
                    lmsSetup.lmsApiUrl,
                    lmsCredentials.clientIdAsString(),
                    lmsCredentials.secretAsString(),
                    lmsCredentials.accessTokenAsString(),
                    lmsSetup.getProxyHost(),
                    lmsSetup.getProxyPort(),
                    proxyCredentials.clientIdAsString(),
                    proxyCredentials.secretAsString(),
                    savedRecord.getActive());

            this.lmsSetupRecordMapper.updateByPrimaryKey(newRecord);
            return this.lmsSetupRecordMapper.selectByPrimaryKey(lmsSetup.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<LmsSetup> createNew(final LmsSetup lmsSetup) {
        return Result.tryCatch(() -> {

            checkUniqueName(lmsSetup);

            final ClientCredentials lmsCredentials = createAPIClientCredentials(lmsSetup);
            final ClientCredentials proxyCredentials = createProxyClientCredentials(lmsSetup);
            final LmsSetupRecord newRecord = new LmsSetupRecord(
                    null,
                    lmsSetup.institutionId,
                    lmsSetup.name,
                    (lmsSetup.lmsType != null) ? lmsSetup.lmsType.name() : null,
                    lmsSetup.lmsApiUrl,
                    lmsCredentials.clientIdAsString(),
                    lmsCredentials.secretAsString(),
                    lmsCredentials.accessTokenAsString(),
                    lmsSetup.getProxyHost(),
                    lmsSetup.getProxyPort(),
                    proxyCredentials.clientIdAsString(),
                    proxyCredentials.secretAsString(),
                    BooleanUtils.toInteger(false));

            this.lmsSetupRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            final LmsSetupRecord lmsSetupRecord = new LmsSetupRecord(
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toIntegerObject(active));

            this.lmsSetupRecordMapper.updateByExampleSelective(lmsSetupRecord)
                    .where(LmsSetupRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(final String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return false;
        }

        return this.lmsSetupRecordMapper.countByExample()
                .where(LmsSetupRecordDynamicSqlSupport.id, isEqualTo(Long.valueOf(modelId)))
                .and(LmsSetupRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute() > 0;
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            this.lmsSetupRecordMapper.deleteByExample()
                    .where(LmsSetupRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
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
    public Result<Collection<LmsSetup>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> this.lmsSetupRecordMapper.selectByExample()
                .where(LmsSetupRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientCredentials> getLmsAPIAccessCredentials(final String lmsSetupId) {
        return Result.tryCatch(() -> {
            final LmsSetupRecord record = this.lmsSetupRecordMapper
                    .selectByPrimaryKey(Long.parseLong(lmsSetupId));

            return new ClientCredentials(
                    record.getLmsClientname(),
                    record.getLmsClientsecret(),
                    record.getLmsRestApiToken());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ProxyData> getLmsAPIAccessProxyData(final String lmsSetupId) {
        return Result.tryCatch(() -> {
            final LmsSetupRecord record = this.lmsSetupRecordMapper
                    .selectByPrimaryKey(Long.parseLong(lmsSetupId));

            if (StringUtils.isNoneBlank(record.getLmsProxyHost())) {
                return new ProxyData(
                        record.getLmsProxyHost(),
                        record.getLmsProxyPort(),
                        new ClientCredentials(
                                record.getLmsProxyAuthUsername(),
                                record.getLmsProxyAuthSecret()));
            } else {
                throw new RuntimeException("No proxy settings for LmsSetup: " + lmsSetupId);
            }
        });
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.lmsSetupRecordMapper.selectIdsByExample()
                .where(LmsSetupRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
                .collect(Collectors.toList()));
    }

    private Result<LmsSetupRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final LmsSetupRecord record = this.lmsSetupRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.LMS_SETUP,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<LmsSetup> toDomainModel(final LmsSetupRecord record) {

        final ClientCredentials clientCredentials = new ClientCredentials(
                record.getLmsClientname(),
                record.getLmsClientsecret(),
                record.getLmsRestApiToken());

        final ClientCredentials proxyCredentials = new ClientCredentials(
                record.getLmsProxyAuthUsername(),
                record.getLmsProxyAuthSecret());

        final CharSequence plainAccessToken = this.clientCredentialService.getPlainAccessToken(clientCredentials);
        return Result.tryCatch(() -> new LmsSetup(
                record.getId(),
                record.getInstitutionId(),
                record.getName(),
                LmsType.valueOf(record.getLmsType()),
                Utils.toString(clientCredentials.clientId),
                null,
                record.getLmsUrl(),
                Utils.toString(plainAccessToken),
                record.getLmsProxyHost(),
                record.getLmsProxyPort(),
                Utils.toString(proxyCredentials.clientId),
                Utils.toString(proxyCredentials.secret),
                BooleanUtils.toBooleanObject(record.getActive())));
    }

    // check if same name already exists for the same institution
    // if true an APIMessageException with a field validation error is thrown
    private void checkUniqueName(final LmsSetup lmsSetup) {

        final Long otherWithSameName = this.lmsSetupRecordMapper
                .countByExample()
                .where(LmsSetupRecordDynamicSqlSupport.name, isEqualTo(lmsSetup.name))
                .and(LmsSetupRecordDynamicSqlSupport.institutionId, isEqualTo(lmsSetup.institutionId))
                .and(LmsSetupRecordDynamicSqlSupport.id, isNotEqualToWhenPresent(lmsSetup.id))
                .build()
                .execute();

        if (otherWithSameName != null && otherWithSameName > 0) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    Domain.LMS_SETUP.ATTR_NAME,
                    "lmsSetup:name:name.notunique"));
        }
    }

    private ClientCredentials createProxyClientCredentials(final LmsSetup lmsSetup) {
        return (StringUtils.isBlank(lmsSetup.proxyAuthUsername))
                ? new ClientCredentials(null, null)
                : this.clientCredentialService.encryptClientCredentials(
                        lmsSetup.proxyAuthUsername,
                        lmsSetup.proxyAuthSecret);
    }

    private ClientCredentials createAPIClientCredentials(final LmsSetup lmsSetup) {
        return this.clientCredentialService.encryptClientCredentials(
                lmsSetup.lmsAuthName,
                lmsSetup.lmsAuthSecret,
                lmsSetup.lmsRestApiToken);
    }

}
