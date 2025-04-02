/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.*;
import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.lmsSetupId;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ExamUUIDMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter;
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
    private final ExamUUIDMapper examUUIDMapper;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final Cryptor cryptor;

    public ExamRecordDAO(
            final ExamRecordMapper examRecordMapper,
            final ExamUUIDMapper examUUIDMapper,
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final Cryptor cryptor) {

        this.examRecordMapper = examRecordMapper;
        this.examUUIDMapper = examUUIDMapper;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
        this.cryptor = cryptor;
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
            if (StringUtils.isBlank(quizId)) {
                return Collections.emptyList();
            }

            return this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.externalId,
                            isEqualTo(quizId))
                    .and(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualToWhenPresent(BooleanUtils.toIntegerObject(true)))
                    .build()
                    .execute()
                    .stream()
                    .map(ExamRecord::getInstitutionId)
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

            final String supporterUUID = filterMap.getSQLWildcard(FilterMap.ATTR_SUPPORTER_USER_ID);
            if (StringUtils.isNotBlank(supporterUUID)) {
                whereClause = whereClause.and(
                        supporter,
                        SqlBuilder.isLike(supporterUUID));
            }

            // SEBSERV-298
            if (filterMap.getBoolean(Exam.FILTER_ATTR_HIDE_MISSING)) {
                whereClause = whereClause.and(
                        ExamRecordDynamicSqlSupport.lmsAvailable,
                        SqlBuilder.isGreaterThan(0));
            }

            final String examStatus = filterMap.getExamStatus();
            if (StringUtils.isNotBlank(examStatus)) {
                if (examStatus.contains(Constants.LIST_SEPARATOR)) {
                    final List<String> state_names = Arrays.asList(StringUtils.split(examStatus, Constants.LIST_SEPARATOR));
                    whereClause = whereClause
                            .and(
                                    ExamRecordDynamicSqlSupport.status,
                                    isIn(state_names));
                } else {
                    whereClause = whereClause
                            .and(
                                    ExamRecordDynamicSqlSupport.status,
                                    isEqualToWhenPresent(examStatus));
                }
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

            final Long startTime = filterMap.getLong(Exam.FILTER_ATTR_START_TIME_MILLIS);
            
            // if start timestamp is available use user day span search for quiz start time
            if (startTime != null) {
                final DateTimeZone userTimeZone = filterMap.getUserTimeZone();
                final Pair<Long, Long> userDaySpanMillis = Utils.getUserDaySpanMillis(startTime, userTimeZone);
                if (userDaySpanMillis != null) {
                    whereClause = whereClause
                            .and(
                                    quizStartTime,
                                    isGreaterThanOrEqualTo( Utils.toDateTimeUTC(userDaySpanMillis.a)),
                                    and(
                                            quizStartTime, 
                                            isLessThanOrEqualTo(Utils.toDateTimeUTC(userDaySpanMillis.b))));
                }
            // if old exam from time filter is available apply old search
            } else if (filterMap.getExamFromTime() != null) {
                whereClause = whereClause
                        .and(
                                ExamRecordDynamicSqlSupport.quizEndTime,
                                isGreaterThanOrEqualToWhenPresent(filterMap.getExamFromTime()),
                                or(ExamRecordDynamicSqlSupport.quizEndTime, isNull()));
            }

            final String nameCriteria = filterMap.contains(QuizData.FILTER_ATTR_NAME)
                    ? filterMap.getSQLWildcard(QuizData.FILTER_ATTR_NAME)
                    : filterMap.getSQLWildcard(Domain.EXAM.ATTR_QUIZ_NAME);

            return whereClause
                    .and(
                            ExamRecordDynamicSqlSupport.quizName,
                            isLikeWhenPresent(nameCriteria))
                    .build()
                    .execute();
        });
    }

    @Transactional
    public Result<ExamRecord> updateQuitPassword(final Exam exam, final String pwd) {
        return Result.tryCatch(() -> {
            final String examQuitPassword = exam.quitPassword != null
                    ? this.cryptor
                    .decrypt(exam.quitPassword)
                    .getOr(exam.quitPassword)
                    .toString()
                    : null;

            if (Objects.equals(examQuitPassword, pwd)) {
                return this.examRecordMapper.selectByPrimaryKey(exam.id);
            }

            UpdateDSL.updateWithMapper(examRecordMapper::update, examRecord)
                    .set(quitPassword).equalTo(getEncryptedQuitPassword(pwd))
                    .where(id, isEqualTo(exam.id))
                    .build()
                    .execute();


            return this.examRecordMapper.selectByPrimaryKey(exam.id);
        })
                .onError(TransactionHandler::rollback);

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

                    UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                            .set(ExamRecordDynamicSqlSupport.status).equalTo(status.name())
                            .set(lastModified).equalTo(Utils.getMillisecondsNow())
                            .where(id,  isEqualTo(examRecord.getId()))
                            .build()
                            .execute();

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
                log.info(
                    "Exam state change on save. Exam. {}, Old state: {}, new state: {}",
                    exam.externalId,
                    oldRecord.getStatus(),
                    exam.status);
            }

            final UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> clause = UpdateDSL.updateWithMapper(
                            examRecordMapper::update,
                            examRecord)
                    .set(supporter).equalTo((exam.supporter != null)
                            ? StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null)
                    .set(type).equalTo((exam.type != null)
                            ? exam.type.name()
                            : ExamType.UNDEFINED.name())
                    .set(browserKeys).equalToWhenPresent(exam.browserExamKeys)
                    .set(lmsSebRestriction).equalTo(1) // seb restriction (deprecated)
                    .set(examTemplateId).equalTo(oldRecord.getExamTemplateId())
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .set(quizName).equalToWhenPresent(exam.lmsSetupId == null ? exam.name : null)
                    .set(quizStartTime).equalToWhenPresent(exam.lmsSetupId == null ? exam.startTime : null)
                    .set(quizEndTime).equalToWhenPresent(exam.lmsSetupId == null ? exam.endTime : null)
                    .set(followupId).equalTo(exam.followUpId);

            if (StringUtils.isBlank(exam.quitPassword)) {
                clause.set(quitPassword).equalToNull();
            } else {
                clause.set(quitPassword).equalTo(getEncryptedQuitPassword(exam.quitPassword));
            }
            
            clause
                    .where(id, isEqualTo(exam.id))
                    .build()
                    .execute();

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

            UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                    .set(lastupdate).equalTo(updateId)
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .set(lmsAvailable).equalTo(BooleanUtils.toIntegerObject(available))
                    .where(id,  isEqualTo(examId))
                    .build()
                    .execute();

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

            UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                    .set(status).equalTo(ExamStatus.ARCHIVED.name())
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .set(lmsAvailable).equalTo(BooleanUtils.toIntegerObject(false))
                    .where(id,  isEqualTo(examId))
                    .build()
                    .execute();

            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> setSEBRestriction(final Long examId, final boolean sebRestriction) {
        return Result.tryCatch(() -> {

            UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                    .set(lmsSebRestriction).equalTo(BooleanUtils.toInteger(sebRestriction))
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .where(id,  isEqualTo(examId))
                    .build()
                    .execute();

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
                    getEncryptedQuitPassword(exam.quitPassword),
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
                    BooleanUtils.toIntegerObject(true),
                    exam.followUpId);

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
                    and(ExamRecordDynamicSqlSupport.quizEndTime, isNull(),
                            or(
                                    ExamRecordDynamicSqlSupport.quizEndTime, 
                                    SqlBuilder.isGreaterThanOrEqualTo(now.plus(leadTime)))));

            // if up-coming but running or finished
            final SqlCriterion<String> upcoming = or(
                    ExamRecordDynamicSqlSupport.status,
                    isIn(ExamStatus.UP_COMING.name(), ExamStatus.TEST_RUN.name()),
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
                            and(ExamRecordDynamicSqlSupport.quizEndTime, isNull(), 
                                    or(
                                            ExamRecordDynamicSqlSupport.quizEndTime, 
                                            SqlBuilder.isGreaterThanOrEqualTo(now.minus(followupTime)))),
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

    @Transactional
    public Result<ExamRecord> saveBrowserExamKeys(final Long examId, final String bek) {
        return Result.tryCatch(() -> {

            UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                    .set(browserKeys).equalTo(bek)
                    .set(lastModified).equalTo(Utils.getMillisecondsNow())
                    .where(id,  isEqualTo(examId))
                    .build()
                    .execute();

            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
        .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> updateSupporterAccounts(final Long examId, final List<String> supporterUUIDs) {
        return Result.tryCatch(() -> {

                    final String joinedUUIds = StringUtils.join(supporterUUIDs, Constants.COMMA);
                    UpdateDSL.updateWithMapper(examRecordMapper::update, ExamRecordDynamicSqlSupport.examRecord)
                            .set(supporter).equalTo(joinedUUIds)
                            .set(lastModified).equalTo(Utils.getMillisecondsNow())
                            .where(id,  isEqualTo(examId))
                            .build()
                            .execute();

                    return this.examRecordMapper.selectByPrimaryKey(examId);
                })
                .onError(TransactionHandler::rollback);
    }

    @Transactional(readOnly = true)
    public int numOfExamsReferencingSupporter(final String uuid) {
        try {
            
            return examRecordMapper.countByExample()
                    .where(supporter, isLike(Utils.toSQLWildcard(uuid)))
                    .build()
                    .execute()
                    .intValue();
            
        } catch (final Exception e) {
            log.error("Failed to verify number of exams with supporter accounts. cause: {}", e.getMessage());
            return -1;
        }
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamUUIDMapper.ExamUUID>> allExamUUIDs(final Long lmsSetupId) {
        return Result.tryCatch(() -> {

            if (lmsSetupId == null) {
                return examUUIDMapper
                        .selectByExample()
                        .build()
                        .execute();
            } else {
                return examUUIDMapper
                        .selectByExample()
                        .where(ExamRecordDynamicSqlSupport.lmsSetupId, isEqualTo(lmsSetupId))
                        .build()
                        .execute();
            }
        });
    }

    @Transactional(readOnly = true)
    public Pair<Long, Long> getConsecutiveStartExamId(final Long examId) {
        try {

            final List<ExamRecord> records = examRecordMapper.selectByExample()
                    .where(followupId, isEqualTo(examId))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return null;
            }

            final ExamRecord examRecord = records.get(0);
            if (examRecord.getFollowupId() == null) {
                return null;
            }
            return new Pair<>(examRecord.getId(), examRecord.getFollowupId());

        } catch (final Exception e) {
            log.error("Failed to verify and get Consecutive Start Exam Id. Cause: {}", e.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> possibleConsecutiveExams(final Exam exam, final DateTimeZone timeZone) {
        return Result.tryCatch(() -> {

            final Set<Long> exclude = new HashSet<>(
                    this.examRecordMapper.selectByExample()
                            .where(followupId, isNotNull())
                            .build()
                            .execute()
                            .stream()
                            .map(ExamRecord::getFollowupId)
                    .toList());
            exclude.add(exam.id);
            ExamRecord exclAlso = this.examRecordMapper.selectByExample()
                    .where(followupId, isEqualTo(exam.id))
                    .build()
                    .execute()
                    .stream().findFirst().orElse(null);
            if (exclAlso != null) {
                exclude.add(exclAlso.getId());
            }

            final DateTime startTime = exam.getStartTime();
            final DateTime dateTime = startTime.toDateTime(timeZone);
            final Pair<Long, Long> userDaySpanMillis = Utils.getUserDaySpanMillis(dateTime.getMillis(), timeZone);

            List<ExamRecord> execute = this.examRecordMapper
                    .selectByExample()
                    .where(institutionId, isEqualToWhenPresent(exam.institutionId))
                    .and(active, isNotEqualTo(0))
                    .and(id, isNotInWhenPresent(exclude.isEmpty() ? null : exclude))
                    .and(status, isNotIn(ExamStatus.ARCHIVED.name(), ExamStatus.FINISHED.name()))
                    .and(quizStartTime,
                            isGreaterThanOrEqualTo(Utils.toDateTimeUTC(userDaySpanMillis.a)),
                            and(
                                    quizStartTime,
                                    isLessThanOrEqualTo(Utils.toDateTimeUTC(userDaySpanMillis.b))))
                    .or(id, isEqualToWhenPresent(exam.followUpId))
                    .build()
                    .execute();
            
            return execute;
        });
    }

    private String getEncryptedQuitPassword(final String pwd) {
        return (StringUtils.isNotBlank(pwd))
                ?  this.cryptor
                .encryptCheckAlreadyEncrypted(pwd)
                .onError(err -> log.error("failed to encrypt quit password, skip...", err))
                .getOr(pwd)
                .toString()
                : null;
    }


    
}

