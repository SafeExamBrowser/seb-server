/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@Lazy
@Component
@WebServiceProfile
public class ExamDAOImpl implements ExamDAO {

    private final ExamRecordMapper examRecordMapper;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final LmsAPIService lmsAPIService;

    public ExamDAOImpl(
            final ExamRecordMapper examRecordMapper,
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final LmsAPIService lmsAPIService) {

        this.examRecordMapper = examRecordMapper;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
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
    public Result<Exam> byClientConnection(final Long connectionId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByPrimaryKey(connectionId))
                .flatMap(ccRecord -> recordById(ccRecord.getExamId()))
                .flatMap(this::toDomainModelCached)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> all(final Long institutionId, final Boolean active) {
        return Result.tryCatch(() -> (active != null)
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
                        .execute())
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Collection<Long>> allByQuizId(final String quizId) {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.externalId,
                            isEqualToWhenPresent(quizId))
                    .and(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
                    .build()
                    .execute()
                    .stream()
                    .map(rec -> rec.getId())
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allMatching(final FilterMap filterMap, final Predicate<Exam> predicate) {

        return Result.tryCatch(() -> {

            final boolean cached = filterMap.getBoolean(Exam.FILTER_CACHED_QUIZZES);
            final String name = filterMap.getQuizName();
            final DateTime from = filterMap.getExamFromTime();
            final Predicate<Exam> quizDataFilter = exam -> {
                if (StringUtils.isNotBlank(name)) {
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
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isEqualToWhenPresent(filterMap.getExamStatus()))
                    .build()
                    .execute();

            return this.toDomainModel(records, cached)
                    .getOrThrow()
                    .stream()
                    .filter(quizDataFilter.and(predicate))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Result<Exam> updateState(final Long examId, final ExamStatus status, final String updateId) {
        return recordById(examId)
                .map(examRecord -> {
                    if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(examRecord.getUpdating()))) {
                        if (!updateId.equals(examRecord.getLastupdate())) {
                            throw new IllegalStateException("Exam is currently locked: " + examRecord.getExternalId());
                        }
                    }

                    final ExamRecord newExamRecord = new ExamRecord(
                            examRecord.getId(),
                            null, null, null, null, null, null, null, null,
                            status.name(),
                            null, null, null, null);

                    this.examRecordMapper.updateByPrimaryKeySelective(newExamRecord);
                    return this.examRecordMapper.selectByPrimaryKey(examId);
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Exam> save(final Exam exam) {
        return Result.tryCatch(() -> {

            // check internal persistent write-lock
            final ExamRecord oldRecord = this.examRecordMapper.selectByPrimaryKey(exam.id);
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(oldRecord.getUpdating()))) {
                throw new IllegalStateException("Exam is currently locked: " + exam.externalId);
            }

            final ExamRecord examRecord = new ExamRecord(
                    exam.id,
                    null, null, null, null,
                    (exam.supporter != null)
                            ? StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    (exam.type != null)
                            ? exam.type.name()
                            : null,
                    null,
                    exam.browserExamKeys,
                    (exam.status != null)
                            ? exam.status.name()
                            : null,
                    1, // seb restriction (deprecated)
                    null, // updating
                    null, // lastUpdate
                    null // active
            );

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(exam.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Exam> setSEBRestriction(final Long examId, final boolean sebRestriction) {
        return Result.tryCatch(() -> {

            final ExamRecord examRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toInteger(sebRestriction),
                    null, null, null);

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
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
                // if the same institution tries to import an exam that already exists
                // open the existing. otherwise create new one if requested
                if (exam.institutionId.equals(examRecord.getInstitutionId())) {
                    final ExamRecord newRecord = new ExamRecord(
                            examRecord.getId(),
                            null, null, null, null, null,
                            (exam.type != null) ? exam.type.name() : ExamType.UNDEFINED.name(),
                            null, // quitPassword
                            null, // browser keys
                            null, // status
                            null, // lmsSebRestriction (deprecated)
                            null, // updating
                            null, // lastUpdate
                            BooleanUtils.toIntegerObject(exam.active));

                    this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.examRecordMapper.selectByPrimaryKey(examRecord.getId());
                }
            }

            final ExamRecord examRecord = new ExamRecord(
                    null,
                    exam.institutionId,
                    exam.lmsSetupId,
                    exam.externalId,
                    exam.owner,
                    (exam.supporter != null)
                            ? StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    (exam.type != null) ? exam.type.name() : ExamType.UNDEFINED.name(),
                    null, // quitPassword
                    null, // browser keys
                    (exam.status != null) ? exam.status.name() : ExamStatus.UP_COMING.name(),
                    1, // seb restriction (deprecated)
                    BooleanUtils.toInteger(false),
                    null, // lastUpdate
                    BooleanUtils.toInteger(true));

            this.examRecordMapper.insert(examRecord);
            return examRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            final ExamRecord examRecord = new ExamRecord(null, null, null, null, null,
                    null, null, null, null, null, null, null, null, BooleanUtils.toInteger(active));

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
                .execute() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getExamIdsForStatus(final Long institutionId, final ExamStatus status) {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(
                        ExamRecordDynamicSqlSupport.active,
                        isEqualTo(BooleanUtils.toInteger(true)))
                .and(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(institutionId))
                .and(
                        ExamRecordDynamicSqlSupport.status,
                        isEqualTo(status.name()))
                .and(
                        ExamRecordDynamicSqlSupport.updating,
                        isEqualTo(BooleanUtils.toInteger(false)))
                .build()
                .execute());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> allRunningExamIds() {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(
                        ExamRecordDynamicSqlSupport.active,
                        isEqualTo(BooleanUtils.toInteger(true)))
                .and(
                        ExamRecordDynamicSqlSupport.status,
                        isEqualTo(ExamStatus.RUNNING.name()))
                .and(
                        ExamRecordDynamicSqlSupport.updating,
                        isEqualTo(BooleanUtils.toInteger(false)))

                .build()
                .execute());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allForRunCheck() {
        return Result.tryCatch(() -> {
            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isEqualTo(ExamStatus.UP_COMING.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.updating,
                            isEqualTo(BooleanUtils.toInteger(false)))

                    .build()
                    .execute();

            return new ArrayList<>(this.toDomainModel(records)
                    .getOrThrow());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allForEndCheck() {
        return Result.tryCatch(() -> {
            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isEqualTo(ExamStatus.RUNNING.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.updating,
                            isEqualTo(BooleanUtils.toInteger(false)))

                    .build()
                    .execute();

            return new ArrayList<>(this.toDomainModel(records)
                    .getOrThrow());
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Exam> placeLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.recordById(examId)
                    .getOrThrow();

            // consistency check
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(examRec.getUpdating()))) {
                throw new IllegalStateException(
                        "Exam to end update is not in expected state: " + examRec.getExternalId());
            }

            final ExamRecord newRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toInteger(true),
                    updateId,
                    null);

            this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
            return newRecord;
        })
                .flatMap(rec -> this.recordById(rec.getId()))
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Exam> releaseLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.recordById(examId)
                    .getOrThrow();

            // consistency check
            if (BooleanUtils.isFalse(BooleanUtils.toBooleanObject(examRec.getUpdating()))
                    || !updateId.equals(examRec.getLastupdate())) {

                throw new IllegalStateException(
                        "Exam to end update is not in expected state: " + examRec.getExternalId());
            }

            final ExamRecord newRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toInteger(false),
                    updateId,
                    null);

            this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
            return newRecord;
        })
                .flatMap(rec -> this.recordById(rec.getId()))
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Long> forceUnlock(final Long examId) {

        if (log.isDebugEnabled()) {
            log.debug("forceUnlock for exam: {}", examId);
        }

        return Result.tryCatch(() -> {
            final ExamRecord examRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toInteger(false),
                    null, null);

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return examRecord.getId();
        })
                .onError(TransactionHandler::rollback);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Collection<Long>> forceUnlockAll(final String updateId) {

        if (log.isDebugEnabled()) {
            log.debug("forceUnlock for updateId: {}", updateId);
        }

        return Result.tryCatch(() -> {
            final Collection<Long> result = this.examRecordMapper.selectIdsByExample()
                    .where(ExamRecordDynamicSqlSupport.lastupdate, isEqualTo(updateId))
                    .build()
                    .execute()
                    .stream()
                    .map(this::forceUnlock)
                    .flatMap(Result::skipOnError)
                    .collect(Collectors.toList());

            return result;
        })
                .onError(TransactionHandler::rollback);

    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> isLocked(final Long examId) {
        return this.recordById(examId)
                .map(rec -> BooleanUtils.toBooleanObject(rec.getUpdating()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> upToDate(final Long examId, final String updateId) {
        return this.recordById(examId)
                .map(rec -> {
                    if (updateId == null) {
                        return rec.getLastupdate() == null;
                    } else {
                        return updateId.equals(rec.getLastupdate());
                    }
                });
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

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
        Function<EntityKey, Result<Collection<EntityKey>>> selectionFunction;
        switch (bulkAction.sourceType) {
            case INSTITUTION:
                selectionFunction = this::allIdsOfInstitution;
                break;
            case LMS_SETUP:
                selectionFunction = this::allIdsOfLmsSetup;
                break;
            default:
                selectionFunction = key -> Result.of(Collections.emptyList()); //empty select function
                break;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Exam>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> this.examRecordMapper.selectByExample()
                .where(ExamRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                .build()
                .execute()).flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> allIdsOfInstitution(final Long institutionId) {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualTo(institutionId))
                .and(
                        ExamRecordDynamicSqlSupport.active,
                        isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
                .build()
                .execute());
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(ExamRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityKey>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(ExamRecordDynamicSqlSupport.lmsSetupId,
                        isEqualTo(Long.valueOf(lmsSetupKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM))
                .collect(Collectors.toList()));
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

    private Result<Exam> toDomainModelCached(final ExamRecord record) {
        return Result.tryCatch(() -> this.lmsAPIService
                .getLmsAPITemplate(record.getLmsSetupId())
                .getOrThrow())
                .flatMap(template -> template.getQuizFromCache(record.getExternalId()))
                .flatMap(quizData -> this.toDomainModel(record, quizData));
    }

    private Result<Exam> toDomainModel(final ExamRecord record) {
        return toDomainModel(
                record.getLmsSetupId(),
                Arrays.asList(record))
                        .map(col -> col.iterator().next());
    }

    private Result<Collection<Exam>> toDomainModel(final Collection<ExamRecord> records) {
        return toDomainModel(records, false);
    }

    private Result<Collection<Exam>> toDomainModel(
            final Collection<ExamRecord> records,
            final boolean cached) {

        return Result.tryCatch(() -> {

            final HashMap<Long, Collection<ExamRecord>> lmsSetupToRecordMapping = records
                    .stream()
                    .reduce(new HashMap<>(),
                            (map, record) -> Utils.mapCollect(map, record.getLmsSetupId(), record),
                            Utils::mapPutAll);

            return lmsSetupToRecordMapping
                    .entrySet()
                    .stream()
                    .flatMap(entry -> toDomainModel(entry.getKey(), entry.getValue(), cached)
                            .getOrThrow()
                            .stream())
                    .collect(Collectors.toList());
        });
    }

    private Result<Collection<Exam>> toDomainModel(
            final Long lmsSetupId,
            final Collection<ExamRecord> records) {

        return toDomainModel(lmsSetupId, records, false);
    }

    private Result<Collection<Exam>> toDomainModel(
            final Long lmsSetupId,
            final Collection<ExamRecord> records,
            final boolean cached) {

        return Result.tryCatch(() -> {

            // map records
            final Map<String, ExamRecord> recordMapping = records
                    .stream()
                    .collect(Collectors.toMap(ExamRecord::getExternalId, Function.identity()));

            // get and map quizzes
            final Map<String, QuizData> quizzes = this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .map(template -> (cached)
                            ? template.getQuizzesFromCache(recordMapping.keySet())
                            : template.getQuizzes(recordMapping.keySet()))
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

            final Collection<String> supporter = (StringUtils.isNotBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            ExamStatus status;
            try {
                status = ExamStatus.valueOf(record.getStatus());
            } catch (final Exception e) {
                log.error("Missing exam status form data base. Set ExamStatus.UP_COMING as fallback ", e);
                status = ExamStatus.UP_COMING;
            }

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
                    record.getOwner(),
                    supporter,
                    status,
                    record.getBrowserKeys(),
                    BooleanUtils.toBooleanObject((quizData != null) ? record.getActive() : null),
                    record.getLastupdate());
        });
    }

}
