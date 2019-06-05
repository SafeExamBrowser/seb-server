/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationValueRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ConfigurationValueDAOImpl implements ConfigurationValueDAO {

    private final ConfigurationValueRecordMapper configurationValueRecordMapper;
    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;
    private final ConfigurationRecordMapper configurationRecordMapper;

    protected ConfigurationValueDAOImpl(
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final ConfigurationRecordMapper configurationRecordMapper) {

        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
        this.configurationRecordMapper = configurationRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_VALUE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationValue> byPK(final Long id) {
        return recordById(id)
                .flatMap(ConfigurationValueDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationValue>> allMatching(
            final FilterMap filterMap,
            final Predicate<ConfigurationValue> predicate) {

        return Result.tryCatch(() -> this.configurationValueRecordMapper
                .selectByExample()
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigValueConfigId()))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigValueAttributeId()))
                .build()
                .execute()
                .stream()
                .map(ConfigurationValueDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationValue>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {
            return this.configurationValueRecordMapper.selectByExample()
                    .where(ConfigurationValueRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationValueDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationValue>> allRootAttributeValues(
            final Long institutionId,
            final Long configurationId) {

        return Result.tryCatch(() -> this.configurationValueRecordMapper
                .selectByExample()
                .join(ConfigurationAttributeRecordDynamicSqlSupport.configurationAttributeRecord)
                .on(
                        ConfigurationAttributeRecordDynamicSqlSupport.id,
                        SqlBuilder.equalTo(ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId))
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(institutionId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        SqlBuilder.isEqualTo(configurationId))
                .and(
                        ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                        SqlBuilder.isNull())
                .build()
                .execute()
                .stream()
                .map(ConfigurationValueDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ConfigurationValue> createNew(final ConfigurationValue data) {
        return checkInstitutionalIntegrity(data)
                .map(this::checkFollowUpIntegrity)
                .map(this::checkCreationIntegrity)
                .flatMap(this::attributeRecord)
                .map(attributeRecord -> {

                    final String value = (data.value != null)
                            ? data.value
                            : attributeRecord.getDefaultValue();
                    final ConfigurationValueRecord newRecord = new ConfigurationValueRecord(
                            null,
                            data.institutionId,
                            data.configurationId,
                            data.attributeId,
                            data.listIndex,
                            value);

                    this.configurationValueRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .flatMap(ConfigurationValueDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationValue> save(final ConfigurationValue data) {
        return checkInstitutionalIntegrity(data)
                .map(this::checkFollowUpIntegrity)
                .flatMap(this::attributeRecord)
                .map(attributeRecord -> {

                    final Long id;
                    if (data.id == null) {
                        id = this.configurationValueRecordMapper.selectIdsByExample()
                                .where(
                                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                        isEqualTo(data.configurationId))
                                .and(
                                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                        isEqualTo(data.attributeId))
                                .and(
                                        ConfigurationValueRecordDynamicSqlSupport.listIndex,
                                        isEqualTo(data.listIndex))
                                .build()
                                .execute()
                                .stream()
                                .collect(Utils.toSingleton());
                    } else {
                        id = data.id;
                    }

                    final ConfigurationValueRecord newRecord = new ConfigurationValueRecord(
                            id,
                            null,
                            null,
                            null,
                            data.listIndex,
                            data.value);

                    this.configurationValueRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.configurationValueRecordMapper.selectByPrimaryKey(id);
                })
                .flatMap(ConfigurationValueDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationTableValues> getTableValues(
            final Long institutionId,
            final Long configurationId,
            final Long attributeId) {

        return attributeRecordById(attributeId)
                .flatMap(this::getAttributeMapping)
                .map(attributeMapping -> {
                    // get all values of the table
                    final List<TableValue> values = this.configurationValueRecordMapper.selectByExample()
                            .where(
                                    ConfigurationValueRecordDynamicSqlSupport.institutionId,
                                    isEqualTo(institutionId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                    isEqualTo(configurationId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                    SqlBuilder.isIn(new ArrayList<>(attributeMapping.keySet())))
                            .build()
                            .execute()
                            .stream()
                            .map(value -> new TableValue(
                                    value.getConfigurationAttributeId(),
                                    value.getListIndex(),
                                    value.getValue()))
                            .collect(Collectors.toList());

                    return new ConfigurationTableValues(
                            institutionId,
                            configurationId,
                            attributeId,
                            values);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<List<ConfigurationValue>>> getOrderedTableValues(
            final Long institutionId,
            final Long configurationId,
            final Long attributeId) {

        return Result.tryCatch(() -> {

            final List<ConfigurationAttributeRecord> attributes = this.configurationAttributeRecordMapper
                    .selectByExample()
                    .where(
                            ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                            SqlBuilder.isEqualTo(attributeId))
                    .build()
                    .execute()
                    .stream()
                    .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
                    .collect(Collectors.toList());

            final Map<Integer, Map<Long, ConfigurationValue>> indexMapping = new HashMap<>();
            this.configurationValueRecordMapper
                    .selectByExample()
                    .join(ConfigurationAttributeRecordDynamicSqlSupport.configurationAttributeRecord)
                    .on(
                            ConfigurationAttributeRecordDynamicSqlSupport.id,
                            SqlBuilder.equalTo(ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId))
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.institutionId,
                            isEqualTo(institutionId))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            isEqualTo(configurationId))
                    .and(
                            ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                            SqlBuilder.isEqualTo(attributeId))
                    .build()
                    .execute()
                    .stream()
                    .forEach(rec -> {
                        final Map<Long, ConfigurationValue> rowValues = indexMapping.computeIfAbsent(
                                rec.getListIndex(),
                                key -> new HashMap<>());
                        rowValues.put(
                                rec.getConfigurationAttributeId(),
                                ConfigurationValueDAOImpl
                                        .toDomainModel(rec)
                                        .getOrThrow());
                    });

            final List<List<ConfigurationValue>> result = new ArrayList<>();
            final List<Integer> rows = new ArrayList<>(indexMapping.keySet());
            rows.sort((i1, i2) -> i1.compareTo(i2));
            rows
                    .stream()
                    .forEach(i -> {

                        final Map<Long, ConfigurationValue> rowValuesMapping = indexMapping.get(i);
                        final List<ConfigurationValue> rowValues = attributes
                                .stream()
                                .map(attr -> rowValuesMapping.get(attr.getId()))
                                .collect(Collectors.toList());
                        result.add(rowValues);
                    });

            return result;
        });
    }

    // get all attributes of the table (columns) mapped to attribute id
    private Result<Map<Long, ConfigurationAttributeRecord>> getAttributeMapping(
            final ConfigurationAttributeRecord attributeRecord) {

        return Result.tryCatch(() -> {
            final List<ConfigurationAttributeRecord> columnAttributes =
                    this.configurationAttributeRecordMapper.selectByExample()
                            .where(
                                    ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                                    isEqualTo(attributeRecord.getId()))
                            .build()
                            .execute();

            return columnAttributes
                    .stream()
                    .collect(Collectors.toMap(attr -> attr.getId(), Function.identity()));
        });
    }

    @Override
    @Transactional
    public Result<ConfigurationTableValues> saveTableValues(final ConfigurationTableValues value) {
        return checkInstitutionalIntegrity(value)
                .map(this::checkFollowUpIntegrity)
                .flatMap(val -> attributeRecordById(val.attributeId))
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

    private void saveAsTable(
            final ConfigurationTableValues value,
            final ConfigurationAttributeRecord attributeRecord) {

        final Map<Long, ConfigurationAttributeRecord> attributeMap =
                this.configurationAttributeRecordMapper
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
        this.configurationValueRecordMapper.deleteByExample()
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        isEqualTo(value.configurationId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                        SqlBuilder.isIn(columnAttributeIds))
                .build()
                .execute();

        // then add the new values
        // TODO optimize with batching
        for (final TableValue tableValue : value.values) {
            final ConfigurationAttributeRecord columnAttr = attributeMap.get(tableValue.attributeId);
            final ConfigurationValueRecord valueRecord = new ConfigurationValueRecord(
                    null,
                    value.institutionId,
                    value.configurationId,
                    columnAttr.getId(),
                    tableValue.listIndex,
                    tableValue.value);

            this.configurationValueRecordMapper.insert(valueRecord);
        }
    }

    private void saveAsComposite(final ConfigurationTableValues value) {
        // TODO optimize with batching
        for (final TableValue tableValue : value.values) {

            final List<Long> valuePK = this.configurationValueRecordMapper.selectIdsByExample()
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
                this.configurationValueRecordMapper.insert(
                        new ConfigurationValueRecord(
                                null,
                                value.institutionId,
                                value.configurationId,
                                tableValue.attributeId,
                                tableValue.listIndex,
                                tableValue.value));
            } else {
                // update
                this.configurationValueRecordMapper.updateByPrimaryKey(
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

    private Result<ConfigurationAttributeRecord> attributeRecord(final ConfigurationValue value) {
        return attributeRecordById(value.attributeId);
    }

    private Result<ConfigurationAttributeRecord> attributeRecordById(final Long id) {
        return Result.tryCatch(() -> {
            final ConfigurationAttributeRecord record = this.configurationAttributeRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION_ATTRIBUTE,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<ConfigurationValueRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ConfigurationValueRecord record = this.configurationValueRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION_VALUE,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private static Result<ConfigurationValue> toDomainModel(final ConfigurationValueRecord record) {
        return Result.tryCatch(() -> {

            return new ConfigurationValue(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getConfigurationId(),
                    record.getConfigurationAttributeId(),
                    record.getListIndex(),
                    record.getValue());
        });
    }

    private Result<ConfigurationValue> checkInstitutionalIntegrity(final ConfigurationValue data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord r = this.configurationRecordMapper.selectByPrimaryKey(data.configurationId);
            if (r.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }
            return data;
        });
    }

    private Result<ConfigurationTableValues> checkInstitutionalIntegrity(final ConfigurationTableValues data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord r = this.configurationRecordMapper.selectByPrimaryKey(data.configurationId);
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

    private ConfigurationValue checkFollowUpIntegrity(final ConfigurationValue data) {
        checkFollowUp(data.configurationId);
        return data;
    }

    private void checkFollowUp(final Long configurationId) {
        final ConfigurationRecord config = this.configurationRecordMapper
                .selectByPrimaryKey(configurationId);

        if (!BooleanUtils.toBoolean(config.getFollowup())) {
            throw new IllegalArgumentException(
                    "Forbidden to modify an configuration value of a none follow-up configuration");
        }
    }

    private ConfigurationValue checkCreationIntegrity(final ConfigurationValue data) {
        final Long exists = this.configurationValueRecordMapper.countByExample()
                .where(
                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                        isEqualTo(data.configurationId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                        isEqualTo(data.attributeId))
                .and(
                        ConfigurationValueRecordDynamicSqlSupport.listIndex,
                        isEqualTo(data.listIndex))
                .build()
                .execute();

        if (exists != null && exists.longValue() > 0) {
            throw new IllegalArgumentException(
                    "The configuration value already exists");
        }

        return data;
    }

}
