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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
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

@Lazy
@Component
@WebServiceProfile
class ConfigurationDAOBatchService {

    private final ConfigurationNodeRecordMapper batchConfigurationNodeRecordMapper;
    private final ConfigurationValueRecordMapper batchConfigurationValueRecordMapper;
    private final ConfigurationAttributeRecordMapper batchConfigurationAttributeRecordMapper;
    private final ConfigurationRecordMapper batchConfigurationRecordMapper;

    private final SqlSessionTemplate batchSqlSessionTemplate;

    protected ConfigurationDAOBatchService(
            @Qualifier(BatisConfig.SQL_BATCH_SESSION_TEMPLATE) final SqlSessionTemplate batchSqlSessionTemplate) {

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
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualTo(data.institutionId))
                    .build()
                    .execute();

            if (count != null && count.longValue() > 0) {
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

            // close follow-up configuration to save in history
            final ConfigurationRecord configUpdate = new ConfigurationRecord(
                    followupConfig.getId(),
                    null,
                    null,
                    "v" + versions,
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
                    .forEach(newValRec -> this.batchConfigurationValueRecordMapper.insert(newValRec));

            return newFollowup;

        })
                .flatMap(ConfigurationDAOImpl::toDomainModel);
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
                    .forEach(newValRec -> this.batchConfigurationValueRecordMapper.insert(newValRec));

            return followup;
        })
                .flatMap(ConfigurationDAOImpl::toDomainModel);
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
                        .collect(Collectors.toMap(rec -> rec.getId(), Function.identity()));

        final List<Long> columnAttributeIds = attributeMap.values()
                .stream()
                .map(a -> a.getId())
                .collect(Collectors.toList());

        // first delete all old values of this table
        this.batchConfigurationValueRecordMapper.deleteByExample()
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        isEqualTo(value.configurationId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                        SqlBuilder.isIn(columnAttributeIds))
                .build()
                .execute();

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
                    "v0", // TODO?
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

            createAttributeValues(config, followup)
                    .getOrThrow();

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

            // templateValues to override default values if available
            final Map<Long, String> templateValues = getTemplateValues(configNode);

            // go through all configuration attributes and create and store a
            // configuration value from either the default value or the value from the template
            this.batchConfigurationAttributeRecordMapper
                    .selectByExample()
                    .build()
                    .execute()
                    .stream()
                    // filter child attributes of tables. No default value for tables. Use templates for that
                    .filter(ConfigurationDAOBatchService::filterChildAttribute)
                    .forEach(attrRec -> {
                        final String value = templateValues.getOrDefault(
                                attrRec.getId(),
                                attrRec.getDefaultValue());

                        this.batchConfigurationValueRecordMapper.insert(new ConfigurationValueRecord(
                                null,
                                configNode.institutionId,
                                config.getId(),
                                attrRec.getId(),
                                0,
                                value));
                    });

            this.batchSqlSessionTemplate.flushStatements();

            return configNode;
        });
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
    private Map<Long, String> getTemplateValues(final ConfigurationNode configNode) {
        if (configNode.templateId == null || configNode.templateId.equals(ConfigurationNode.DEFAULT_TEMPLATE_ID)) {
            return Collections.emptyMap();
        }

        final Long configurationId = this.batchConfigurationRecordMapper.selectByExample()
                .where(ConfigurationRecordDynamicSqlSupport.configurationNodeId, isEqualTo(configNode.id))
                .and(ConfigurationRecordDynamicSqlSupport.followup, isEqualTo(BooleanUtils.toIntegerObject(true)))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton())
                .getId();

        return this.batchConfigurationValueRecordMapper.selectByExample()
                .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isEqualTo(configurationId))
                .build()
                .execute()
                .stream()
                .collect(Collectors.toMap(
                        valRec -> valRec.getConfigurationAttributeId(),
                        valRec -> valRec.getValue()));
    }

}
