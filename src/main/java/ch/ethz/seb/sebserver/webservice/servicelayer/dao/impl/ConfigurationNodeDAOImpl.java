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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCopyInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ConfigurationNodeDAOImpl implements ConfigurationNodeDAO {

    private final ConfigurationRecordMapper configurationRecordMapper;
    private final ConfigurationNodeRecordMapper configurationNodeRecordMapper;
    private final ConfigurationValueRecordMapper configurationValueRecordMapper;
    private final ConfigurationDAOBatchService configurationDAOBatchService;
    private final String copyNamePrefix;
    private final String copyNameSuffix;

    protected ConfigurationNodeDAOImpl(
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final ConfigurationDAOBatchService ConfigurationDAOBatchService,
            @Value("${sebserver.webservice.api.copy-name-prefix:Copy of }") final String copyNamePrefix,
            @Value("${sebserver.webservice.api.copy-name-suffix:}") final String copyNameSuffix) {

        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.configurationDAOBatchService = ConfigurationDAOBatchService;
        this.copyNamePrefix = copyNamePrefix;
        this.copyNameSuffix = copyNameSuffix;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_NODE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationNode> byPK(final Long id) {
        return recordById(id)
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationNode>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {
            return this.configurationNodeRecordMapper.selectByExample()
                    .where(ConfigurationNodeRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationNodeDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationNode>> allMatching(
            final FilterMap filterMap,
            final Predicate<ConfigurationNode> predicate) {

        return Result.tryCatch(() -> this.configurationNodeRecordMapper
                .selectByExample()
                .where(
                        ConfigurationNodeRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeStatus()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.name,
                        SqlBuilder.isLikeWhenPresent(filterMap.getName()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.description,
                        SqlBuilder.isLikeWhenPresent(filterMap.getConfigNodeDesc()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.type,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeType()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.templateId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeTemplateId()))
                .build()
                .execute()
                .stream()
                .map(ConfigurationNodeDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        // define the select function in case of source type
        final Function<EntityKey, Result<Collection<EntityKey>>> selectionFunction =
                (bulkAction.sourceType == EntityType.INSTITUTION)
                        ? this::allIdsOfInstitution
                        : key -> Result.of(Collections.emptyList()); // else : empty select function

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional
    public Result<ConfigurationNode> createNew(final ConfigurationNode data) {
        return this.configurationDAOBatchService
                .createNewConfiguration(data)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationNode> save(final ConfigurationNode data) {
        return Result.tryCatch(() -> {

            final Long count = this.configurationNodeRecordMapper.countByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            isEqualTo(data.name))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.id,
                            isNotEqualTo(data.id))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            isNotEqualTo(data.institutionId))
                    .build()
                    .execute();

            if (count != null && count.longValue() > 0) {
                throw new FieldValidationException("name", "configurationNode:name:exists");
            }

            final ConfigurationNodeRecord newRecord = new ConfigurationNodeRecord(
                    data.id,
                    null,
                    null,
                    null,
                    data.name,
                    data.description,
                    null,
                    (data.status != null) ? data.status.name() : ConfigurationStatus.CONSTRUCTION.name());

            this.configurationNodeRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.configurationNodeRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationNode> createCopy(
            final Long institutionId,
            final String newOwner,
            final ConfigCopyInfo copyInfo) {

        return this.recordById(copyInfo.configurationNodeId)
                .flatMap(nodeRec -> (nodeRec.getInstitutionId().equals(institutionId)
                        ? Result.of(nodeRec)
                        : Result.ofError(new IllegalArgumentException("Institution integrity violation"))))
                .map(nodeRec -> this.copyNodeRecord(nodeRec, newOwner, copyInfo))
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            // find all configurations for this configuration node
            final List<Long> configurationIds = this.configurationRecordMapper.selectIdsByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.configurationNodeId, isIn(ids))
                    .build()
                    .execute();

            // delete all ConfigurationValue's that belongs to the Configuration's to delete
            this.configurationValueRecordMapper.deleteByExample()
                    .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isIn(configurationIds))
                    .build()
                    .execute();

            // delete all Configuration's
            this.configurationRecordMapper.deleteByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.id, isIn(configurationIds))
                    .build()
                    .execute();

            // and finally delete the requested ConfigurationNode's
            this.configurationNodeRecordMapper.deleteByExample()
                    .where(ConfigurationNodeRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CONFIGURATION_NODE))
                    .collect(Collectors.toList());
        });
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> {
            return this.configurationNodeRecordMapper.selectIdsByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            isEqualTo(Long.valueOf(institutionKey.modelId)))
                    .build()
                    .execute()
                    .stream()
                    .map(id -> new EntityKey(id, EntityType.CONFIGURATION_NODE))
                    .collect(Collectors.toList());
        });
    }

    private Result<ConfigurationNodeRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ConfigurationNodeRecord record = this.configurationNodeRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION_NODE,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private ConfigurationNodeRecord copyNodeRecord(
            final ConfigurationNodeRecord nodeRec,
            final String newOwner,
            final ConfigCopyInfo copyInfo) {

        final ConfigurationNodeRecord newNodeRec = new ConfigurationNodeRecord(
                null,
                nodeRec.getInstitutionId(),
                nodeRec.getTemplateId(),
                StringUtils.isNotBlank(newOwner) ? newOwner : nodeRec.getOwner(),
                this.copyNamePrefix + nodeRec.getName() + this.copyNameSuffix,
                nodeRec.getDescription(),
                nodeRec.getType(),
                ConfigurationStatus.CONSTRUCTION.name());
        this.configurationNodeRecordMapper.insert(newNodeRec);

        final List<ConfigurationRecord> configs = this.configurationRecordMapper
                .selectByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(nodeRec.getId()))
                .build()
                .execute();

        if (BooleanUtils.toBoolean(copyInfo.withHistory)) {
            configs
                    .stream()
                    .forEach(configRec -> this.configurationDAOBatchService.copyConfiguration(
                            configRec.getInstitutionId(),
                            configRec.getId(),
                            newNodeRec.getId()));
        } else {
            configs
                    .stream()
                    .filter(configRec -> configRec.getVersionDate() == null)
                    .findFirst()
                    .map(configRec -> {
                        // No history means to create a first version and a follow-up with the copied values
                        final ConfigurationRecord newFirstVersion = new ConfigurationRecord(
                                null,
                                configRec.getInstitutionId(),
                                configRec.getConfigurationNodeId(),
                                ConfigurationDAOBatchService.INITIAL_VERSION_NAME,
                                DateTime.now(DateTimeZone.UTC),
                                BooleanUtils.toInteger(false));
                        this.configurationRecordMapper.insert(newFirstVersion);
                        this.configurationDAOBatchService.copyValues(
                                configRec.getInstitutionId(),
                                configRec.getId(),
                                newFirstVersion.getId());
                        // and copy the follow-up
                        this.configurationDAOBatchService.copyConfiguration(
                                configRec.getInstitutionId(),
                                configRec.getId(),
                                newNodeRec.getId());
                        return configRec;
                    });
        }

        return newNodeRec;
    }

    static Result<ConfigurationNode> toDomainModel(final ConfigurationNodeRecord record) {
        return Result.tryCatch(() -> new ConfigurationNode(
                record.getId(),
                record.getInstitutionId(),
                record.getTemplateId(),
                record.getName(),
                record.getDescription(),
                ConfigurationType.valueOf(record.getType()),
                record.getOwner(),
                ConfigurationStatus.valueOf(record.getStatus())));
    }

}
