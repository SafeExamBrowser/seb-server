/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
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
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DuplicateResourceException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ExamRecordDAO {

    private static final Logger log = LoggerFactory.getLogger(ExamRecordDAO.class);

    private final ExamRecordMapper examRecordMapper;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;

    public ExamRecordDAO(
            final ExamRecordMapper examRecordMapper,
            final ClientConnectionRecordMapper clientConnectionRecordMapper) {

        this.examRecordMapper = examRecordMapper;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
    }

    @Transactional(readOnly = true)
    public Result<ExamRecord> recordById(final Long id) {
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

    @Transactional(readOnly = true)
    public Result<Long> idByExternalQuizId(final String externalQuizId) {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectIdsByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.externalId,
                            isEqualToWhenPresent(externalQuizId))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());
        });
    }

    @Transactional(readOnly = true)
    public Result<ExamRecord> recordByClientConnection(final Long connectionId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByPrimaryKey(connectionId))
                .flatMap(ccRecord -> recordById(ccRecord.getExamId()));
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> all(final Long institutionId, final Boolean active) {
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
                        .execute());
    }

    @Transactional(readOnly = true)
    public Result<Collection<Long>> allInstitutionIdsByQuizId(final String quizId) {
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
                    .map(rec -> rec.getInstitutionId())
                    .collect(Collectors.toList());
        });
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> allMatching(final FilterMap filterMap, final List<String> stateNames) {

        return Result.tryCatch(() -> {

            // If we have a sort on institution name, join the institution table
            // If we have a sort on lms setup name, join lms setup table
            QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>>.QueryExpressionWhereBuilder whereClause =
                    (filterMap.getBoolean(FilterMap.ATTR_ADD_INSITUTION_JOIN))
                            ? this.examRecordMapper
                                    .selectByExample()
                                    .join(InstitutionRecordDynamicSqlSupport.institutionRecord)
                                    .on(
                                            InstitutionRecordDynamicSqlSupport.id,
                                            SqlBuilder.equalTo(ExamRecordDynamicSqlSupport.institutionId))
                                    .where(
                                            ExamRecordDynamicSqlSupport.active,
                                            isEqualToWhenPresent(filterMap.getActiveAsInt()))
                            : (filterMap.getBoolean(FilterMap.ATTR_ADD_LMS_SETUP_JOIN))
                                    ? this.examRecordMapper
                                            .selectByExample()
                                            .join(LmsSetupRecordDynamicSqlSupport.lmsSetupRecord)
                                            .on(
                                                    LmsSetupRecordDynamicSqlSupport.id,
                                                    SqlBuilder.equalTo(ExamRecordDynamicSqlSupport.lmsSetupId))
                                            .where(
                                                    ExamRecordDynamicSqlSupport.active,
                                                    isEqualToWhenPresent(filterMap.getActiveAsInt()))
                                    : this.examRecordMapper.selectByExample()
                                            .where(
                                                    ExamRecordDynamicSqlSupport.active,
                                                    isEqualToWhenPresent(filterMap.getActiveAsInt()));

            whereClause = whereClause
                    .and(
                            ExamRecordDynamicSqlSupport.institutionId,
                            isEqualToWhenPresent(filterMap.getInstitutionId()))
                    .and(
                            ExamRecordDynamicSqlSupport.lmsSetupId,
                            isEqualToWhenPresent(filterMap.getLmsSetupId()))
                    .and(
                            ExamRecordDynamicSqlSupport.type,
                            isEqualToWhenPresent(filterMap.getExamType()));

            // SEBSERV-298
            if (filterMap.getBoolean(Exam.FILTER_ATTR_HIDE_MISSING)) {
                whereClause = whereClause.and(
                        ExamRecordDynamicSqlSupport.lmsAvailable,
                        SqlBuilder.isGreaterThan(0));
            }

            final String examStatus = filterMap.getExamStatus();
            if (StringUtils.isNotBlank(examStatus)) {
                whereClause = whereClause
                        .and(
                                ExamRecordDynamicSqlSupport.status,
                                isEqualToWhenPresent(examStatus));
            } else if (stateNames != null && !stateNames.isEmpty()) {
                whereClause = whereClause
                        .and(
                                ExamRecordDynamicSqlSupport.status,
                                isIn(stateNames));
            } else {
                // for default the archived state is not presented only on explicit request
                whereClause = whereClause
                        .and(
                                ExamRecordDynamicSqlSupport.status,
                                isNotEqualTo(ExamStatus.ARCHIVED.name()));
            }

            if (filterMap.getExamFromTime() != null) {
                whereClause = whereClause
                        .and(
                                ExamRecordDynamicSqlSupport.quizEndTime,
                                isGreaterThanOrEqualToWhenPresent(filterMap.getExamFromTime()),
                                or(ExamRecordDynamicSqlSupport.quizEndTime, isNull()));
            }

            final String nameCriteria = filterMap.contains(QuizData.FILTER_ATTR_NAME)
                    ? filterMap.getSQLWildcard(QuizData.FILTER_ATTR_NAME)
                    : filterMap.getSQLWildcard(Domain.EXAM.ATTR_QUIZ_NAME);

            final List<ExamRecord> records = whereClause
                    .and(
                            ExamRecordDynamicSqlSupport.quizName,
                            isLikeWhenPresent(nameCriteria))
                    .build()
                    .execute();

            return records;
        });
    }

    @Transactional
    public Result<ExamRecord> updateState(final Long examId, final ExamStatus status, final String updateId) {
        return recordById(examId)
                .map(examRecord -> {
                    if (updateId != null &&
                            BooleanUtils.isTrue(BooleanUtils.toBooleanObject(examRecord.getUpdating()))) {

                        if (!updateId.equals(examRecord.getLastupdate())) {
                            throw new IllegalStateException("Exam is currently locked: " + examRecord.getExternalId());
                        }
                    }

                    final ExamRecord newExamRecord = new ExamRecord(
                            examRecord.getId(),
                            null, null, null, null, null, null, null, null,
                            status.name(),
                            null, null, null, null, null,
                            Utils.getMillisecondsNow(), null, null, null, null);

                    this.examRecordMapper.updateByPrimaryKeySelective(newExamRecord);
                    return this.examRecordMapper.selectByPrimaryKey(examId);
                })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> save(final Exam exam) {
        return Result.tryCatch(() -> {

            // check internal persistent write-lock
            final ExamRecord oldRecord = this.examRecordMapper.selectByPrimaryKey(exam.id);
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(oldRecord.getUpdating()))) {
                throw new IllegalStateException("Exam is currently locked: " + exam.externalId);
            }

            if (exam.status != null && !exam.status.name().equals(oldRecord.getStatus())) {
                log.info("Exam state change on save. Exam. {}, Old state: {}, new state: {}",
                        exam.externalId,
                        oldRecord.getStatus(),
                        exam.status);
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
                    null,
                    1, // seb restriction (deprecated)
                    null, // updating
                    null, // lastUpdate
                    null, // active
                    exam.examTemplateId,
                    Utils.getMillisecondsNow(),
                    exam.lmsSetupId == null ? exam.name : null,
                    exam.lmsSetupId == null ? exam.startTime : null,
                    exam.lmsSetupId == null ? exam.endTime : null,
                    null);

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(exam.id);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> updateFromQuizData(
            final Long examId,
            final QuizData quizData,
            final String updateId) {

        return Result.tryCatch(() -> {

            // check internal persistent write-lock
            final ExamRecord oldRecord = this.examRecordMapper.selectByPrimaryKey(examId);
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(oldRecord.getUpdating()))) {
                throw new IllegalStateException("Exam is currently locked: " + examId);
            }

            UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                    .set(externalId).equalTo(quizData.id)
                    .set(lastupdate).equalTo(updateId)
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .set(quizName).equalTo(quizData.getName())
                    .set(quizStartTime).equalTo(quizData.getStartTime())
                    .set(quizEndTime).equalTo(quizData.getEndTime())
                    .where(id, isEqualTo(oldRecord::getId))
                    .build()
                    .execute();

            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> updateLmsNotAvailable(final Long examId, final boolean available, final String updateId) {
        return Result.tryCatch(() -> {

            // check internal persistent write-lock
            final ExamRecord oldRecord = this.examRecordMapper.selectByPrimaryKey(examId);
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(oldRecord.getUpdating()))) {
                throw new IllegalStateException("Exam is currently locked: " + examId);
            }

            final ExamRecord examRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null,
                    null, null,
                    updateId,
                    null, null,
                    Utils.getMillisecondsNow(),
                    null, null, null,
                    BooleanUtils.toIntegerObject(available));

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> archive(final Long examId) {
        return Result.tryCatch(() -> {

            // check internal persistent write-lock
            final ExamRecord oldRecord = this.examRecordMapper.selectByPrimaryKey(examId);
            if (BooleanUtils.isTrue(BooleanUtils.toBooleanObject(oldRecord.getUpdating()))) {
                throw new IllegalStateException("Exam is currently locked: " + examId);
            }

            final ExamRecord examRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null,
                    ExamStatus.ARCHIVED.name(),
                    null, null, null, null, null,
                    Utils.getMillisecondsNow(),
                    null, null, null,
                    BooleanUtils.toIntegerObject(false));

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> setSEBRestriction(final Long examId, final boolean sebRestriction) {
        return Result.tryCatch(() -> {

            final ExamRecord examRecord = new ExamRecord(
                    examId,
                    null, null, null, null, null, null, null, null, null,
                    BooleanUtils.toInteger(sebRestriction),
                    null, null, null, null,
                    Utils.getMillisecondsNow(),
                    null, null, null, null);

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> createNew(final Exam exam) {
        return Result.tryCatch(() -> {

            // fist check if it is not already existing
            if (exam.lmsSetupId != null) {
                final List<ExamRecord> records = this.examRecordMapper.selectByExample()
                        .where(lmsSetupId, isEqualTo(exam.lmsSetupId))
                        .and(externalId, isEqualTo(exam.externalId))
                        .build()
                        .execute();
                // if there is already an existing imported exam for the quiz, this is
                // used to save instead of create a new one
                if (records != null && !records.isEmpty()) {
                    final ExamRecord examRecord = records.get(0);
                    // if the same institution tries to import an exam that already exists throw an error
                    if (exam.institutionId.equals(examRecord.getInstitutionId())) {
                        throw new DuplicateResourceException(EntityType.EXAM, exam.externalId);
                    }
                }
            } else {
                final Long nameCount = this.examRecordMapper.countByExample()
                        .where(institutionId, isEqualTo(exam.institutionId))
                        .and(quizName, isEqualTo(exam.name))
                        .build()
                        .execute();
                if (nameCount > 0) {
                    throw new DuplicateResourceException(EntityType.EXAM, exam.externalId);
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
                    BooleanUtils.toInteger(true),
                    exam.examTemplateId,
                    Utils.getMillisecondsNow(),
                    exam.name,
                    exam.startTime,
                    exam.endTime,
                    BooleanUtils.toIntegerObject(true));

            this.examRecordMapper.insert(examRecord);
            return examRecord;
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> allThatNeedsStatusUpdate(final long leadTime, final long followupTime) {
        return Result.tryCatch(() -> {

            final DateTime now = DateTime.now(DateTimeZone.UTC);
            final List<ExamRecord> result = new ArrayList<>();

            // check those on running state that are not within the time-frame anymore
            final List<ExamRecord> running = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isEqualTo(ExamStatus.RUNNING.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.updating,
                            isEqualTo(BooleanUtils.toInteger(false)))
                    .and( // not within time frame
                            ExamRecordDynamicSqlSupport.quizStartTime,
                            SqlBuilder.isGreaterThanOrEqualToWhenPresent(now.plus(leadTime)),
                            or(
                                    ExamRecordDynamicSqlSupport.quizEndTime,
                                    SqlBuilder.isLessThanWhenPresent(now.minus(followupTime))))
                    .build()
                    .execute();

            // check those in not running state (and not archived) and are within the time-frame or on wrong side of the time-frame
            // if finished but up-coming or running
            final SqlCriterion<String> finished = or(
                    ExamRecordDynamicSqlSupport.status,
                    isEqualTo(ExamStatus.FINISHED.name()),
                    and(
                            ExamRecordDynamicSqlSupport.quizEndTime,
                            SqlBuilder.isGreaterThanOrEqualToWhenPresent(now.plus(leadTime))));

            // if up-coming but running or finished
            final SqlCriterion<String> upcoming = or(
                    ExamRecordDynamicSqlSupport.status,
                    isEqualTo(ExamStatus.UP_COMING.name()),
                    and(
                            ExamRecordDynamicSqlSupport.quizStartTime,
                            SqlBuilder.isLessThanWhenPresent(now.minus(followupTime))),
                    finished);

            final List<ExamRecord> notRunning = this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isNotEqualTo(ExamStatus.RUNNING.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isNotEqualTo(ExamStatus.ARCHIVED.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.updating,
                            isEqualTo(BooleanUtils.toInteger(false)))
                    .and( // within time frame
                            ExamRecordDynamicSqlSupport.quizStartTime,
                            SqlBuilder.isLessThanWhenPresent(now.plus(leadTime)),
                            and(
                                    ExamRecordDynamicSqlSupport.quizEndTime,
                                    SqlBuilder.isGreaterThanOrEqualToWhenPresent(now.minus(followupTime))),
                            upcoming)
                    .build()
                    .execute();

            result.addAll(running);
            result.addAll(notRunning);
            return result;
        });
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.examRecordMapper.selectByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute();
        });
    }

}
