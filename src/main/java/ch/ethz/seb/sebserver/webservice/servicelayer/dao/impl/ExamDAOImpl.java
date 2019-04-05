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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
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
    public Result<Collection<Exam>> all(final Long institutionId, final Boolean active) {
        return Result.tryCatch(() -> {
            return (active != null)
                    ? this.examRecordMapper.selectByExample()
                            .where(
                                    ExamRecordDynamicSqlSupport.institutionId,
                                    isEqualToWhenPresent(institutionId))
                            .and(
                                    ExamRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(active)))
                            .build()
                            .execute()
                    : this.examRecordMapper.selectByExample()
                            .build()
                            .execute();

        }).flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allMatching(final FilterMap filterMap, final Predicate<Exam> predicate) {

        return Result.tryCatch(() -> {

            final String name = filterMap.getQuizName();
            final DateTime from = filterMap.getExamFromTime();
            final Predicate<Exam> quizDataFilter = exam -> {
                if (StringUtils.isNoneBlank(name)) {
                    if (!exam.name.contains(name)) {
                        return false;
                    }
                }

                if (from != null) {
                    if (exam.startTime.isBefore(from)) {
                        return false;
                    }
                }

                return true;
            };

            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(filterMap.getActiveAsInt()))
                    .and(
                            ExamRecordDynamicSqlSupport.institutionId,
                            isEqualToWhenPresent(filterMap.getInstitutionId()))
                    .and(
                            ExamRecordDynamicSqlSupport.lmsSetupId,
                            isEqualToWhenPresent(filterMap.getLmsSetupId()))
                    .and(
                            ExamRecordDynamicSqlSupport.type,
                            isEqualToWhenPresent(filterMap.getExamType()))
                    .build()
                    .execute();

            return this.toDomainModel(records)
                    .getOrThrow()
                    .stream()
                    .filter(quizDataFilter.and(predicate))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Exam> save(final Exam exam) {
        return Result.tryCatch(() -> {

            final ExamRecord examRecord = new ExamRecord(
                    exam.id,
                    null, null, null, null,
                    (exam.supporter != null)
                            ? StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    (exam.type != null) ? exam.type.name() : ExamType.UNDEFINED.name(),
                    exam.quitPassword,
                    BooleanUtils.toIntegerObject(exam.active));

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(exam.id);
        })
                .flatMap(this::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Exam> createNew(final Exam exam) {
        return Result.tryCatch(() -> {

            // fist check if it is not already existing
            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(ExamRecordDynamicSqlSupport.lmsSetupId, isEqualTo(exam.lmsSetupId))
                    .and(ExamRecordDynamicSqlSupport.externalId, isEqualTo(exam.externalId))
                    .build()
                    .execute();

            // if there is already an existing imported exam for the quiz, this is
            // used to save instead of create a new one
            if (records != null && records.size() > 0) {
                final ExamRecord examRecord = records.get(0);
                final ExamRecord newRecord = new ExamRecord(
                        examRecord.getId(),
                        null, null, null, null, null,
                        (exam.type != null) ? exam.type.name() : ExamType.UNDEFINED.name(),
                        null,
                        BooleanUtils.toIntegerObject(exam.active));

                this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
                return this.examRecordMapper.selectByPrimaryKey(examRecord.getId());
            }

            final ExamRecord examRecord = new ExamRecord(
                    null,
                    exam.institutionId,
                    exam.lmsSetupId,
                    exam.externalId,
                    exam.owner,
                    null,
                    (exam.type != null) ? exam.type.name() : ExamType.UNDEFINED.name(),
                    null,
                    BooleanUtils.toInteger(true));

            this.examRecordMapper.insert(examRecord);
            return examRecord;
        })
                .flatMap(this::toDomainModel)
                .onErrorDo(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractPKsFromKeys(all);
            final ExamRecord examRecord = new ExamRecord(null, null, null, null, null,
                    null, null, null, BooleanUtils.toInteger(active));

            this.examRecordMapper.updateByExampleSelective(examRecord)
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM))
                    .collect(Collectors.toList());

        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(final String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return false;
        }

        return this.examRecordMapper.countByExample()
                .where(ExamRecordDynamicSqlSupport.id, isEqualTo(Long.valueOf(modelId)))
                .and(ExamRecordDynamicSqlSupport.active, isEqualTo(BooleanUtils.toInteger(true)))
                .build()
                .execute()
                .longValue() > 0;
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractPKsFromKeys(all);

            this.examRecordMapper.deleteByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM))
                    .collect(Collectors.toList());

        });
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
    public Result<Collection<Exam>> byEntityKeys(final Set<EntityKey> keys) {
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

            // map records
            final Map<String, ExamRecord> recordMapping = records
                    .stream()
                    .collect(Collectors.toMap(r -> r.getExternalId(), Function.identity()));

            // get and map quizzes
            final Map<String, QuizData> quizzes = this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .map(template -> template.getQuizzes(recordMapping.keySet()))
                    .getOrThrow()
                    .stream()
                    .flatMap(Result::skipOnError)
                    .collect(Collectors.toMap(q -> q.id, Function.identity()));

            // collect Exam's
            return recordMapping.entrySet()
                    .stream()
                    .map(entry -> toDomainModel(entry.getValue(), quizzes.get(entry.getKey())).getOrThrow())
                    .collect(Collectors.toList());
        });
    }

    private Result<Exam> toDomainModel(
            final ExamRecord record,
            final QuizData quizData) {

        return Result.tryCatch(() -> {

            final Collection<String> supporter = (StringUtils.isNoneBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            return new Exam(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getLmsSetupId(),
                    record.getExternalId(),
                    (quizData != null) ? quizData.name : Constants.EMPTY_NOTE,
                    (quizData != null) ? quizData.description : Constants.EMPTY_NOTE,
                    (quizData != null) ? quizData.startTime : null,
                    (quizData != null) ? quizData.endTime : null,
                    (quizData != null) ? quizData.startURL : Constants.EMPTY_NOTE,
                    ExamType.valueOf(record.getType()),
                    record.getQuitPassword(),
                    record.getOwner(),
                    supporter,
                    BooleanUtils.toBooleanObject((quizData != null) ? record.getActive() : 0));
        });
    }

}
