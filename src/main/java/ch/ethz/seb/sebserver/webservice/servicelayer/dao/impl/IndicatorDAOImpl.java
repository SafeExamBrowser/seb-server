/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ThresholdRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ThresholdRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.IndicatorRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ThresholdRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class IndicatorDAOImpl implements IndicatorDAO {

    private final IndicatorRecordMapper indicatorRecordMapper;
    private final ThresholdRecordMapper thresholdRecordMapper;

    public IndicatorDAOImpl(
            final IndicatorRecordMapper indicatorRecordMapper,
            final ThresholdRecordMapper thresholdRecordMapper) {

        this.indicatorRecordMapper = indicatorRecordMapper;
        this.thresholdRecordMapper = thresholdRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.INDICATOR;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Indicator> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Indicator>> allMatching(final FilterMap filterMap, final Predicate<Indicator> predicate) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        SqlBuilder.equalTo(IndicatorRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        IndicatorRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getIndicatorExamId()))
                .and(
                        IndicatorRecordDynamicSqlSupport.name,
                        isLikeWhenPresent(filterMap.getIndicatorName()))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Indicator>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.indicatorRecordMapper.selectByExample()
                    .where(IndicatorRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Indicator> save(final Indicator modified) {
        return Result.tryCatch(() -> {

            final IndicatorRecord newRecord = new IndicatorRecord(
                    modified.id,
                    null,
                    modified.type.name(),
                    modified.name,
                    modified.defaultColor,
                    modified.defaultIcon,
                    modified.tags);

            this.indicatorRecordMapper.updateByPrimaryKeySelective(newRecord);

            // update also the thresholds
            this.thresholdRecordMapper.deleteByExample()
                    .where(ThresholdRecordDynamicSqlSupport.indicatorId, isEqualTo(modified.id))
                    .build()
                    .execute();

            modified.thresholds
                    .stream()
                    .map(threshold -> new ThresholdRecord(
                            null,
                            modified.id,
                            new BigDecimal(threshold.value),
                            threshold.color,
                            threshold.icon))
                    .forEach(this.thresholdRecordMapper::insert);

            return this.indicatorRecordMapper.selectByPrimaryKey(modified.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Indicator> createNew(final Indicator modified) {
        return Result.tryCatch(() -> {

            final IndicatorRecord newRecord = new IndicatorRecord(
                    null,
                    modified.examId,
                    modified.type.name(),
                    modified.name,
                    modified.defaultColor,
                    modified.defaultIcon,
                    modified.tags);

            this.indicatorRecordMapper.insert(newRecord);

            // insert thresholds
            modified.thresholds
                    .stream()
                    .filter(threshold -> threshold.value != null && threshold.color != null)
                    .map(threshold -> new ThresholdRecord(
                            null,
                            newRecord.getId(),
                            new BigDecimal(threshold.value),
                            threshold.color,
                            threshold.icon))
                    .forEach(this.thresholdRecordMapper::insert);

            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // first delete all thresholds of indicators
            this.thresholdRecordMapper.deleteByExample()
                    .where(ThresholdRecordDynamicSqlSupport.indicatorId, isIn(ids))
                    .build()
                    .execute();

            // then delete all indicators
            this.indicatorRecordMapper.deleteByExample()
                    .where(IndicatorRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.INDICATOR))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Indicator>> allForExam(final Long examId) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .where(IndicatorRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only for deletion
        if (bulkAction.type == BulkActionType.ACTIVATE || bulkAction.type == BulkActionType.DEACTIVATE) {
            return Collections.emptySet();
        }
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.INDICATOR)) {
            return Collections.emptySet();
        }

        // define the select function in case of source type
        Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction;
        switch (bulkAction.sourceType) {
            case INSTITUTION:
                selectionFunction = this::allIdsOfInstitution;
                break;
            case LMS_SETUP:
                selectionFunction = this::allIdsOfLmsSetup;
                break;
            case USER:
                selectionFunction = this::allIdsOfUser;
                break;
            case EXAM:
                selectionFunction = this::allIdsOfExam;
                break;
            default:
                selectionFunction = key -> Result.of(Collections.emptyList()); //empty select function
                break;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(IndicatorRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.parseLong(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.INDICATOR),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(IndicatorRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.lmsSetupId,
                        isEqualTo(Long.parseLong(lmsSetupKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        lmsSetupKey,
                        new EntityKey(rec.getId(), EntityType.INDICATOR),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(IndicatorRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.owner,
                        isEqualTo(userKey.modelId))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        userKey,
                        new EntityKey(rec.getId(), EntityType.INDICATOR),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfExam(final EntityKey examKey) {
        return Result.tryCatch(() -> this.indicatorRecordMapper.selectByExample()
                .where(
                        IndicatorRecordDynamicSqlSupport.examId,
                        isEqualTo(Long.parseLong(examKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        examKey,
                        new EntityKey(rec.getId(), EntityType.INDICATOR),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<IndicatorRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {

            final IndicatorRecord record = this.indicatorRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        entityType(),
                        String.valueOf(id));
            }

            return record;
        });
    }

    private Result<Indicator> toDomainModel(final IndicatorRecord record) {
        return Result.tryCatch(() -> {

            final List<Threshold> thresholds = this.thresholdRecordMapper.selectByExample()
                    .where(ThresholdRecordDynamicSqlSupport.indicatorId, isEqualTo(record.getId()))
                    .build()
                    .execute()
                    .stream()
                    .map(tRec -> new Threshold(
                            tRec.getValue().doubleValue(),
                            tRec.getColor(),
                            tRec.getIcon()))
                    .collect(Collectors.toList());

            return new Indicator(
                    record.getId(),
                    record.getExamId(),
                    record.getName(),
                    IndicatorType.valueOf(record.getType()),
                    record.getColor(),
                    record.getIcon(),
                    record.getTags(),
                    thresholds);
        });

    }

}
