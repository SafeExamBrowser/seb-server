/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isNotEqualTo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.BatisConfig;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationValueRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigInitService;

/** This service is internally used to implement MyBatis batch functionality for the most
 * intensive write operation on Configuration domain. */
@Lazy
@Component
@WebServiceProfile
@DependsOn("batisConfig")
class ConfigurationDAOBatchService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationDAOBatchService.class);

    public static final String INITIAL_VERSION_NAME = "v0";

    private final ConfigurationNodeRecordMapper batchConfigurationNodeRecordMapper;
    private final ConfigurationValueRecordMapper batchConfigurationValueRecordMapper;
    private final ConfigurationAttributeRecordMapper batchConfigurationAttributeRecordMapper;
    private final ConfigurationRecordMapper batchConfigurationRecordMapper;
    private final ExamConfigInitService examConfigInitService;

    private final SqlSessionTemplate batchSqlSessionTemplate;

    protected ConfigurationDAOBatchService(
            @Qualifier(BatisConfig.SQL_BATCH_SESSION_TEMPLATE) final SqlSessionTemplate batchSqlSessionTemplate,
            final ExamConfigInitService examConfigInitService) {

        final org.apache.ibatis.session.Configuration batisConfig =
                batchSqlSessionTemplate.getConfiguration();
        this.examConfigInitService = examConfigInitService;

        log.info("Registered MyBatis Mappers: {}", batisConfig.getMapperRegistry().getMappers());

        // NOTE: sometimes this mapper was not registered on startup. No reason why. Force loading if absent.
        if (!batisConfig.hasMapper(ConfigurationNodeRecordMapper.class)) {
            batisConfig.addMapper(ConfigurationNodeRecordMapper.class);
        }

        if (!batisConfig.hasMapper(ConfigurationValueRecordMapper.class)) {
            batisConfig.addMapper(ConfigurationValueRecordMapper.class);
        }

        if (!batisConfig.hasMapper(ConfigurationAttributeRecordMapper.class)) {
            batisConfig.addMapper(ConfigurationAttributeRecordMapper.class);
        }

        if (!batisConfig.hasMapper(ConfigurationRecordMapper.class)) {
            batisConfig.addMapper(ConfigurationRecordMapper.class);
        }

        this.batchConfigurationNodeRecordMapper =
                batchSqlSessionTemplate.getMapper(ConfigurationNodeRecordMapper.class);
        this.batchConfigurationValueRecordMapper =
                batchSqlSessionTemplate.getMapper(ConfigurationValueRecordMapper.class);
        this.batchConfigurationAttributeRecordMapper =
                batchSqlSessionTemplate.getMapper(ConfigurationAttributeRecordMapper.class);
        this.batchConfigurationRecordMapper =
                batchSqlSessionTemplate.getMapper(ConfigurationRecordMapper.class);
        this.batchSqlSessionTemplate = batchSqlSessionTemplate;

    }

    Result<ConfigurationNode> createNewConfiguration(final ConfigurationNode data) {
        return Result.tryCatch(() -> {

            final Long count = this.batchConfigurationNodeRecordMapper.countByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            isEqualTo(data.name))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.type,
                            SqlBuilder.isEqualTo(data.type.name()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualTo(data.institutionId))
                    .build()
                    .execute();

            if (count != null && count > 0) {
                throw new FieldValidationException("name", "configurationNode:name:exists");
            }

            final ConfigurationNodeRecord newRecord = new ConfigurationNodeRecord(
                    null,
                    data.institutionId,
                    data.templateId,
                    data.owner,
                    data.name,
                    data.description,
                    data.type.name(),
                    (data.status != null) ? data.status.name() : ConfigurationStatus.CONSTRUCTION.name());

            this.batchConfigurationNodeRecordMapper.insert(newRecord);
            this.batchSqlSessionTemplate.flushStatements();
            return newRecord;
        })
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel)
                .flatMap(this::createInitialConfiguration);
    }

    Result<ConfigurationTableValues> saveNewTableValues(final ConfigurationTableValues value) {
        return checkInstitutionalIntegrity(value)
                .map(this::checkFollowUpIntegrity)
                .map(val -> {
                    final ConfigurationAttributeRecord record = this.batchConfigurationAttributeRecordMapper
                            .selectByPrimaryKey(val.attributeId);
                    if (record == null) {
                        throw new ResourceNotFoundException(
                                EntityType.CONFIGURATION_ATTRIBUTE,
                                String.valueOf(val.attributeId));
                    }
                    return record;
                })
                .map(attributeRecord -> {

                    final String type = attributeRecord.getType();
                    if (AttributeType.TABLE.name().equals(type)) {
                        saveAsTable(value, attributeRecord);
                    } else {
                        saveAsComposite(value);
                    }

                    return value;
                });
    }

    Result<Configuration> saveToHistory(final Long configurationNodeId) {
        return Result.tryCatch(() -> {

            // get follow-up configuration...
            final ConfigurationRecord followupConfig = getFollowupConfigurationRecord(configurationNodeId);

            // with actual attribute values
            final List<ConfigurationValueRecord> allValues = this.batchConfigurationValueRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(followupConfig.getId()))
                    .build()
                    .execute();

            // get current versions count

            // close follow-up configuration to save in history
            final ConfigurationRecord configUpdate = new ConfigurationRecord(
                    followupConfig.getId(),
                    null,
                    null,
                    generateVersionName(configurationNodeId),
                    DateTime.now(DateTimeZone.UTC),
                    BooleanUtils.toInteger(false));
            this.batchConfigurationRecordMapper.updateByPrimaryKeySelective(configUpdate);

            this.batchSqlSessionTemplate.flushStatements();

            // and create a new follow-up...
            final ConfigurationRecord newFollowup = new ConfigurationRecord(
                    null,
                    followupConfig.getInstitutionId(),
                    followupConfig.getConfigurationNodeId(),
                    null,
                    null,
                    BooleanUtils.toInteger(true));
            this.batchConfigurationRecordMapper.insert(newFollowup);

            this.batchSqlSessionTemplate.flushStatements();

            // with the current attribute values
            allValues.stream()
                    .map(oldValRec -> new ConfigurationValueRecord(
                            null,
                            oldValRec.getInstitutionId(),
                            newFollowup.getId(),
                            oldValRec.getConfigurationAttributeId(),
                            oldValRec.getListIndex(),
                            oldValRec.getValue()))
                    .forEach(this.batchConfigurationValueRecordMapper::insert);

            return this.batchConfigurationRecordMapper
                    .selectByPrimaryKey(newFollowup.getId());

        })
                .flatMap(ConfigurationDAOImpl::toDomainModel);
    }

    private String generateVersionName(final Long configurationNodeId) {
        final Long versions = this.batchConfigurationRecordMapper
                .countByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(configurationNodeId))
                .and(
                        ConfigurationRecordDynamicSqlSupport.followup,
                        isNotEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute();
        return "v" + versions;
    }

    Result<Configuration> undo(final Long configurationNodeId) {
        return Result.tryCatch(() -> {
            // get all configurations of the node
            final List<ConfigurationRecord> configs = this.batchConfigurationRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configurationNodeId))
                    .orderBy(ConfigurationRecordDynamicSqlSupport.versionDate)
                    .build()
                    .execute();

            return configs.get(configs.size() - 1);
        })
                .flatMap(rec -> restoreToVersion(configurationNodeId, rec.getId()));
    }

    Result<Configuration> restoreToDefaultValues(final Long configurationNodeId) {
        return Result.tryCatch(() -> {
            // get initial version that contains the default values either from base or from template
            return this.batchConfigurationRecordMapper.selectIdsByExample()
                    .where(
                            ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                            isEqualTo(configurationNodeId))
                    .and(
                            ConfigurationRecordDynamicSqlSupport.version,
                            isEqualTo(INITIAL_VERSION_NAME))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());

        })
                .flatMap(configId -> restoreToVersion(configurationNodeId, configId));
    }

    Result<Configuration> restoreToVersion(final Long configurationNodeId, final Long configId) {
        return Result.tryCatch(() -> {

            // get requested configuration in history...
            final ConfigurationRecord config = this.batchConfigurationRecordMapper
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
            final List<ConfigurationValueRecord> historicValues = this.batchConfigurationValueRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(config.getId()))
                    .build()
                    .execute();

            // get follow-up configuration id
            final ConfigurationRecord followup = getFollowupConfigurationRecord(configurationNodeId);

            // delete all values of the follow-up
            this.batchConfigurationValueRecordMapper
                    .deleteByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(followup.getId()))
                    .build()
                    .execute();

            // restore all current values of the follow-up with historic values
            historicValues.stream()
                    .map(historicValRec -> new ConfigurationValueRecord(
                            null,
                            followup.getInstitutionId(),
                            followup.getId(),
                            historicValRec.getConfigurationAttributeId(),
                            historicValRec.getListIndex(),
                            historicValRec.getValue()))
                    .forEach(this.batchConfigurationValueRecordMapper::insert);

            return followup;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel);
    }

    Result<ConfigurationNode> createCopy(
            final Long institutionId,
            final String newOwner,
            final ConfigCreationInfo copyInfo) {

        return Result.tryCatch(() -> {

            final Long count = this.batchConfigurationNodeRecordMapper.countByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            isEqualTo(copyInfo.name))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.type,
                            SqlBuilder.isEqualTo(copyInfo.configurationType.name()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            isEqualTo(institutionId))
                    .build()
                    .execute();

            if (count != null && count > 0) {
                throw new FieldValidationException("name", "configurationNode:name:exists");
            }

            final ConfigurationNodeRecord sourceNode = this.batchConfigurationNodeRecordMapper
                    .selectByPrimaryKey(copyInfo.configurationNodeId);

            if (!sourceNode.getInstitutionId().equals(institutionId)) {
                throw new IllegalArgumentException("Institution integrity violation");
            }

            return this.copyNodeRecord(sourceNode, newOwner, copyInfo);
        })
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    private ConfigurationNodeRecord copyNodeRecord(
            final ConfigurationNodeRecord nodeRec,
            final String newOwner,
            final ConfigCreationInfo copyInfo) {

        final ConfigurationNodeRecord newNodeRec = new ConfigurationNodeRecord(
                null,
                nodeRec.getInstitutionId(),
                nodeRec.getTemplateId(),
                StringUtils.isNotBlank(newOwner) ? newOwner : nodeRec.getOwner(),
                copyInfo.getName(),
                copyInfo.getDescription(),
                copyInfo.configurationType.name(),
                ConfigurationStatus.CONSTRUCTION.name());

        this.batchConfigurationNodeRecordMapper.insert(newNodeRec);
        this.batchSqlSessionTemplate.flushStatements();

        final List<ConfigurationRecord> configs = this.batchConfigurationRecordMapper
                .selectByExample()
                .where(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(nodeRec.getId()))
                .build()
                .execute();

        if (BooleanUtils.toBoolean(copyInfo.withHistory)) {
            configs.forEach(configRec -> this.copyConfiguration(
                    configRec.getInstitutionId(),
                    configRec.getId(),
                    newNodeRec.getId()));
        } else {
            configs
                    .stream()
                    .filter(configRec -> configRec.getVersionDate() == null)
                    .findFirst()
                    .ifPresent(configRec -> {
                        // No history means to create a first version and a follow-up with the copied values
                        final ConfigurationRecord newFirstVersion = new ConfigurationRecord(
                                null,
                                configRec.getInstitutionId(),
                                newNodeRec.getId(),
                                ConfigurationDAOBatchService.INITIAL_VERSION_NAME,
                                DateTime.now(DateTimeZone.UTC),
                                BooleanUtils.toInteger(false));
                        this.batchConfigurationRecordMapper.insert(newFirstVersion);
                        this.batchSqlSessionTemplate.flushStatements();
                        this.copyValues(
                                configRec.getInstitutionId(),
                                configRec.getId(),
                                newFirstVersion.getId());
                        // and copy the follow-up
                        final ConfigurationRecord followup = new ConfigurationRecord(
                                null,
                                configRec.getInstitutionId(),
                                newNodeRec.getId(),
                                null,
                                null,
                                BooleanUtils.toInteger(true));
                        this.batchConfigurationRecordMapper.insert(followup);
                        this.batchSqlSessionTemplate.flushStatements();
                        this.copyValues(
                                configRec.getInstitutionId(),
                                configRec.getId(),
                                followup.getId());
                    });
        }

        this.batchSqlSessionTemplate.flushStatements();
        return newNodeRec;
    }

    private Result<Configuration> copyConfiguration(
            final Long institutionId,
            final Long fromConfigurationId,
            final Long toConfigurationNodeId) {

        return Result.tryCatch(() -> {
            final ConfigurationRecord fromRecord = this.batchConfigurationRecordMapper
                    .selectByPrimaryKey(fromConfigurationId);

            if (!fromRecord.getInstitutionId().equals(institutionId)) {
                throw new IllegalArgumentException("Institution integrity violation");
            }

            final ConfigurationRecord configurationRecord = new ConfigurationRecord(
                    null,
                    fromRecord.getInstitutionId(),
                    toConfigurationNodeId,
                    fromRecord.getVersion(),
                    fromRecord.getVersionDate(),
                    fromRecord.getFollowup());
            this.batchConfigurationRecordMapper.insert(configurationRecord);
            this.batchSqlSessionTemplate.flushStatements();
            return configurationRecord;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel)
                .map(newConfig -> {
                    this.copyValues(
                            institutionId,
                            fromConfigurationId,
                            newConfig.getId());
                    return newConfig;
                });
    }

    private void copyValues(
            final Long institutionId,
            final Long fromConfigId,
            final Long toConfigId) {

        this.batchConfigurationValueRecordMapper
                .selectByExample()
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.institutionId,
                        isEqualTo(institutionId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        isEqualTo(fromConfigId))
                .build()
                .execute()
                .stream()
                .map(fromRec -> new ConfigurationValueRecord(
                        null,
                        fromRec.getInstitutionId(),
                        toConfigId,
                        fromRec.getConfigurationAttributeId(),
                        fromRec.getListIndex(),
                        fromRec.getValue()))
                .forEach(this.batchConfigurationValueRecordMapper::insert);
    }

    private ConfigurationRecord getFollowupConfigurationRecord(final Long configurationNodeId) {
        return this.batchConfigurationRecordMapper
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

    private void saveAsTable(
            final ConfigurationTableValues value,
            final ConfigurationAttributeRecord attributeRecord) {

        final Map<Long, ConfigurationAttributeRecord> attributeMap =
                this.batchConfigurationAttributeRecordMapper
                        .selectByExample()
                        .where(
                                ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                                isEqualTo(attributeRecord.getId()))
                        .build()
                        .execute()
                        .stream()
                        .collect(Collectors.toMap(ConfigurationAttributeRecord::getId, Function.identity()));

        final List<Long> columnAttributeIds = attributeMap.values()
                .stream()
                .map(ConfigurationAttributeRecord::getId)
                .collect(Collectors.toList());

        // first delete all old values of this table
        if (!columnAttributeIds.isEmpty()) {
            this.batchConfigurationValueRecordMapper.deleteByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(value.configurationId))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                            SqlBuilder.isIn(columnAttributeIds))
                    .build()
                    .execute();
        }

        // then add the new values
        for (final TableValue tableValue : value.values) {
            final ConfigurationAttributeRecord columnAttr = attributeMap.get(tableValue.attributeId);
            final ConfigurationValueRecord valueRecord = new ConfigurationValueRecord(
                    null,
                    value.institutionId,
                    value.configurationId,
                    columnAttr.getId(),
                    tableValue.listIndex,
                    tableValue.value);

            this.batchConfigurationValueRecordMapper.insertSelective(valueRecord);
        }
    }

    private void saveAsComposite(final ConfigurationTableValues value) {
        for (final TableValue tableValue : value.values) {

            final List<Long> valuePK = this.batchConfigurationValueRecordMapper.selectIdsByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(value.configurationId))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                            isEqualTo(tableValue.attributeId))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.listIndex,
                            isEqualTo(tableValue.listIndex))
                    .build()
                    .execute();

            if (valuePK != null && valuePK.size() > 1) {
                throw new IllegalStateException("Expected no more then one element");
            }

            if (valuePK == null || valuePK.isEmpty()) {
                // insert
                this.batchConfigurationValueRecordMapper.insert(
                        new ConfigurationValueRecord(
                                null,
                                value.institutionId,
                                value.configurationId,
                                tableValue.attributeId,
                                tableValue.listIndex,
                                tableValue.value));
            } else {
                // update
                this.batchConfigurationValueRecordMapper.updateByPrimaryKey(
                        new ConfigurationValueRecord(
                                valuePK.iterator().next(),
                                value.institutionId,
                                value.configurationId,
                                tableValue.attributeId,
                                tableValue.listIndex,
                                tableValue.value));
            }
        }
    }

    private Result<ConfigurationTableValues> checkInstitutionalIntegrity(final ConfigurationTableValues data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord r = this.batchConfigurationRecordMapper.selectByPrimaryKey(data.configurationId);
            if (r.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }
            return data;
        });
    }

    private ConfigurationTableValues checkFollowUpIntegrity(final ConfigurationTableValues data) {
        checkFollowUp(data.configurationId);
        return data;
    }

    private void checkFollowUp(final Long configurationId) {
        final ConfigurationRecord config = this.batchConfigurationRecordMapper
                .selectByPrimaryKey(configurationId);

        if (!BooleanUtils.toBoolean(config.getFollowup())) {
            throw new IllegalArgumentException(
                    "Forbidden to modify an configuration value of a none follow-up configuration");
        }
    }

    /*
     * Creates the first Configuration and a follow-up within a newly created ConfigurationNode.
     * This creates a first new Configuration and all attribute values for that either
     * from the default values or from template values if defined.
     * Then a follow-up Configuration is created with the same values to follow-up user input
     */
    private Result<ConfigurationNode> createInitialConfiguration(final ConfigurationNode config) {
        return Result.tryCatch(() -> {

            final ConfigurationRecord initConfig = new ConfigurationRecord(
                    null,
                    config.institutionId,
                    config.id,
                    INITIAL_VERSION_NAME,
                    DateTime.now(DateTimeZone.UTC),
                    BooleanUtils.toInteger(false));

            this.batchConfigurationRecordMapper.insert(initConfig);
            this.batchSqlSessionTemplate.flushStatements();

            createAttributeValues(config, initConfig)
                    .getOrThrow();

            final ConfigurationRecord followup = new ConfigurationRecord(
                    null,
                    config.institutionId,
                    config.id,
                    null,
                    null,
                    BooleanUtils.toInteger(true));

            this.batchConfigurationRecordMapper.insert(followup);
            this.batchSqlSessionTemplate.flushStatements();

            this.copyValues(
                    config.institutionId,
                    initConfig.getId(),
                    followup.getId());

            return config;
        });
    }

    /*
     * Creates all attribute values for a given ConfigurationNode with its newly created first ConfigurationRecord
     * If the ConfigurationNode has a templateId this will gather all attributes values from the latest
     * configuration of this ConfigurationNode template to override the default values.
     * Otherwise creates all attribute values from the default values.
     */
    private Result<ConfigurationNode> createAttributeValues(
            final ConfigurationNode configNode,
            final ConfigurationRecord config) {

        return Result.tryCatch(() -> {

            // go through all configuration attributes and create and store the default value
            this.batchConfigurationAttributeRecordMapper
                    .selectByExample()
                    .build()
                    .execute()
                    .stream()
                    // filter child attributes of tables. No default value for tables. Use templates for that
                    .filter(ConfigurationDAOBatchService::filterChildAttribute)
                    .forEach(attrRec -> this.batchConfigurationValueRecordMapper.insert(new ConfigurationValueRecord(
                            null,
                            configNode.institutionId,
                            config.getId(),
                            attrRec.getId(),
                            0,
                            attrRec.getDefaultValue())));

            // override with template values if available
            if (configNode.templateId == null || configNode.templateId.equals(ConfigurationNode.DEFAULT_TEMPLATE_ID)) {
                initAdditionalDefaultValues(configNode, config);
            } else {
                writeTemplateValues(configNode, config);
            }

            return configNode;
        });
    }

    private void initAdditionalDefaultValues(
            final ConfigurationNode configNode,
            final ConfigurationRecord config) {

        // get all attributes and map the names to id's
        final Map<String, ConfigurationAttribute> attributeMap = this.batchConfigurationAttributeRecordMapper
                .selectByExample()
                .build()
                .execute()
                .stream()
                .map(ConfigurationAttributeDAOImpl::toDomainModel)
                .map(Result::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        attr -> attr.name,
                        Function.identity()));

        this.examConfigInitService.getAdditionalDefaultValues(
                configNode.institutionId,
                config.getId(),
                attributeMap::get)
                .forEach(value -> {
                    final ConfigurationValueRecord valueRec = new ConfigurationValueRecord(
                            null,
                            value.institutionId,
                            value.configurationId,
                            value.attributeId,
                            value.listIndex,
                            value.value);

                    this.batchConfigurationValueRecordMapper.insert(valueRec);
                });

        this.batchSqlSessionTemplate.flushStatements();
    }

    private void writeTemplateValues(
            final ConfigurationNode configNode,
            final ConfigurationRecord config) {

        final List<ConfigurationValueRecord> templateValues = getTemplateValues(configNode);
        templateValues.forEach(templateValue -> {
            final Long existingId = this.batchConfigurationValueRecordMapper
                    .selectIdsByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(config.getId()))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                            isEqualTo(templateValue.getConfigurationAttributeId()))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.listIndex,
                            isEqualTo(templateValue.getListIndex()))
                    .build()
                    .execute()
                    .stream()
                    .findFirst()
                    .orElse(null);

            final ConfigurationValueRecord valueRec = new ConfigurationValueRecord(
                    existingId,
                    configNode.institutionId,
                    config.getId(),
                    templateValue.getConfigurationAttributeId(),
                    templateValue.getListIndex(),
                    templateValue.getValue());

            if (existingId != null) {
                this.batchConfigurationValueRecordMapper.updateByPrimaryKey(valueRec);
            } else {
                this.batchConfigurationValueRecordMapper.insert(valueRec);
            }
        });

        this.batchSqlSessionTemplate.flushStatements();
    }

    private static boolean filterChildAttribute(final ConfigurationAttributeRecord rec) {

        if (rec.getParentId() == null) {
            return true;
        }

        return BooleanUtils.toBoolean(ConfigurationAttribute.getDependencyValue(
                ConfigurationAttribute.DEPENDENCY_CREATE_DEFAULT_VALUE,
                rec.getDependencies()));
    }

    /*
     * Get values from template with configuration attribute id mapped to the value
     * returns empty list if no template available
     */
    private List<ConfigurationValueRecord> getTemplateValues(final ConfigurationNode configNode) {
        if (configNode.templateId == null || configNode.templateId.equals(ConfigurationNode.DEFAULT_TEMPLATE_ID)) {
            return Collections.emptyList();
        }

        final Long configurationId = this.batchConfigurationRecordMapper.selectByExample()
                .where(ConfigurationRecordDynamicSqlSupport.configurationNodeId, isEqualTo(configNode.templateId))
                .and(ConfigurationRecordDynamicSqlSupport.followup, isEqualTo(BooleanUtils.toIntegerObject(true)))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton())
                .getId();

        return this.batchConfigurationValueRecordMapper.selectByExample()
                .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isEqualTo(configurationId))
                .build()
                .execute();
    }

}
