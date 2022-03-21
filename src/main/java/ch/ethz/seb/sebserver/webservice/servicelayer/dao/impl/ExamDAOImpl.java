/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.AdditionalAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.AdditionalAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseAccess;

@Lazy
@Component
@WebServiceProfile
public class ExamDAOImpl implements ExamDAO {

    public static final String FAILED_TO_LOAD_QUIZ_DATA_MARK = "[FAILED TO LOAD DATA FROM LMS]";

    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordDAO examRecordDAO;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AdditionalAttributeRecordMapper additionalAttributeRecordMapper;
    private final LmsAPIService lmsAPIService;

    public ExamDAOImpl(
            final ExamRecordMapper examRecordMapper,
            final ExamRecordDAO examRecordDAO,
            final ApplicationEventPublisher applicationEventPublisher,
            final AdditionalAttributeRecordMapper additionalAttributeRecordMapper,
            final LmsAPIService lmsAPIService) {

        this.examRecordMapper = examRecordMapper;
        this.examRecordDAO = examRecordDAO;
        this.applicationEventPublisher = applicationEventPublisher;
        this.additionalAttributeRecordMapper = additionalAttributeRecordMapper;
        this.lmsAPIService = lmsAPIService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM;
    }

    @Override
    public Result<Exam> byPK(final Long id) {
        return this.examRecordDAO
                .recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Exam> loadWithAdditionalAttributes(final Long examId) {
        return this.examRecordDAO
                .recordById(examId)
                .flatMap(record -> {
                    final QuizData quizData = this.lmsAPIService
                            .getLmsAPITemplate(record.getLmsSetupId())
                            .flatMap(template -> template.getQuiz(record.getExternalId()))
                            .getOrThrow();
                    return toDomainModel(record, quizData, null, true);
                });
    }

    @Override
    public Result<GrantEntity> examGrantEntityByPK(final Long id) {
        return this.examRecordDAO.recordById(id)
                .map(record -> toDomainModel(record, null, null).getOrThrow());
    }

    @Override
    public Result<GrantEntity> examGrantEntityByClientConnection(final Long connectionId) {
        return this.examRecordDAO
                .recordByClientConnection(connectionId)
                .map(record -> toDomainModel(record, null, null).getOrThrow());
    }

    @Override
    public Result<Collection<Exam>> all(final Long institutionId, final Boolean active) {
        return this.examRecordDAO
                .all(institutionId, active)
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Collection<Long>> allInstitutionIdsByQuizId(final String quizId) {
        return this.examRecordDAO.allInstitutionIdsByQuizId(quizId);
    }

    @Override
    public Result<Collection<Exam>> allMatching(final FilterMap filterMap, final Predicate<Exam> predicate) {

        return Result.tryCatch(() -> {

            final String name = filterMap.getQuizName();
            final DateTime from = filterMap.getExamFromTime();
            final Predicate<Exam> quizDataFilter = exam -> {
                if (StringUtils.isNotBlank(name)) {
                    if (!exam.name.contains(name)) {
                        return false;
                    }
                }

                if (from != null && exam.startTime != null) {
                    // always show exams that has not ended yet
                    if (exam.endTime == null || exam.endTime.isAfter(from)) {
                        return true;
                    }
                    if (exam.startTime.isBefore(from)) {
                        return false;
                    }
                }

                return true;
            };

            return this.examRecordDAO
                    .allMatching(filterMap)
                    .flatMap(this::toDomainModel)
                    .getOrThrow()
                    .stream()
                    .filter(quizDataFilter.and(predicate))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Result<Exam> updateState(final Long examId, final ExamStatus status, final String updateId) {
        return this.examRecordDAO
                .updateState(examId, status, updateId)
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Exam> save(final Exam exam) {
        return this.examRecordDAO
                .save(exam)
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Exam> setSEBRestriction(final Long examId, final boolean sebRestriction) {
        return this.examRecordDAO
                .setSEBRestriction(examId, sebRestriction)
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Exam> createNew(final Exam exam) {
        return this.examRecordDAO
                .createNew(exam)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> setActive(final Set<EntityKey> all, final boolean active) {
        final Result<Collection<EntityKey>> tryCatch = Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            final ExamRecord examRecord = new ExamRecord(null, null, null, null, null,
                    null, null, null, null, null, null, null, null, BooleanUtils.toInteger(active), null,
                    Utils.getMillisecondsNow());

            this.examRecordMapper.updateByExampleSelective(examRecord)
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM))
                    .collect(Collectors.toList());

        });
        return tryCatch.onError(TransactionHandler::rollback);
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
    public Result<Collection<Exam>> allForRunCheck() {
        return this.examRecordDAO
                .allForRunCheck()
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Collection<Exam>> allForEndCheck() {
        return this.examRecordDAO
                .allForEndCheck()
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Long> placeLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.examRecordDAO
                    .recordById(examId)
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
                    null, null, null);

            this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
            return examId;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Long> releaseLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.examRecordDAO
                    .recordById(examId)
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
                    null, null, null);

            this.examRecordMapper.updateByPrimaryKeySelective(newRecord);
            return examId;
        })
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
                    null, null, null, null);

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
            final Collection<Long> result = this.examRecordMapper
                    .selectIdsByExample()
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
    public Result<Boolean> isLocked(final Long examId) {
        return this.examRecordDAO
                .recordById(examId)
                .map(rec -> BooleanUtils.toBooleanObject(rec.getUpdating()));
    }

    @Override
    @Transactional
    public void releaseAgedLocks() {
        try {

            final List<ExamRecord> lockedRecords = this.examRecordMapper
                    .selectByExample()
                    .where(ExamRecordDynamicSqlSupport.updating, isNotEqualTo(0))
                    .build()
                    .execute();

            if (lockedRecords != null && !lockedRecords.isEmpty()) {
                final long millisecondsNow = Utils.getMillisecondsNow();
                lockedRecords.stream().forEach(record -> {
                    try {
                        final String lastUpdateString = record.getLastupdate();
                        if (StringUtils.isNotBlank(lastUpdateString)) {
                            final String[] split = StringUtils.split(lastUpdateString, Constants.UNDERLINE);
                            final long timestamp = Long.parseLong(split[2]);
                            if (millisecondsNow - timestamp > Constants.MINUTE_IN_MILLIS) {
                                forceUnlock(record.getId()).getOrThrow();
                            }
                        }
                    } catch (final Exception e) {
                        log.warn("Failed to release aged write lock for exam: {} cause:", record, e.getMessage());
                    }
                });
            }
        } catch (final Exception e) {
            log.error("Failed to release aged write locks: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> upToDate(final Exam exam) {
        return Result.tryCatch(() -> {
            if (exam.lastModified == null) {
                return this.examRecordMapper.countByExample()
                        .where(ExamRecordDynamicSqlSupport.id, isEqualTo(exam.id))
                        .and(ExamRecordDynamicSqlSupport.lastModified, isNull())
                        .build()
                        .execute() > 0;
            } else {
                return this.examRecordMapper.countByExample()
                        .where(ExamRecordDynamicSqlSupport.id, isEqualTo(exam.id))
                        .and(ExamRecordDynamicSqlSupport.lastModified, isEqualTo(exam.lastModified))
                        .build()
                        .execute() > 0;
            }
        });
    }

    @Override
    public void setModified(final Long examId) {
        try {
            UpdateDSL.updateWithMapper(this.examRecordMapper::update, examRecord)
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .where(id, isEqualTo(examId))
                    .build()
                    .execute();
        } catch (final Exception e) {
            log.error("Failed to set modified now: ", e);
        }
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // notify exam deletion listener about following deletion, to cleanup stuff before deletion
            this.applicationEventPublisher.publishEvent(new ExamDeletionEvent(ids));

            this.examRecordMapper.deleteByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            // delete all additional attributes
            this.additionalAttributeRecordMapper
                    .deleteByExample()
                    .where(AdditionalAttributeRecordDynamicSqlSupport.entityType, isEqualTo(EntityType.EXAM.name()))
                    .and(AdditionalAttributeRecordDynamicSqlSupport.entityId, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.EXAM)) {
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
            default:
                selectionFunction = key -> Result.of(Collections.emptyList()); //empty select function
                break;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    public Result<Collection<Exam>> allOf(final Set<Long> pks) {
        return this.examRecordDAO
                .allOf(pks)
                .flatMap(this::toDomainModel);
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

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteTemplateReferences(final Long examTemplateId) {
        return Result.tryCatch(() -> {

            final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.examTemplateId,
                            isEqualTo(examTemplateId))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            final ArrayList<EntityKey> result = new ArrayList<>();
            for (final ExamRecord rec : records) {

                try {
                    this.examRecordMapper.updateByPrimaryKey(new ExamRecord(
                            rec.getId(),
                            rec.getInstitutionId(),
                            rec.getLmsSetupId(),
                            rec.getExternalId(),
                            rec.getOwner(),
                            rec.getSupporter(),
                            rec.getType(),
                            rec.getQuitPassword(),
                            rec.getBrowserKeys(),
                            rec.getStatus(),
                            rec.getLmsSebRestriction(),
                            rec.getUpdating(),
                            rec.getLastupdate(),
                            rec.getActive(),
                            null,
                            Utils.getMillisecondsNow()));

                    result.add(new EntityKey(rec.getId(), EntityType.EXAM));
                } catch (final Exception e) {
                    log.error("Failed to delete template references for exam: {}", rec, e);
                }
            }

            return result;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> allIdsOfRunning(final Long institutionId) {
        return Result.tryCatch(() -> this.examRecordMapper.selectIdsByExample()
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualTo(institutionId))
                .and(
                        ExamRecordDynamicSqlSupport.active,
                        isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
                .and(
                        ExamRecordDynamicSqlSupport.status,
                        isEqualTo(ExamStatus.RUNNING.name()))
                .build()
                .execute());
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examRecordMapper.selectByExample()
                        .where(ExamRecordDynamicSqlSupport.institutionId,
                                isEqualTo(Long.valueOf(institutionKey.modelId)))
                        .build()
                        .execute(),
                institutionKey));
    }

    private Result<Collection<EntityDependency>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examRecordMapper.selectByExample()
                        .where(ExamRecordDynamicSqlSupport.lmsSetupId,
                                isEqualTo(Long.valueOf(lmsSetupKey.modelId)))
                        .build()
                        .execute(),
                lmsSetupKey));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examRecordMapper.selectByExample()
                        .where(ExamRecordDynamicSqlSupport.owner,
                                isEqualTo(userKey.modelId))
                        .build()
                        .execute(),
                userKey));
    }

    private Collection<EntityDependency> toDependencies(
            final List<ExamRecord> records,
            final EntityKey parent) {

        return this.toDomainModel(records)
                .map(models -> models
                        .stream()
                        .map(model -> getDependency(model, parent))
                        .collect(Collectors.toList()))
                .getOrThrow();
    }

    private EntityDependency getDependency(final Exam exam, final EntityKey parent) {
        return new EntityDependency(
                parent,
                new EntityKey(exam.getId(), EntityType.EXAM),
                exam.getName(),
                exam.getDescription());
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
                    .reduce(new LinkedHashMap<>(),
                            (map, record) -> Utils.mapCollect(map, record.getLmsSetupId(), record),
                            Utils::mapPutAll);

            return lmsSetupToRecordMapping
                    .entrySet()
                    .stream()
                    .flatMap(entry -> toDomainModel(
                            entry.getKey(),
                            entry.getValue())
                                    .onError(error -> log.error(
                                            "Failed to get quizzes from LMS Setup: {}",
                                            entry.getKey(), error))
                                    .getOr(Collections.emptyList())
                                    .stream())
                    .collect(Collectors.toList());
        });
    }

    private Result<Collection<Exam>> toDomainModel(
            final Long lmsSetupId,
            final Collection<ExamRecord> records) {

        return Result.tryCatch(() -> {

            // map records
            final Map<String, ExamRecord> recordMapping = records
                    .stream()
                    .collect(Collectors.toMap(ExamRecord::getExternalId, Function.identity()));

            // get and map quizzes
            final Map<String, QuizData> quizzes = this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .flatMap(template -> template.getQuizzes(recordMapping.keySet()))
                    .getOrElse(() -> Collections.emptyList())
                    .stream()
                    .collect(Collectors.toMap(q -> q.id, Function.identity()));

            if (records.size() != quizzes.size()) {

                // Check if we have LMS connection to verify the source of the exam quiz mismatch
                final LmsAPITemplate lmsSetup = this.lmsAPIService
                        .getLmsAPITemplate(lmsSetupId)
                        .getOrThrow();

                if (log.isDebugEnabled()) {
                    log.debug("Quizzes size mismatch detected by getting exams quiz data from LMS: {}", lmsSetup);
                }

                if (lmsSetup.testCourseAccessAPI().hasAnyError()) {
                    // No course access on the LMS. This means we can't get any quizzes from this LMSSetup at the moment
                    // All exams are marked as corrupt because of LMS Setup failure

                    log.warn("Failed to get quizzes form LMS Setup. No access to LMS {}", lmsSetup.lmsSetup());

                    return recordMapping.entrySet()
                            .stream()
                            .map(entry -> toDomainModel(
                                    entry.getValue(),
                                    null,
                                    ExamStatus.CORRUPT_NO_LMS_CONNECTION)
                                            .getOr(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }

            // collect Exam's
            return recordMapping.entrySet()
                    .stream()
                    .map(entry -> toDomainModel(
                            entry.getValue(),
                            getQuizData(quizzes, entry.getKey(), entry.getValue()),
                            ExamStatus.CORRUPT_INVALID_ID)
                                    .onError(error -> log.error(
                                            "Failed to get quiz data from remote LMS for exam: ",
                                            error))
                                    .getOr(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        });
    }

    private QuizData getQuizData(
            final Map<String, QuizData> quizzes,
            final String externalId,
            final ExamRecord record) {

        if (quizzes.containsKey(externalId)) {
            return quizzes.get(externalId);
        } else {
            // If this is a Moodle quiz, try to recover from eventually restore of the quiz on the LMS side
            // NOTE: This is a workaround for Moodle quizzes that had have a recovery within the sandbox tool
            //       Where potentially quiz identifiers get changed during such a recovery and the SEB Server
            //       internal mapping is not working properly anymore. In this case we try to recover from such
            //       a case by using the short name of the quiz and search for the quiz within the course with this
            //       short name. If one quiz has been found that matches all criteria, we adapt the internal id
            //       mapping to this quiz.
            //       If recovering fails, this returns null and the calling side must handle the lack of quiz data
            try {
                final LmsSetup lmsSetup = this.lmsAPIService
                        .getLmsSetup(record.getLmsSetupId())
                        .getOrThrow();
                if (lmsSetup.lmsType == LmsType.MOODLE) {

                    log.info("Try to recover quiz data for Moodle quiz with internal identifier: {}", externalId);

                    // get additional quiz name attribute
                    final AdditionalAttributeRecord additionalAttribute =
                            this.additionalAttributeRecordMapper.selectByExample()
                                    .where(
                                            AdditionalAttributeRecordDynamicSqlSupport.entityType,
                                            SqlBuilder.isEqualTo(EntityType.EXAM.name()))
                                    .and(
                                            AdditionalAttributeRecordDynamicSqlSupport.entityId,
                                            SqlBuilder.isEqualTo(record.getId()))
                                    .and(
                                            AdditionalAttributeRecordDynamicSqlSupport.name,
                                            SqlBuilder.isEqualTo(QuizData.QUIZ_ATTR_NAME))
                                    .build()
                                    .execute()
                                    .stream()
                                    .findAny()
                                    .orElse(null);
                    if (additionalAttribute != null) {

                        log.debug("Found additional quiz name attribute: {}", additionalAttribute);

                        // get the course name identifier
                        final String shortname = MoodleCourseAccess.getShortname(externalId);
                        if (StringUtils.isNotBlank(shortname)) {

                            log.debug("Using short-name: {} for recovering", shortname);

                            final QuizData recoveredQuizData = this.lmsAPIService
                                    .getLmsAPITemplate(lmsSetup.id)
                                    .map(template -> template.getQuizzes(new FilterMap())
                                            .getOrThrow()
                                            .stream()
                                            .filter(quiz -> {
                                                final String qShortName = MoodleCourseAccess.getShortname(quiz.id);
                                                return qShortName != null && qShortName.equals(shortname);
                                            })
                                            .filter(quiz -> additionalAttribute.getValue().equals(quiz.name))
                                            .findAny()
                                            .get())
                                    .getOrThrow();
                            if (recoveredQuizData != null) {

                                log.debug("Found quiz data for recovering: {}", recoveredQuizData);

                                // save exam with new external id
                                this.examRecordMapper.updateByPrimaryKeySelective(new ExamRecord(
                                        record.getId(),
                                        null, null,
                                        recoveredQuizData.id,
                                        null, null, null, null, null, null, null, null, null, null, null,
                                        Utils.getMillisecondsNow()));

                                log.debug("Successfully recovered exam quiz data to new externalId {}",
                                        recoveredQuizData.id);
                            }
                            return recoveredQuizData;
                        }
                    }
                }
            } catch (final Exception e) {
                log.warn("Failed to try to recover from Moodle quiz restore: {}", e.getMessage());
            }
            return null;
        }
    }

    private Result<Exam> toDomainModel(
            final ExamRecord record,
            final QuizData quizData,
            final ExamStatus statusOverride) {

        return this.toDomainModel(record, quizData, statusOverride, false);
    }

    private Result<Exam> toDomainModel(
            final ExamRecord record,
            final QuizData quizData,
            final ExamStatus statusOverride,
            final boolean withAdditionalAttributed) {

        return Result.tryCatch(() -> {

            final Collection<String> supporter = (StringUtils.isNotBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            ExamStatus status;
            try {
                status = ExamStatus.valueOf(record.getStatus());
            } catch (final Exception e) {
                log.error("Missing exam status from data base. Set ExamStatus.UP_COMING as fallback ", e);
                status = ExamStatus.UP_COMING;
            }

            Map<String, String> additionalAttributes = null;
            if (withAdditionalAttributed) {
                additionalAttributes = this.additionalAttributeRecordMapper.selectByExample()
                        .where(
                                AdditionalAttributeRecordDynamicSqlSupport.entityType,
                                SqlBuilder.isEqualTo(EntityType.EXAM.name()))
                        .and(
                                AdditionalAttributeRecordDynamicSqlSupport.entityId,
                                SqlBuilder.isEqualTo(record.getId()))
                        .build()
                        .execute()
                        .stream()
                        .collect(Collectors.toMap(r -> r.getName(), r -> r.getValue()));
            }

            return new Exam(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getLmsSetupId(),
                    record.getExternalId(),
                    (quizData != null) ? quizData.name : FAILED_TO_LOAD_QUIZ_DATA_MARK,
                    (quizData != null) ? quizData.description : FAILED_TO_LOAD_QUIZ_DATA_MARK,
                    (quizData != null) ? quizData.startTime : new DateTime(0),
                    (quizData != null) ? quizData.endTime : null,
                    (quizData != null) ? quizData.startURL : Constants.EMPTY_NOTE,
                    ExamType.valueOf(record.getType()),
                    record.getOwner(),
                    supporter,
                    (quizData != null) ? status : (statusOverride != null) ? statusOverride : status,
                    BooleanUtils.toBooleanObject(record.getLmsSebRestriction()),
                    record.getBrowserKeys(),
                    BooleanUtils.toBooleanObject((quizData != null) ? record.getActive() : null),
                    record.getLastupdate(),
                    record.getExamTemplateId(),
                    record.getLastModified(),
                    additionalAttributes);
        });
    }

}
