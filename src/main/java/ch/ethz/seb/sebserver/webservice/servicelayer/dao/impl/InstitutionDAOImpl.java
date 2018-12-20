/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.InstitutionRecord;
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
    public Result<Institution> byId(final Long id) {
        return recordById(id)
                .flatMap(InstitutionDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Institution>> allActive() {
        return allMatching(null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Institution>> all(final Predicate<Institution> predicate, final boolean onlyActive) {
        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<InstitutionRecord>>> example =
                    this.institutionRecordMapper.selectByExample();

            final List<InstitutionRecord> records = (onlyActive)
                    ? example.where(UserRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(true)))
                            .build()
                            .execute()
                    : example.build().execute();

            return records.stream()
                    .map(InstitutionDAOImpl::toDomainModel)
                    .flatMap(Result::skipWithError)
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
                        SqlBuilder.isEqualTo(BooleanUtils.toInteger(BooleanUtils.isNotFalse(active))))
                .and(
                        InstitutionRecordDynamicSqlSupport.name,
                        SqlBuilder.isEqualToWhenPresent(Utils.toSQLWildcard(name)))
                .build()
                .execute()
                .stream()
                .map(InstitutionDAOImpl::toDomainModel)
                .flatMap(Result::skipWithError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<Institution> save(final Institution institution) {
        if (institution == null) {
            return Result.ofError(new NullPointerException("institution has null-reference"));
        }

        return (institution.id != null)
                ? updateUser(institution)
                        .flatMap(InstitutionDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback)
                : createNewUser(institution)
                        .flatMap(InstitutionDAOImpl::toDomainModel)
                        .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Institution> setActive(final String entityId, final boolean active) {
        return Result.tryCatch(() -> {
            final Long institutionId = Long.valueOf(entityId);

            this.institutionRecordMapper.updateByPrimaryKeySelective(new InstitutionRecord(
                    institutionId, null, null, BooleanUtils.toInteger(active), null));

            return this.institutionRecordMapper.selectByPrimaryKey(institutionId);
        }).flatMap(InstitutionDAOImpl::toDomainModel);
    }

    @Override
    public void notifyActivation(final Entity source) {
        // No dependencies of activation on Institution
    }

    @Override
    public void notifyDeactivation(final Entity source) {
        // No dependencies of activation on Institution
    }

    @Override
    @Transactional
    public Result<EntityProcessingReport> delete(final Long id, final boolean archive) {
        // TODO Auto-generated method stub
        return null;
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

    private Result<InstitutionRecord> createNewUser(final Institution institution) {
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

    private Result<InstitutionRecord> updateUser(final Institution institution) {
        return recordById(institution.id)
                .flatMap(record -> Result.tryCatch(() -> {

                    final InstitutionRecord newRecord = new InstitutionRecord(
                            institution.id,
                            institution.name,
                            institution.urlSuffix,
                            null,
                            institution.logoImage);

                    this.institutionRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.institutionRecordMapper.selectByPrimaryKey(institution.id);
                }));
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
