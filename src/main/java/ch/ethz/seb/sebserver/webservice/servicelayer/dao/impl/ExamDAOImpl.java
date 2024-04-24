/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.*;
import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.examRecord;
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

import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;

@Lazy
@Component
@WebServiceProfile
public class ExamDAOImpl implements ExamDAO {

    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordDAO examRecordDAO;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AdditionalAttributesDAO additionalAttributesDAO;

    public ExamDAOImpl(
            final ExamRecordMapper examRecordMapper,
            final ExamRecordDAO examRecordDAO,
            final ApplicationEventPublisher applicationEventPublisher,
            final AdditionalAttributesDAO additionalAttributesDAO) {

        this.examRecordMapper = examRecordMapper;
        this.examRecordDAO = examRecordDAO;
        this.applicationEventPublisher = applicationEventPublisher;
        this.additionalAttributesDAO = additionalAttributesDAO;
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
    @Transactional(readOnly = true)
    public Result<Exam> byExternalIdLike(final String internalQuizIdLike) {
        return Result.tryCatch(() -> {
            final List<ExamRecord> execute = examRecordMapper.selectByExample()
                    .where(externalId, isLike(internalQuizIdLike))
                    .build()
                    .execute();

            if (execute == null || execute.isEmpty()) {
                throw new NoResourceFoundException(EntityType.EXAM, "No exam found for external_id like" + internalQuizIdLike);
            }

            if (execute.size() > 1) {
                throw new IllegalStateException("To many exams found for external_id like" + internalQuizIdLike);
            }

            return execute.get(0);
        }).flatMap(this::toDomainModel);
    }

    @Override
    public Result<GrantEntity> examGrantEntityByPK(final Long id) {
        return this.examRecordDAO.recordById(id)
                .map(record -> toDomainModel(record).getOrThrow());
    }

    @Override
    public Result<GrantEntity> examGrantEntityByClientConnection(final Long connectionId) {
        return this.examRecordDAO
                .recordByClientConnection(connectionId)
                .map(record -> toDomainModel(record).getOrThrow());
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

            return this.examRecordDAO
                    .allMatching(filterMap, null)
                    .flatMap(this::toDomainModel)
                    .getOrThrow()
                    .stream()
                    .filter(predicate)
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
        return this.checkStateEdit(exam)
                .flatMap(this.examRecordDAO::save)
                .flatMap(rec -> saveAdditionalAttributes(exam, rec))
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<QuizData> updateQuizData(
            final Long examId,
            final QuizData quizData,
            final String updateId) {

        return this.examRecordDAO
                .updateFromQuizData(examId, quizData, updateId)
                .map(rec -> saveAdditionalQuizAttributes(examId, quizData));
    }

    @Override
    @Transactional
    public void markUpdate(final Long examId) {

        try {

            final long millisecondsNow = Utils.getMillisecondsNow();

            UpdateDSL.updateWithMapper(
                    this.examRecordMapper::update,
                    ExamRecordDynamicSqlSupport.examRecord)
                    .set(ExamRecordDynamicSqlSupport.lastModified)
                    .equalTo(millisecondsNow)
                    .where(ExamRecordDynamicSqlSupport.id, isEqualTo(examId))
                    .build()
                    .execute();

        } catch (final Exception e) {
            log.error("Failed to mark exam for update on distributed setup. exam: {}", examId, e);
        }
    }

    @Override
    public void markLMSAvailability(final String externalQuizId, final boolean available, final String updateId) {

        if (!available) {
            log.info("Mark exam quiz data not available from LMS: {}", externalQuizId);
        } else {
            log.info("Mark exam quiz data back again from LMS: {}", externalQuizId);
        }

        this.examRecordDAO.idByExternalQuizId(externalQuizId)
                .flatMap(examId -> this.examRecordDAO.updateLmsNotAvailable(examId, available, updateId))
                .onError(error -> log.error("Failed to mark LMS not available: {}", externalQuizId, error));
    }

    @Override
    public void updateQuitPassword(final Exam exam, final String quitPassword) {
        this.examRecordDAO
                .updateQuitPassword(exam, quitPassword)
                .onError(err -> log.error("Failed to update quit password on exam: {}", exam, err));
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
                .flatMap(rec -> saveAdditionalAttributes(exam, rec))
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

            UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .set(ExamRecordDynamicSqlSupport.active).equalTo(BooleanUtils.toInteger(active))
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .and(ExamRecordDynamicSqlSupport.status, isNotEqualTo(ExamStatus.ARCHIVED.name()))
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
    public Result<Collection<Exam>> getExamsForStatus(
            final FilterMap filterMap,
            final Predicate<Exam> predicate,
            final ExamStatus... status) {

        return Result.tryCatch(() -> {

            final List<String> stateNames = (status != null && status.length > 0)
                    ? Arrays.stream(status).map(Enum::name)
                            .collect(Collectors.toList())
                    : null;
            return this.examRecordDAO
                    .allMatching(filterMap, stateNames)
                    .flatMap(this::toDomainModel)
                    .getOrThrow()
                    .stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
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
    public Result<Collection<Exam>> allForLMSUpdate() {
        return Result.tryCatch(() -> this.examRecordMapper.selectByExample()
                .where(
                        ExamRecordDynamicSqlSupport.active,
                        isEqualTo(BooleanUtils.toInteger(true)))
                .and(
                        ExamRecordDynamicSqlSupport.lmsSetupId,
                        isNotNull())
                .and(
                        ExamRecordDynamicSqlSupport.status,
                        isNotEqualTo(ExamStatus.ARCHIVED.name()))
                .and(
                        ExamRecordDynamicSqlSupport.updating,
                        isEqualTo(BooleanUtils.toInteger(false)))

                .build()
                .execute())
                .flatMap(this::toDomainModel);
    }

    @Override
    public Result<Collection<Exam>> allThatNeedsStatusUpdate(final long leadTime, final long followupTime) {
        return this.examRecordDAO
                .allThatNeedsStatusUpdate(leadTime, followupTime)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Result<Long> placeLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.examRecordDAO
                    .recordById(examId)
                    .getOrThrow();

            // consistency check
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(examRec.getUpdating()))) {
                throw new IllegalStateException(
                        "Exam to place lock is not in expected state: " + examRec.getExternalId());
            }

            UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                    .set(updating).equalTo(BooleanUtils.toInteger(true))
                    .set(lastupdate).equalTo(updateId)
                    .where(id, isEqualTo(examId))
                    .build()
                    .execute();

            return examId;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Result<Long> releaseLock(final Long examId, final String updateId) {
        return Result.tryCatch(() -> {

            final ExamRecord examRec = this.examRecordDAO
                    .recordById(examId)
                    .getOrThrow();

            // consistency check
            if (BooleanUtils.isFalse(BooleanUtils.toBooleanObject(examRec.getUpdating()))
                    || !updateId.equals(examRec.getLastupdate())) {

                throw new IllegalStateException(
                        "Exam to release lock is not in expected state: " + examRec.getExternalId());
            }

            UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                    .set(updating).equalTo(BooleanUtils.toInteger(false))
                    .set(lastupdate).equalTo(updateId)
                    .where(id, isEqualTo(examId))
                    .build()
                    .execute();

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

        UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                .set(updating).equalTo(BooleanUtils.toInteger(false))
                .where(id, isEqualTo(examId))
                .build()
                .execute();

            return examId;
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
                lockedRecords.forEach(record -> {
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
                        log.warn("Failed to release aged write lock for exam: {} cause: {}", record, e.getMessage());
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
    public synchronized String getAppSignatureKeySalt(final Long examId) {
        final CharSequence salt = KeyGenerators.string().generateKey();
        this.additionalAttributesDAO.initAdditionalAttribute(
                EntityType.EXAM,
                examId,
                Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_SALT, salt.toString());

        return this.additionalAttributesDAO.getAdditionalAttribute(
                EntityType.EXAM,
                examId,
                Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_SALT)
                .getOrThrow()
                .getValue();
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // notify exam deletion listener about following deletion, to clean up stuff before deletion
            this.applicationEventPublisher.publishEvent(new ExamDeletionEvent(ids));

            this.examRecordMapper.deleteByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            // delete all additional attributes
            ids.forEach(id -> this.additionalAttributesDAO.deleteAll(EntityType.EXAM, id));

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
        final Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction;
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
                            Utils.getMillisecondsNow(),
                            rec.getQuizName(),
                            rec.getQuizStartTime(),
                            rec.getQuizEndTime(),
                            rec.getLmsAvailable()));

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
    public Result<Collection<Long>> allIdsOfRunningWithScreenProctoringEnabled() {

        return this.additionalAttributesDAO
                .getAdditionalAttribute(EntityType.EXAM, ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)
                .map(attrs -> {

                    if (attrs.isEmpty()) {
                        return Collections.emptyList();
                    }

                    final List<Long> examIds = attrs
                            .stream()
                            .map(AdditionalAttributeRecord::getEntityId)
                            .collect(Collectors.toList());

                    return this.examRecordMapper.selectIdsByExample()
                            .where(
                                    ExamRecordDynamicSqlSupport.id,
                                    isIn(examIds))
                            .and(
                                    ExamRecordDynamicSqlSupport.active,
                                    isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
                            .and(
                                    ExamRecordDynamicSqlSupport.status,
                                    isEqualTo(ExamStatus.RUNNING.name()))
                            .build()
                            .execute();
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
                .and(
                        ExamRecordDynamicSqlSupport.lmsAvailable,
                        isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
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

    private Result<Collection<Exam>> toDomainModel(final Collection<ExamRecord> records) {
        return Result.tryCatch(() -> records.stream()
                .map(rec -> this.toDomainModel(rec).getOrThrow())
                .collect(Collectors.toList()));
    }

    private Result<Exam> toDomainModel(final ExamRecord record) {

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

            final Map<String, String> additionalAttributes = this.additionalAttributesDAO
                    .getAdditionalAttributes(EntityType.EXAM, record.getId())
                    .getOrThrow()
                    .stream()
                    .collect(Collectors.toMap(
                            AdditionalAttributeRecord::getName,
                            AdditionalAttributeRecord::getValue));

            return new Exam(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getLmsSetupId(),
                    record.getExternalId(),
                    BooleanUtils.toBooleanObject(record.getLmsAvailable()),
                    StringUtils.isNotBlank(record.getQuizName())
                            ? record.getQuizName()
                            : Constants.SQUARE_BRACE_OPEN + record.getExternalId() + Constants.SQUARE_BRACE_CLOSE,
                    record.getQuizStartTime(),
                    record.getQuizEndTime(),
                    ExamType.valueOf(record.getType()),
                    record.getOwner(),
                    supporter,
                    status,
                    record.getQuitPassword(),
                    BooleanUtils.toBooleanObject(record.getLmsSebRestriction()),
                    record.getBrowserKeys(),
                    BooleanUtils.toBooleanObject(record.getActive()),
                    record.getLastupdate(),
                    record.getExamTemplateId(),
                    record.getLastModified(),
                    additionalAttributes);
        });
    }

    private Result<ExamRecord> saveAdditionalAttributes(final Exam exam, final ExamRecord rec) {
        return Result.tryCatch(() -> {

            if (exam.additionalAttributesIncluded()) {
                this.additionalAttributesDAO.saveAdditionalAttributes(
                        EntityType.EXAM,
                        rec.getId(),
                        exam.additionalAttributes)
                        .getOrThrow();
            }

            return rec;
        });
    }

    private QuizData saveAdditionalQuizAttributes(final Long examId, final QuizData quizData) {
        final Map<String, String> additionalAttributes = new HashMap<>(quizData.getAdditionalAttributes());
        if (StringUtils.isNotBlank(quizData.description)) {
            additionalAttributes.put(QuizData.QUIZ_ATTR_DESCRIPTION, quizData.description);
        } else {
            additionalAttributes.put(QuizData.QUIZ_ATTR_DESCRIPTION, StringUtils.EMPTY);
        }
        if (StringUtils.isNotBlank(quizData.startURL)) {
            additionalAttributes.put(QuizData.QUIZ_ATTR_START_URL, quizData.startURL);
        }

        additionalAttributes.forEach((key, value) -> {
            if (value == null) {
                this.additionalAttributesDAO.delete(
                        EntityType.EXAM,
                        examId,
                        key);
            } else {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                                EntityType.EXAM,
                                examId,
                                key,
                                value)
                        .onError(error -> log.error("Failed to save additional quiz attribute: {}",
                                key,
                                error));
            }
        });

        return quizData;
    }

    private Result<Exam> checkStateEdit(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.status == ExamStatus.ARCHIVED) {
                throw new APIMessageException(APIMessage.ErrorMessage.INTEGRITY_VALIDATION.of("Exam is archived"));
            }

            return exam;
        });
    }

}
