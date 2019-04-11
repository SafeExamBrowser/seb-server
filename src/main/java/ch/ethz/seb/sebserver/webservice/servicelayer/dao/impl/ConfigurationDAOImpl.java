/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
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
    private final ConfigurationValueRecordMapper configurationValueRecordMapper;

    protected ConfigurationDAOImpl(
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ConfigurationValueRecordMapper configurationValueRecordMapper) {

        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.configurationValueRecordMapper = configurationValueRecordMapper;
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
            return this.configurationRecordMapper.selectByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
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
                        SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeId()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.versionDate,
                        SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getConfigFromTime()))
                .and(
                        ConfigurationRecordDynamicSqlSupport.followup,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigFollowup()))
                .build()
                .execute()
                .stream()
                .map(ConfigurationDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<Configuration> createNew(final Configuration data) {
        return checkInstitutionalIntegrity(data)
                .map(config -> {
                    final ConfigurationRecord newRecord = new ConfigurationRecord(
                            null,
                            config.institutionId,
                            config.configurationNodeId,
                            config.version,
                            config.versionDate,
                            BooleanUtils.toInteger(config.followup));

                    this.configurationRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Configuration> save(final Configuration data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord newRecord = new ConfigurationRecord(
                    null,
                    null,
                    null,
                    data.version,
                    data.versionDate,
                    BooleanUtils.toInteger(data.followup));

            this.configurationRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            // delete all ConfigurationValue's that belongs to the Configuration's to delete
            this.configurationValueRecordMapper.deleteByExample()
                    .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isIn(ids))
                    .build()
                    .execute();

            // delete all requested Configuration's
            this.configurationRecordMapper.deleteByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CONFIGURATION))
                    .collect(Collectors.toList());
        });
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

    private static Result<Configuration> toDomainModel(final ConfigurationRecord record) {
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
