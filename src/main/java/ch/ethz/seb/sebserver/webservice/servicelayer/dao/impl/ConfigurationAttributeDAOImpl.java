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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ConfigurationAttributeDAOImpl implements ConfigurationAttributeDAO {

    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;
    private final ConfigurationValueRecordMapper configurationValueRecordMapper;
    private final OrientationRecordMapper orientationRecordMapper;

    protected ConfigurationAttributeDAOImpl(
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final OrientationRecordMapper orientationRecordMapper) {

        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.orientationRecordMapper = orientationRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_ATTRIBUTE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationAttribute> byPK(final Long id) {
        return recordById(id)
                .flatMap(ConfigurationAttributeDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationAttribute>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.configurationAttributeRecordMapper.selectByExample()
                    .where(ConfigurationAttributeRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationAttributeDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationAttribute>> allMatching(
            final FilterMap filterMap,
            final Predicate<ConfigurationAttribute> predicate) {

        return Result.tryCatch(() -> this.configurationAttributeRecordMapper
                .selectByExample()
                .where(
                        ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigAttributeParentId()))
                .and(
                        ConfigurationAttributeRecordDynamicSqlSupport.type,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigAttributeType()))
                .build()
                .execute()
                .stream()
                .map(ConfigurationAttributeDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationAttribute>> allChildAttributes(final Long parentId) {
        return Result.tryCatch(() -> this.configurationAttributeRecordMapper
                .selectByExample()
                .where(
                        ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                        SqlBuilder.isEqualTo(parentId))
                .build()
                .execute()
                .stream()
                .map(ConfigurationAttributeDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationAttribute>> getAllRootAttributes() {
        return Result.tryCatch(() -> this.configurationAttributeRecordMapper
                .selectByExample()
                .where(
                        ConfigurationAttributeRecordDynamicSqlSupport.parentId,
                        SqlBuilder.isNull())
                .build()
                .execute()
                .stream()
                .map(ConfigurationAttributeDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ConfigurationAttribute> createNew(final ConfigurationAttribute data) {
        return Result.tryCatch(() -> {
            final ConfigurationAttributeRecord newRecord = new ConfigurationAttributeRecord(
                    null,
                    data.name,
                    data.type.name(),
                    data.parentId,
                    data.resources,
                    data.validator,
                    data.dependencies,
                    data.defaultValue);

            this.configurationAttributeRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(ConfigurationAttributeDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationAttribute> save(final ConfigurationAttribute data) {
        return Result.tryCatch(() -> {

            final ConfigurationAttributeRecord newRecord = new ConfigurationAttributeRecord(
                    data.id,
                    data.name,
                    data.type.name(),
                    data.parentId,
                    data.resources,
                    data.validator,
                    data.dependencies,
                    data.defaultValue);

            this.configurationAttributeRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.configurationAttributeRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(ConfigurationAttributeDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            final List<EntityKey> result = new ArrayList<>();

            if (ids == null || ids.isEmpty()) {
                return result;
            }

            // if this is a complex attribute that has children, delete the children first
            final List<ConfigurationAttributeRecord> children =
                    this.configurationAttributeRecordMapper.selectByExample()
                            .where(ConfigurationAttributeRecordDynamicSqlSupport.parentId, isIn(ids))
                            .build()
                            .execute();

            // recursive call for children and adding result to the overall result set
            if (children != null && !children.isEmpty()) {
                result.addAll(delete(children.stream()
                        .map(r -> new EntityKey(r.getId(), EntityType.CONFIGURATION_ATTRIBUTE))
                        .collect(Collectors.toSet()))
                                .getOrThrow());
            }

            // delete all ConfigurationValue's that belongs to the ConfigurationAttributes to delete
            this.configurationValueRecordMapper.deleteByExample()
                    .where(
                            ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            // delete all Orientations that belongs to the ConfigurationAttributes to delete
            this.orientationRecordMapper.deleteByExample()
                    .where(
                            OrientationRecordDynamicSqlSupport.configAttributeId,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            // then delete all requested ConfigurationAttributes
            this.configurationAttributeRecordMapper.deleteByExample()
                    .where(ConfigurationAttributeRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            result.addAll(ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CONFIGURATION_ATTRIBUTE))
                    .collect(Collectors.toList()));

            return result;
        });
    }

    Result<ConfigurationAttributeRecord> recordById(final Long id) {
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

    static Result<ConfigurationAttribute> toDomainModel(final ConfigurationAttributeRecord record) {
        return Result.tryCatch(() -> new ConfigurationAttribute(
                record.getId(),
                record.getParentId(),
                record.getName(),
                AttributeType.valueOf(record.getType()),
                record.getResources(),
                record.getValidator(),
                record.getDependencies(),
                record.getDefaultValue()));
    }

}
