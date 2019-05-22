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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationValueRecord;
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
    public Result<Configuration> getFollowupConfiguration(final String configNodeId) {
        return Result.tryCatch(() -> {
            return this.configurationRecordMapper.selectByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(Long.parseLong(configNodeId)))
                    .and(
                            ConfigurationRecordDynamicSqlSupport.followup,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());
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
    @Transactional
    public Result<Configuration> saveToHistory(final Long configurationNodeId) {
        return Result.tryCatch(() -> {

            // get follow-up configuration...
            final ConfigurationRecord followupConfig = getFollowupConfigurationRecord(configurationNodeId);

            // with actual attribute values
            final List<ConfigurationValueRecord> allValues = this.configurationValueRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(followupConfig.getId()))
                    .build()
                    .execute();

            // get current versions count
            final Long versions = this.configurationRecordMapper
                    .countByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configurationNodeId))
                    .and(
                            ConfigurationRecordDynamicSqlSupport.followup,
                            isNotEqualTo(BooleanUtils.toInteger(true)))
                    .build()
                    .execute();

            // close follow-up configuration to save in history
            final ConfigurationRecord configUpdate = new ConfigurationRecord(
                    followupConfig.getId(),
                    null,
                    null,
                    "v" + versions,
                    DateTime.now(DateTimeZone.UTC),
                    BooleanUtils.toInteger(false));
            this.configurationRecordMapper.updateByPrimaryKeySelective(configUpdate);

            // and create a new follow-up...
            final ConfigurationRecord newFollowup = new ConfigurationRecord(
                    null,
                    followupConfig.getInstitutionId(),
                    followupConfig.getConfigurationNodeId(),
                    null,
                    null,
                    BooleanUtils.toInteger(true));
            this.configurationRecordMapper.insert(newFollowup);

            // with the current attribute values
            // TODO batch here for better performance
            allValues.stream()
                    .map(oldValRec -> new ConfigurationValueRecord(
                            null,
                            oldValRec.getInstitutionId(),
                            newFollowup.getId(),
                            oldValRec.getConfigurationAttributeId(),
                            oldValRec.getListIndex(),
                            oldValRec.getValue(),
                            oldValRec.getText()))
                    .forEach(newValRec -> this.configurationValueRecordMapper.insert(newValRec));

            return newFollowup;

        })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    private ConfigurationRecord getFollowupConfigurationRecord(final Long configurationNodeId) {
        return this.configurationRecordMapper
                .selectByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(configurationNodeId))
                .and(
                        ConfigurationRecordDynamicSqlSupport.followup,
                        isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton());
    }

    @Override
    @Transactional
    public Result<Configuration> undo(final Long configurationNodeId) {
        return Result.tryCatch(() -> {
            // get all configurations of the node
            final List<ConfigurationRecord> configs = this.configurationRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configurationNodeId))
                    .orderBy(ConfigurationRecordDynamicSqlSupport.versionDate)
                    .build()
                    .execute();

            return configs.get(configs.size() - 1);
        })
                .flatMap(rec -> restoreToVersion(configurationNodeId, rec.getId()))
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Configuration> restoreToVersion(final Long configurationNodeId, final Long configId) {
        return Result.tryCatch(() -> {

            // get requested configuration in history...
            final ConfigurationRecord config = this.configurationRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configurationNodeId))
                    .and(
                            ConfigurationRecordDynamicSqlSupport.id,
                            isEqualTo(configId))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());

            // with historic attribute values
            final List<ConfigurationValueRecord> historicValues = this.configurationValueRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(config.getId()))
                    .build()
                    .execute();

            // get follow-up configuration id
            final ConfigurationRecord followup = getFollowupConfigurationRecord(configurationNodeId);

            // restore all current values of the follow-up with historic values
            // TODO batch here for better performance
            historicValues.stream()
                    .map(historicValRec -> new ConfigurationValueRecord(
                            null,
                            followup.getInstitutionId(),
                            followup.getId(),
                            historicValRec.getConfigurationAttributeId(),
                            historicValRec.getListIndex(),
                            historicValRec.getValue(),
                            historicValRec.getText()))
                    .forEach(newValRec -> this.configurationValueRecordMapper
                            .updateByExample(newValRec)
                            .where(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                    isEqualTo(followup.getId()))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                    isEqualTo(newValRec.getConfigurationAttributeId()))
                            .build()
                            .execute());

            return followup;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
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
