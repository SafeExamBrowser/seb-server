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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeValueType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
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
                .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
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
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
                    .collect(Collectors.toList());
        });
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
                    final boolean bigValue = isBigValue(attributeRecord);
                    final ConfigurationValueRecord newRecord = new ConfigurationValueRecord(
                            null,
                            data.institutionId,
                            data.configurationId,
                            data.attributeId,
                            data.listIndex,
                            (bigValue) ? null : value,
                            (bigValue) ? value : null);

                    this.configurationValueRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .flatMap(ConfigurationValueDAOImpl::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationValue> save(final ConfigurationValue data) {
        return checkInstitutionalIntegrity(data)
                .map(this::checkFollowUpIntegrity)
                .flatMap(this::attributeRecord)
                .map(attributeRecord -> {

                    final boolean bigValue = isBigValue(attributeRecord);
                    final ConfigurationValueRecord newRecord = new ConfigurationValueRecord(
                            data.id,
                            null,
                            null,
                            null,
                            data.listIndex,
                            (bigValue) ? null : data.value,
                            (bigValue) ? data.value : null);

                    this.configurationValueRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.configurationValueRecordMapper.selectByPrimaryKey(data.id);
                })
                .flatMap(ConfigurationValueDAOImpl::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationTableValue> getTableValue(
            final Long institutionId,
            final Long attributeId,
            final Long configurationId) {

        return attributeRecordById(attributeId)
                .map(attributeRecord -> {

                    // get all attributes of the table (columns)
                    final List<ConfigurationAttributeRecord> columnAttributes =
                            this.configurationAttributeRecordMapper.selectByExample()
                                    .where(
                                            ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                                            isEqualTo(attributeRecord.getId()))
                                    .build()
                                    .execute();

                    final List<Long> columnAttributeIds = columnAttributes.stream()
                            .map(a -> a.getId())
                            .collect(Collectors.toList());

                    // get all values of the table and group them by attribute and sorted by list/row index
                    final List<ConfigurationValueRecord> valueRecords =
                            this.configurationValueRecordMapper.selectByExample()
                                    .where(
                                            ConfigurationValueRecordDynamicSqlSupport.institutionId,
                                            isEqualTo(institutionId))
                                    .and(
                                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                            isEqualTo(configurationId))
                                    .and(
                                            ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                            SqlBuilder.isIn(columnAttributeIds))
                                    .build()
                                    .execute();

                    int rows = 0;
                    final List<String> values = new ArrayList<>();
                    final Map<Long, List<ConfigurationValueRecord>> valueMapping = new LinkedHashMap<>();
                    for (final ConfigurationValueRecord valueRecord : valueRecords) {
                        final List<ConfigurationValueRecord> list = valueMapping.putIfAbsent(
                                valueRecord.getId(),
                                new ArrayList<>());
                        list.add(valueRecord);
                        list.sort((r1, r2) -> r1.getListIndex().compareTo(r2.getListIndex()));
                        rows = list.size();
                    }

                    for (int row = 0; row < rows; row++) {
                        for (final ConfigurationAttributeRecord aRecord : columnAttributes) {
                            final List<ConfigurationValueRecord> list = valueMapping.get(aRecord.getId());
                            if (list != null) {
                                final ConfigurationValueRecord valueRecord = list.get(row);
                                if (valueRecord != null) {
                                    values.add((isBigValue(aRecord)) ? valueRecord.getText() : valueRecord.getValue());
                                    continue;
                                }
                            }

                            values.add(null);
                        }
                    }

                    return new ConfigurationTableValue(
                            institutionId,
                            configurationId,
                            attributeId,
                            new ArrayList<>(valueMapping.keySet()),
                            values);
                });
    }

    @Override
    @Transactional
    public Result<ConfigurationTableValue> saveTableValue(final ConfigurationTableValue value) {
        return checkInstitutionalIntegrity(value)
                .map(this::checkFollowUpIntegrity)
                .flatMap(val -> attributeRecordById(val.attributeId))
                .map(attributeRecord -> {

                    final List<ConfigurationAttributeRecord> columnAttributes =
                            this.configurationAttributeRecordMapper.selectByExample()
                                    .where(
                                            ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                                            isEqualTo(attributeRecord.getId()))
                                    .build()
                                    .execute();

                    final List<Long> columnAttributeIds = columnAttributes.stream()
                            .map(a -> a.getId())
                            .collect(Collectors.toList());
                    final int columns = columnAttributeIds.size();

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
                    int columnIndex = 0;
                    int rowIndex = 0;
                    for (final String val : value.values) {
                        final ConfigurationAttributeRecord columnAttr = columnAttributes.get(columnIndex);
                        final boolean bigValue = isBigValue(columnAttr);
                        final ConfigurationValueRecord valueRecord = new ConfigurationValueRecord(
                                null,
                                value.institutionId,
                                value.configurationId,
                                columnAttr.getId(),
                                rowIndex,
                                (bigValue) ? null : val,
                                (bigValue) ? val : null);

                        this.configurationValueRecordMapper.insert(valueRecord);

                        columnIndex++;
                        if (columnIndex >= columns) {
                            columnIndex = 0;
                            rowIndex++;
                        }
                    }

                    return value;
                });
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
        return Result.tryCatch(() -> new ConfigurationValue(
                record.getId(),
                record.getInstitutionId(),
                record.getConfigurationId(),
                record.getConfigurationAttributeId(),
                record.getListIndex(),
                record.getValue()));
    }

    public static boolean isBigValue(final ConfigurationAttributeRecord attributeRecord) {
        try {
            final AttributeType type = AttributeType.valueOf(attributeRecord.getType());
            return type.attributeValueType == AttributeValueType.LARGE_TEXT
                    || type.attributeValueType == AttributeValueType.BASE64_BINARY;
        } catch (final Exception e) {
            return false;
        }
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

    private Result<ConfigurationTableValue> checkInstitutionalIntegrity(final ConfigurationTableValue data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord r = this.configurationRecordMapper.selectByPrimaryKey(data.configurationId);
            if (r.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }
            return data;
        });
    }

    private ConfigurationTableValue checkFollowUpIntegrity(final ConfigurationTableValue data) {
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
