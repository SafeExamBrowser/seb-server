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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ThresholdRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ThresholdRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.IndicatorRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ThresholdRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
public class IndicatorDAOImpl implements IndicatorDAO {

    private final IndicatorRecordMapper indicatorRecordMapper;
    private final ThresholdRecordMapper thresholdRecordMapper;
    private final ExamRecordMapper examRecordMapper;

    public IndicatorDAOImpl(
            final IndicatorRecordMapper indicatorRecordMapper,
            final ThresholdRecordMapper thresholdRecordMapper,
            final ExamRecordMapper examRecordMapper) {

        this.indicatorRecordMapper = indicatorRecordMapper;
        this.thresholdRecordMapper = thresholdRecordMapper;
        this.examRecordMapper = examRecordMapper;
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
        return Result.tryCatch(() -> {
            return this.indicatorRecordMapper.selectByExample()
                    .join(ExamRecordDynamicSqlSupport.examRecord)
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
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Indicator>> loadEntities(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            final List<Long> ids = extractPKsFromKeys(keys);

            return this.indicatorRecordMapper.selectByExample()
                    .where(IndicatorRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Indicator> save(final String modelId, final Indicator modified) {
        return Result.tryCatch(() -> {

            final Long pk = Long.parseLong(modelId);
            final IndicatorRecord newRecord = new IndicatorRecord(
                    pk,
                    null,
                    modified.type.name(),
                    modified.name,
                    modified.defaultColor);

            this.indicatorRecordMapper.updateByPrimaryKeySelective(newRecord);

            // update also the thresholds
            this.thresholdRecordMapper.deleteByExample()
                    .where(ThresholdRecordDynamicSqlSupport.indicatorId, isEqualTo(pk))
                    .build()
                    .execute();

            modified.thresholds
                    .stream()
                    .map(threshold -> new ThresholdRecord(
                            null,
                            pk,
                            new BigDecimal(threshold.value),
                            threshold.color))
                    .forEach(this.thresholdRecordMapper::insert);

            return this.indicatorRecordMapper.selectByPrimaryKey(pk);
        })
                .flatMap(this::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
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
                    modified.defaultColor);

            this.indicatorRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractPKsFromKeys(all);

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
        return Result.tryCatch(() -> {
            return this.indicatorRecordMapper.selectByExample()
                    .where(IndicatorRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logUnexpectedErrorAndSkip)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        final Set<EntityKey> examEntities = (bulkAction.sourceType == EntityType.EXAM)
                ? bulkAction.sources
                : bulkAction.extractKeys(EntityType.EXAM);

        return examEntities
                .stream()
                .flatMap(this::getDependencies)
                .collect(Collectors.toSet());
    }

    private Stream<EntityKey> getDependencies(final EntityKey examKey) {
        return this.indicatorRecordMapper.selectIdsByExample()
                .where(IndicatorRecordDynamicSqlSupport.examId, isEqualTo(Long.valueOf(examKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(pk -> new EntityKey(String.valueOf(pk), EntityType.INDICATOR));
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

            final ExamRecord examRecord = this.examRecordMapper.selectByPrimaryKey(record.getExamId());

            final List<Threshold> thresholds = this.thresholdRecordMapper.selectByExample()
                    .where(ThresholdRecordDynamicSqlSupport.indicatorId, isEqualTo(record.getId()))
                    .build()
                    .execute()
                    .stream()
                    .map(tRec -> new Threshold(
                            tRec.getId(),
                            tRec.getIndicatorId(),
                            tRec.getValue().doubleValue(),
                            tRec.getColor()))
                    .collect(Collectors.toList());

            return new Indicator(
                    record.getId(),
                    examRecord.getInstitutionId(),
                    examRecord.getOwner(),
                    record.getExamId(),
                    record.getName(),
                    IndicatorType.valueOf(record.getType()),
                    record.getColor(),
                    thresholds);
        });

    }

}
