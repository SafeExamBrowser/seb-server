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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationValueRecord;
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
    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;

    protected ConfigurationNodeDAOImpl(
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper) {

        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
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
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
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
                        SqlBuilder.isEqualToWhenPresent(filterMap.getName()))
                .and(
                        ConfigurationNodeRecordDynamicSqlSupport.description,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeDesc()))
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
                .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
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
        return Result.tryCatch(() -> {

            final Long count = this.configurationNodeRecordMapper.countByExample()
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

            this.configurationNodeRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(ConfigurationNodeDAOImpl::toDomainModel)
                .flatMap(this::createInitialConfiguration)
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
                    data.name,
                    data.description,
                    null,
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

    private static Result<ConfigurationNode> toDomainModel(final ConfigurationNodeRecord record) {
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

            this.configurationRecordMapper.insert(initConfig);
            createAttributeValues(config, initConfig)
                    .getOrThrow();

            final ConfigurationRecord followup = new ConfigurationRecord(
                    null,
                    config.institutionId,
                    config.id,
                    null,
                    null,
                    BooleanUtils.toInteger(true));

            this.configurationRecordMapper.insert(followup);
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
            this.configurationAttributeRecordMapper
                    .selectByExample()
                    .build()
                    .execute()
                    .stream()
                    .forEach(attrRec -> {
                        final boolean bigValue = ConfigurationValueDAOImpl.isBigValue(attrRec);
                        final String value = templateValues.getOrDefault(
                                attrRec.getId(),
                                attrRec.getDefaultValue());

                        //if (StringUtils.isNoneBlank(value)) {
                        this.configurationValueRecordMapper.insert(new ConfigurationValueRecord(
                                null,
                                configNode.institutionId,
                                config.getId(),
                                attrRec.getId(),
                                0,
                                bigValue ? null : value,
                                bigValue ? value : null));
                        //}
                    });

            return configNode;
        });
    }

    /*
     * Get values from template with configuration attribute id mapped to the value
     * returns empty list if no template available
     */
    private Map<Long, String> getTemplateValues(final ConfigurationNode configNode) {
        if (configNode.templateId == null || configNode.templateId.equals(ConfigurationNode.DEFAULT_TEMPLATE_ID)) {
            return Collections.emptyMap();
        }

        final Long configurationId = this.configurationRecordMapper.selectByExample()
                .where(ConfigurationRecordDynamicSqlSupport.configurationNodeId, isEqualTo(configNode.id))
                .and(ConfigurationRecordDynamicSqlSupport.followup, isEqualTo(BooleanUtils.toIntegerObject(true)))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton())
                .getId();

        return this.configurationValueRecordMapper.selectByExample()
                .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isEqualTo(configurationId))
                .build()
                .execute()
                .stream()
                .collect(Collectors.toMap(
                        valRec -> valRec.getConfigurationAttributeId(),
                        valRec -> (valRec.getValue() != null)
                                ? valRec.getValue()
                                : valRec.getText()));
    }

}
