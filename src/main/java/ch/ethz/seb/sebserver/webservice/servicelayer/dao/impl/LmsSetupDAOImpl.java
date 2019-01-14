/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.gbl.util.Utils.toSQLWildcard;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.LmsSetupRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
public class LmsSetupDAOImpl implements LmsSetupDAO, BulkActionSupport {

    private static final Logger log = LoggerFactory.getLogger(LmsSetupDAOImpl.class);

    private final LmsSetupRecordMapper lmsSetupRecordMapper;

    public LmsSetupDAOImpl(final LmsSetupRecordMapper lmsSetupRecordMapper) {
        this.lmsSetupRecordMapper = lmsSetupRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.LMS_SETUP;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<LmsSetup> byId(final Long id) {
        return recordById(id)
                .flatMap(LmsSetupDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<LmsSetup>> all(final Predicate<LmsSetup> predicate, final Boolean active) {
        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<LmsSetupRecord>>> example =
                    this.lmsSetupRecordMapper.selectByExample();

            final List<LmsSetupRecord> records = (active != null)
                    ? example
                            .where(LmsSetupRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(active)))
                            .build()
                            .execute()
                    : example.build().execute();

            return records.stream()
                    .map(LmsSetupDAOImpl::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<LmsSetup>> allMatching(
            final Long institutionId,
            final String name,
            final LmsType lmsType,
            final Boolean active) {

        return Result.tryCatch(() -> {

            final String _lmsType = (lmsType != null) ? lmsType.name() : null;
            return this.lmsSetupRecordMapper
                    .selectByExample()
                    .where(LmsSetupRecordDynamicSqlSupport.institutionId, isEqualToWhenPresent(institutionId))
                    .and(LmsSetupRecordDynamicSqlSupport.name, isLikeWhenPresent(toSQLWildcard(name)))
                    .and(LmsSetupRecordDynamicSqlSupport.lmsType, isEqualToWhenPresent(_lmsType))
                    .and(LmsSetupRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                    .build()
                    .execute()
                    .stream()
                    .map(LmsSetupDAOImpl::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<LmsSetup> save(final LmsSetup lmsSetup) {
        if (lmsSetup == null) {
            return Result.ofError(new NullPointerException("lmsSetup has null-reference"));
        }

        return (lmsSetup.id != null)
                ? update(lmsSetup)
                        .flatMap(LmsSetupDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback)
                : createNew(lmsSetup)
                        .flatMap(LmsSetupDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        final Collection<Result<EntityKey>> result = new ArrayList<>();

        final List<Long> ids = extractIdsFromKeys(all, result);
        final LmsSetupRecord lmsSetupRecord = new LmsSetupRecord(
                null, null, null, null, null, null, null, null, null, null,
                BooleanUtils.toIntegerObject(active));

        try {
            this.lmsSetupRecordMapper.updateByExampleSelective(lmsSetupRecord)
                    .where(LmsSetupRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.LMS_SETUP)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Activation failed on unexpected exception for LmsSetup of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> delete(final Set<EntityKey> all) {
        final Collection<Result<EntityKey>> result = new ArrayList<>();

        final List<Long> ids = extractIdsFromKeys(all, result);

        try {
            this.lmsSetupRecordMapper.deleteByExample()
                    .where(LmsSetupRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.LMS_SETUP)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Deletion failed on unexpected exception for LmsSetup of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        // all of institution
        if (bulkAction.sourceType == EntityType.INSTITUTION) {
            final Set<EntityKey> result = new HashSet<>();
            for (final EntityKey sourceKey : bulkAction.sources) {
                try {
                    result.addAll(this.lmsSetupRecordMapper.selectIdsByExample()
                            .where(LmsSetupRecordDynamicSqlSupport.institutionId,
                                    isEqualTo(Long.valueOf(sourceKey.entityId)))
                            .build()
                            .execute()
                            .stream()
                            .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
                            .collect(Collectors.toList()));
                } catch (final Exception e) {
                    log.error("Unexpected error: ", e);
                    return Collections.emptySet();
                }
            }
            return result;
        }

        return Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Entity>> bulkLoadEntities(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            final Collection<Result<EntityKey>> result = new ArrayList<>();
            final List<Long> ids = extractIdsFromKeys(keys, result);

            return this.lmsSetupRecordMapper.selectByExample()
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(LmsSetupDAOImpl::toDomainModel)
                    .map(res -> res.getOrThrow())
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> processBulkAction(final BulkAction bulkAction) {
        final Set<EntityKey> all = bulkAction.extractKeys(EntityType.LMS_SETUP);

        switch (bulkAction.type) {
            case ACTIVATE:
                return setActive(all, true);
            case DEACTIVATE:
                return setActive(all, false);
            case HARD_DELETE:
                return delete(all);
        }

        // should never happen
        throw new UnsupportedOperationException("Unsupported Bulk Action: " + bulkAction);
    }

    private Result<LmsSetupRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final LmsSetupRecord record = this.lmsSetupRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.LMS_SETUP,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private static Result<LmsSetup> toDomainModel(final LmsSetupRecord record) {
        return Result.tryCatch(() -> new LmsSetup(
                record.getId(),
                record.getInstitutionId(),
                record.getName(),
                LmsType.valueOf(record.getLmsType()),
                record.getLmsClientname(),
                record.getLmsClientsecret(),
                record.getLmsUrl(),
                record.getLmsRestApiToken(),
                record.getSebClientname(),
                record.getSebClientsecret(),
                BooleanUtils.toBooleanObject(record.getActive())));
    }

    private Result<LmsSetupRecord> createNew(final LmsSetup lmsSetup) {
        return Result.tryCatch(() -> {

            final LmsSetupRecord newRecord = new LmsSetupRecord(
                    null,
                    lmsSetup.institutionId,
                    lmsSetup.name,
                    (lmsSetup.lmsType != null) ? lmsSetup.lmsType.name() : null,
                    lmsSetup.lmsApiUrl,
                    lmsSetup.lmsAuthName,
                    lmsSetup.lmsAuthSecret,
                    lmsSetup.lmsRestApiToken,
                    lmsSetup.sebAuthName,
                    lmsSetup.sebAuthSecret,
                    BooleanUtils.toInteger(false));

            this.lmsSetupRecordMapper.insert(newRecord);
            return newRecord;
        });
    }

    private Result<LmsSetupRecord> update(final LmsSetup lmsSetup) {
        return recordById(lmsSetup.id)
                .map(record -> {

                    final LmsSetupRecord newRecord = new LmsSetupRecord(
                            lmsSetup.id,
                            lmsSetup.institutionId,
                            lmsSetup.name,
                            (lmsSetup.lmsType != null) ? lmsSetup.lmsType.name() : null,
                            lmsSetup.lmsApiUrl,
                            lmsSetup.lmsAuthName,
                            lmsSetup.lmsAuthSecret,
                            lmsSetup.lmsRestApiToken,
                            lmsSetup.sebAuthName,
                            lmsSetup.sebAuthSecret,
                            null);

                    this.lmsSetupRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.lmsSetupRecordMapper.selectByPrimaryKey(lmsSetup.id);
                });
    }

}
