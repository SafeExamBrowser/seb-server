/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@Lazy
@Component
public class ExamDAOImpl implements ExamDAO {

    private final ExamRecordMapper examRecordMapper;
    private final LmsAPIService lmsAPIService;

    public ExamDAOImpl(
            final ExamRecordMapper examRecordMapper,
            final LmsAPIService lmsAPIService) {

        this.examRecordMapper = examRecordMapper;
        this.lmsAPIService = lmsAPIService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Exam> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Exam> byQuizId(final String quizId) {
        return recordByQuizId(quizId)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> all(final Predicate<Exam> predicate, final Boolean active) {
        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>> example =
                    this.examRecordMapper.selectByExample();

            return (active != null)
                    ? example
                            .where(
                                    ExamRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : example
                            .build()
                            .execute();

        }).flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allMatching(
            final Long institutionId,
            final Long lmsSetupId,
            final String name,
            final ExamStatus status,
            final ExamType type,
            final DateTime from,
            final String owner,
            final Boolean active) {

        return Result.tryCatch(() -> {

            final Predicate<Exam> quizDataFilter = exam -> {
                if (StringUtils.isNoneBlank(name)) {
                    if (!exam.name.contains(name)) {
                        return false;
                    }
                }

                if (from != null) {
                    if (exam.startTime.isAfter(from)) {
                        return false;
                    }
                }

                return true;
            };

            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                    .and(
                            ExamRecordDynamicSqlSupport.institutionId,
                            isEqualToWhenPresent(institutionId))
                    .and(
                            ExamRecordDynamicSqlSupport.lmsSetupId,
                            isEqualToWhenPresent(lmsSetupId))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isEqualToWhenPresent((status != null) ? status.name() : null))
                    .and(
                            ExamRecordDynamicSqlSupport.type,
                            isEqualToWhenPresent((type != null) ? type.name() : null))
                    .and(
                            ExamRecordDynamicSqlSupport.owner,
                            isEqualToWhenPresent(owner))
                    .build()
                    .execute();

            return this.toDomainModel(records)
                    .getOrThrow()
                    .stream()
                    .filter(quizDataFilter)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Exam> importFromQuizData(final QuizData quizData) {
        // TODO Auto-generated method stub
        return Result.ofTODO();
    }

    @Override
    public Result<Exam> save(final Exam exam) {
        if (exam == null) {
            return Result.ofError(new NullPointerException("exam has null-reference"));
        }
        if (exam.id == null) {
            return Result.ofError(new IllegalArgumentException("exam.id has null-reference"));
        }

        return update(exam)
                .flatMap(this::toDomainModel);
    }

    private Result<ExamRecord> update(final Exam exam) {
        return Result.tryCatch(() -> {
            final ExamRecord examRecord = new ExamRecord(
                    exam.id,
                    null, null, null, null,
                    (exam.supporter != null)
                            ? StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    (exam.type != null) ? exam.type.name() : null,
                    (exam.status != null) ? exam.status.name() : null,
                    BooleanUtils.toIntegerObject(exam.active));

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(exam.id);
        });
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        final List<Long> ids = extractPKsFromKeys(all);
        final ExamRecord examRecord = new ExamRecord(null, null, null, null, null,
                null, null, null, BooleanUtils.toInteger(active));

        try {

            this.examRecordMapper.updateByExampleSelective(examRecord)
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.EXAM)))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Activation failed on unexpected exception for Exam of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public Collection<Result<EntityKey>> delete(final Set<EntityKey> all) {
        final List<Long> ids = extractPKsFromKeys(all);

        try {

            this.examRecordMapper.deleteByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> Result.of(new EntityKey(id, EntityType.EXAM)))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            return ids.stream()
                    .map(id -> Result.<EntityKey> ofError(new RuntimeException(
                            "Deletion failed on unexpected exception for Exam of id: " + id, e)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        // define the select function in case of source type
        final Function<EntityKey, Result<Collection<EntityKey>>> selectionFunction =
                (bulkAction.sourceType == EntityType.INSTITUTION)
                        ? this::allIdsOfInstitution
                        : (bulkAction.sourceType == EntityType.LMS_SETUP)
                                ? this::allIdsOfLmsSetup
                                : key -> Result.of(Collections.emptyList()); // else : empty select function

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> loadEntities(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            final List<Long> ids = extractPKsFromKeys(keys);
            return this.examRecordMapper.selectByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();
        }).flatMap(this::toDomainModel);
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectIdsByExample()
                    .where(ExamRecordDynamicSqlSupport.institutionId,
                            isEqualTo(Long.valueOf(institutionKey.modelId)))
                    .build()
                    .execute()
                    .stream()
                    .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
                    .collect(Collectors.toList());
        });
    }

    private Result<Collection<EntityKey>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectIdsByExample()
                    .where(ExamRecordDynamicSqlSupport.lmsSetupId,
                            isEqualTo(Long.valueOf(lmsSetupKey.modelId)))
                    .build()
                    .execute()
                    .stream()
                    .map(id -> new EntityKey(id, EntityType.LMS_SETUP))
                    .collect(Collectors.toList());
        });
    }

    private Result<ExamRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ExamRecord record = this.examRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.EXAM,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<ExamRecord> recordByQuizId(final String quizId) {
        return getSingleResource(
                quizId,
                this.examRecordMapper.selectByExample()
                        .where(ExamRecordDynamicSqlSupport.externalId, isEqualTo(quizId))
                        .build()
                        .execute());
    }

    private Result<Exam> toDomainModel(final ExamRecord record) {
        return toDomainModel(
                record.getLmsSetupId(),
                Arrays.asList(record))
                        .map(col -> col.iterator().next());
    }

    private Result<Collection<Exam>> toDomainModel(final Collection<ExamRecord> records) {
        return Result.tryCatch(() -> {

            final HashMap<Long, Collection<ExamRecord>> lmsSetupToRecordMapping = records
                    .stream()
                    .reduce(new HashMap<Long, Collection<ExamRecord>>(),
                            (map, record) -> Utils.mapCollect(map, record.getLmsSetupId(), record),
                            (map1, map2) -> Utils.mapPutAll(map1, map2));

            return lmsSetupToRecordMapping
                    .entrySet()
                    .stream()
                    .flatMap(entry -> toDomainModel(entry.getKey(), entry.getValue()).getOrThrow().stream())
                    .collect(Collectors.toList());
        });
    }

    private Result<Collection<Exam>> toDomainModel(final Long lmsSetupId, final Collection<ExamRecord> records) {
        return Result.tryCatch(() -> {
            final HashMap<String, ExamRecord> recordMapping = records
                    .stream()
                    .reduce(new HashMap<String, ExamRecord>(),
                            (map, record) -> Utils.mapPut(map, record.getExternalId(), record),
                            (map1, map2) -> Utils.mapPutAll(map1, map2));

            return this.lmsAPIService
                    .createLmsAPITemplate(lmsSetupId)
                    .map(template -> template.getQuizzes(recordMapping.keySet()))
                    .getOrThrow()
                    .stream()
                    .map(result -> result.flatMap(quiz -> toDomainModel(recordMapping, quiz)).getOrThrow())
                    .collect(Collectors.toList());
        });
    }

    private Result<Exam> toDomainModel(
            final HashMap<String, ExamRecord> recordMapping,
            final QuizData quizData) {

        return Result.tryCatch(() -> {

            final ExamRecord record = recordMapping.get(quizData.id);
            final Collection<String> supporter = (StringUtils.isNoneBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            return new Exam(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getLmsSetupId(),
                    quizData.id,
                    quizData.name,
                    quizData.description,
                    ExamStatus.valueOf(record.getStatus()),
                    quizData.startTime,
                    quizData.endTime,
                    quizData.startURL,
                    ExamType.valueOf(record.getType()),
                    record.getOwner(),
                    supporter,
                    BooleanUtils.toBooleanObject(record.getActive()));
        });
    }

}
