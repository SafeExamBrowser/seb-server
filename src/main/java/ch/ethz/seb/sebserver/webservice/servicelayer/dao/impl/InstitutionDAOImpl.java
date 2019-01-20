/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualToWhenPresent;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.InstitutionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
public class InstitutionDAOImpl implements InstitutionDAO {

    private final InstitutionRecordMapper institutionRecordMapper;

    public InstitutionDAOImpl(final InstitutionRecordMapper institutionRecordMapper) {
        this.institutionRecordMapper = institutionRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.INSTITUTION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Institution> byPK(final Long id) {
        return recordById(id)
                .flatMap(InstitutionDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Institution>> all(final Predicate<Institution> predicate, final Boolean active) {
        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<InstitutionRecord>>> example =
                    this.institutionRecordMapper.selectByExample();

            final List<InstitutionRecord> records = (active != null)
                    ? example
                            .where(
                                    InstitutionRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : example.build().execute();

            return records.stream()
                    .map(InstitutionDAOImpl::toDomainModel)
                    .flatMap(Result::skipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Institution>> allMatching(final String name, final Boolean active) {
        return Result.tryCatch(() -> this.institutionRecordMapper
                .selectByExample()
                .where(
                        InstitutionRecordDynamicSqlSupport.active,
                        SqlBuilder.isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                .and(
                        InstitutionRecordDynamicSqlSupport.name,
                        SqlBuilder.isEqualToWhenPresent(Utils.toSQLWildcard(name)))
                .build()
                .execute()
                .stream()
                .map(InstitutionDAOImpl::toDomainModel)
                .flatMap(Result::skipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<Institution> save(final Institution institution) {
        if (institution == null) {
            return Result.ofError(new NullPointerException("institution has null-reference"));
        }

        return (institution.id != null)
                ? update(institution)
                        .flatMap(InstitutionDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback)
                : createNew(institution)
                        .flatMap(InstitutionDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        final List<Long> ids = extractPKsFromKeys(all);
        final InstitutionRecord institutionRecord = new InstitutionRecord(
                null, null, null, BooleanUtils.toInteger(active), null);

        try {
            this.institutionRecordMapper.updateByExampleSelective(institutionRecord)
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.INSTITUTION)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Activation failed on unexpected exception for Institution of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> delete(final Set<EntityKey> all) {
        final List<Long> ids = extractPKsFromKeys(all);

        try {
            this.institutionRecordMapper.deleteByExample()
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.INSTITUTION)))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Deletion failed on unexpected exception for Institution of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        // NOTE since Institution is the top most Entity, there are no other Entity for that an Institution depends on.
        return Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Institution>> bulkLoadEntities(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            final List<Long> ids = extractPKsFromKeys(keys);

            return this.institutionRecordMapper.selectByExample()
                    .where(InstitutionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(InstitutionDAOImpl::toDomainModel)
                    .map(res -> res.getOrThrow())
                    .collect(Collectors.toList());
        });
    }

    private Result<InstitutionRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final InstitutionRecord record = this.institutionRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.INSTITUTION,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<InstitutionRecord> createNew(final Institution institution) {
        return Result.tryCatch(() -> {
            final InstitutionRecord newRecord = new InstitutionRecord(
                    null,
                    institution.name,
                    institution.urlSuffix,
                    BooleanUtils.toInteger(false),
                    institution.logoImage);

            this.institutionRecordMapper.insert(newRecord);
            return newRecord;
        });
    }

    private Result<InstitutionRecord> update(final Institution institution) {
        return Result.tryCatch(() -> {

            final InstitutionRecord newRecord = new InstitutionRecord(
                    institution.id,
                    institution.name,
                    institution.urlSuffix,
                    null,
                    institution.logoImage);

            this.institutionRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.institutionRecordMapper.selectByPrimaryKey(institution.id);
        });
    }

    private static Result<Institution> toDomainModel(final InstitutionRecord record) {
        return Result.tryCatch(() -> new Institution(
                record.getId(),
                record.getName(),
                record.getUrlSuffix(),
                record.getLogoImage(),
                BooleanUtils.toBooleanObject(record.getActive())));
    }

}
