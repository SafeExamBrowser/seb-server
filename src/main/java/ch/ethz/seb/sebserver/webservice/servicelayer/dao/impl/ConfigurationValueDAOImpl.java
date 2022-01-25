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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigInitService;

@Lazy
@Component
@WebServiceProfile
public class ConfigurationValueDAOImpl implements ConfigurationValueDAO {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationValueDAOImpl.class);

    private final ConfigurationValueRecordMapper configurationValueRecordMapper;
    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;
    private final ConfigurationRecordMapper configurationRecordMapper;
    private final ConfigurationDAOBatchService configurationDAOBatchService;
    private final ExamConfigInitService examConfigInitService;

    protected ConfigurationValueDAOImpl(
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationDAOBatchService configurationDAOBatchService,
            final ExamConfigInitService examConfigInitService) {

        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationDAOBatchService = configurationDAOBatchService;
        this.examConfigInitService = examConfigInitService;
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

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

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

                        id = getByProperties(data)
                                .orElseGet(() -> {
                                    log.debug("Missing SEB exam configuration attrribute value for: {}", data);
                                    log.debug("Use self-healing strategy to recover from missing SEB exam "
                                            + "configuration attrribute value\n**** Create new AttributeValue for: {}",
                                            data);

                                    createNew(data);
                                    return getByProperties(data)
                                            .orElseThrow(() -> new ResourceNotFoundException(
                                                    EntityType.CONFIGURATION_VALUE,
                                                    String.valueOf(data.attributeId)));

                                });
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
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> extractPKsFromKeys(all)
                .stream()
                .map(pk -> {
                    this.configurationValueRecordMapper.deleteByPrimaryKey(pk);
                    return new EntityKey(pk, EntityType.CONFIGURATION_VALUE);
                })
                .collect(Collectors.toList()));
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
                    final ArrayList<Long> attrs = (attributeMapping != null && !attributeMapping.isEmpty())
                            ? new ArrayList<>(attributeMapping.keySet())
                            : null;
                    final List<TableValue> values = this.configurationValueRecordMapper.selectByExample()
                            .where(
                                    ConfigurationValueRecordDynamicSqlSupport.institutionId,
                                    isEqualTo(institutionId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                    isEqualTo(configurationId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                    SqlBuilder.isInWhenPresent(attrs))
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
            rows.sort(Comparator.naturalOrder());
            rows.forEach(i -> {

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

    @Override
    @Transactional
    public Result<ConfigurationTableValues> saveTableValues(final ConfigurationTableValues value) {
        return this.configurationDAOBatchService
                .saveNewTableValues(value)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Set<EntityKey>> setDefaultValues(
            final Long institutionId,
            final Long configurationId,
            final Long attributeId) {

        return attributeRecordById(attributeId)
                .flatMap(this::getAttributeMapping)
                .map(attributeMapping -> {

                    final Set<EntityKey> tableValues = new HashSet<>();
                    if (attributeMapping != null && !attributeMapping.isEmpty()) {
                        final ArrayList<Long> attrs = (attributeMapping != null && !attributeMapping.isEmpty())
                                ? new ArrayList<>(attributeMapping.keySet())
                                : null;
                        tableValues.addAll(this.configurationValueRecordMapper.selectByExample()
                                .where(
                                        ConfigurationValueRecordDynamicSqlSupport.institutionId,
                                        isEqualTo(institutionId))
                                .and(
                                        ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                        isEqualTo(configurationId))
                                .and(
                                        ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                        SqlBuilder.isInWhenPresent(attrs))
                                .build()
                                .execute()
                                .stream()
                                .map(r -> new EntityKey(r.getId(), EntityType.CONFIGURATION_VALUE))
                                .collect(Collectors.toSet()));

                        // if there are table values, delete them first and init defaults
                        if (tableValues != null && !tableValues.isEmpty()) {
                            this.delete(tableValues)
                                    .getOrThrow();

                            initTableValues(institutionId, configurationId, attributeId);
                        }
                    }

                    // get the attribute value reset to defaultValue and save
                    final List<ConfigurationValueRecord> values = this.configurationValueRecordMapper.selectByExample()
                            .where(
                                    ConfigurationValueRecordDynamicSqlSupport.institutionId,
                                    isEqualTo(institutionId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationId,
                                    isEqualTo(configurationId))
                            .and(
                                    ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                                    SqlBuilder.isEqualTo(attributeId))
                            .build()
                            .execute();

                    if (values.isEmpty()) {
                        return tableValues;
                    } else {
                        if (values.size() > 1) {
                            throw new IllegalStateException("Expected one but get: " + values.size());
                        }

                        final ConfigurationAttributeRecord attribute = this.configurationAttributeRecordMapper
                                .selectByPrimaryKey(attributeId);
                        final String defaultValue = attribute.getDefaultValue();

                        final ConfigurationValueRecord oldRec = values.get(0);
                        final ConfigurationValueRecord newRec = new ConfigurationValueRecord(
                                oldRec.getId(),
                                oldRec.getInstitutionId(),
                                oldRec.getConfigurationId(),
                                oldRec.getConfigurationAttributeId(),
                                oldRec.getListIndex(),
                                defaultValue);

                        this.configurationValueRecordMapper.updateByPrimaryKey(newRec);

                        final HashSet<EntityKey> result = new HashSet<>();
                        result.add(new EntityKey(newRec.getId(), EntityType.CONFIGURATION_VALUE));
                        result.addAll(tableValues);
                        return result;
                    }
                });
    }

    private void initTableValues(final Long institutionId, final Long configurationId, final Long attributeId) {
        // get table init values and save
        final List<Long> childAttributes = this.configurationAttributeRecordMapper.selectIdsByExample()
                .where(
                        ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                        isEqualTo(attributeId))
                .build()
                .execute();
        // get all attributes and map the names to id's
        final Map<String, ConfigurationAttribute> attributeMap = this.configurationAttributeRecordMapper
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

        this.examConfigInitService
                .getAdditionalDefaultValues(institutionId, configurationId, attributeMap::get)
                .stream()
                .filter(attrValue -> childAttributes.contains(attrValue.attributeId))
                .forEach(attrValue -> this.configurationValueRecordMapper
                        .insert(new ConfigurationValueRecord(
                                null,
                                attrValue.institutionId,
                                attrValue.configurationId,
                                attrValue.attributeId,
                                attrValue.listIndex,
                                attrValue.value)));
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
                    .collect(Collectors.toMap(ConfigurationAttributeRecord::getId, Function.identity()));
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

    private Result<ConfigurationValue> checkInstitutionalIntegrity(final ConfigurationValue data) {
        return Result.tryCatch(() -> {
            final ConfigurationRecord r = this.configurationRecordMapper.selectByPrimaryKey(data.configurationId);
            if (r.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }
            return data;
        });
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

        if (exists != null && exists > 0) {
            throw new IllegalArgumentException(
                    "The configuration value already exists");
        }

        return data;
    }

    private Optional<Long> getByProperties(final ConfigurationValue data) {
        return this.configurationValueRecordMapper.selectIdsByExample()
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
                .findFirst();
    }

}
