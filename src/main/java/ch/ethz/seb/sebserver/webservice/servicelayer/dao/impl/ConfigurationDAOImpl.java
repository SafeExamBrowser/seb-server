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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ConfigurationDAOImpl implements ConfigurationDAO {

    private final ConfigurationRecordMapper configurationRecordMapper;
    private final ConfigurationNodeRecordMapper configurationNodeRecordMapper;
    private final ConfigurationDAOBatchService configurationDAOBatchService;

    protected ConfigurationDAOImpl(
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ConfigurationDAOBatchService configurationDAOBatchService) {

        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.configurationDAOBatchService = configurationDAOBatchService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Configuration> byPK(final Long id) {
        return recordById(id)
                .flatMap(ConfigurationDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Configuration>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.configurationRecordMapper.selectByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Configuration>> allMatching(
            final FilterMap filterMap,
            final Predicate<Configuration> predicate) {

        return Result.tryCatch(() -> this.configurationRecordMapper
                .selectByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualToWhenPresent(filterMap.getConfigNodeId()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.versionDate,
                        isGreaterThanOrEqualToWhenPresent(filterMap.getConfigFromTime()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.followup,
                        isEqualToWhenPresent(filterMap.getConfigFollowup()))
                .build()
                .execute()
                .stream()
                .map(ConfigurationDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Configuration> getFollowupConfiguration(final Long configNodeId) {
        return Result.tryCatch(() -> this.configurationRecordMapper.selectByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(configNodeId))
                .and(
                        ConfigurationRecordDynamicSqlSupport.followup,
                        isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton())).flatMap(ConfigurationDAOImpl::toDomainModel);

    }

    @Override
    @Transactional(readOnly = true)
    public Result<Configuration> getConfigurationLastStableVersion(final Long configNodeId) {
        return Result.tryCatch(() -> {
            final List<ConfigurationRecord> configs = this.configurationRecordMapper.selectByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configNodeId))
                    .and(
                            ConfigurationRecordDynamicSqlSupport.followup,
                            isEqualTo(BooleanUtils.toInteger(false)))
                    .build()
                    .execute();
            configs.sort((c1, c2) -> c1.getVersionDate().compareTo(c2.getVersionDate()) * -1);
            return configs.get(0);
        }).flatMap(ConfigurationDAOImpl::toDomainModel);
    }

    @Override
    @Transactional
    public Result<Configuration> save(final Configuration data) {
        return Result.tryCatch(() -> {

            checkInstitutionalIntegrity(data);

            final ConfigurationRecord newRecord = new ConfigurationRecord(
                    null,
                    null,
                    null,
                    data.version,
                    null,
                    null);

            this.configurationRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Configuration> saveToHistory(final Long configurationNodeId) {
        return this.configurationDAOBatchService
                .saveToHistory(configurationNodeId)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Configuration> undo(final Long configurationNodeId) {
        return this.configurationDAOBatchService
                .undo(configurationNodeId)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Configuration> restoreToDefaultValues(final Long configurationNodeId) {
        return this.configurationDAOBatchService
                .restoreToDefaultValues(configurationNodeId)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Configuration> restoreToVersion(final Long configurationNodeId, final Long configId) {
        return this.configurationDAOBatchService
                .restoreToVersion(configurationNodeId, configId)
                .onError(TransactionHandler::rollback);
    }

    private Result<ConfigurationRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord record = this.configurationRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION,
                        String.valueOf(id));
            }
            return record;
        });
    }

    static Result<Configuration> toDomainModel(final ConfigurationRecord record) {
        return Result.tryCatch(() -> new Configuration(
                record.getId(),
                record.getInstitutionId(),
                record.getConfigurationNodeId(),
                record.getVersion(),
                record.getVersionDate(),
                BooleanUtils.toBooleanObject(record.getFollowup())));
    }

    private Result<Configuration> checkInstitutionalIntegrity(final Configuration data) {
        return Result.tryCatch(() -> {
            final ConfigurationNodeRecord r =
                    this.configurationNodeRecordMapper.selectByPrimaryKey(data.configurationNodeId);
            if (r.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }
            return data;
        });
    }

}
