/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
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
    public Result<Collection<ExamRecord>> allMatching(final FilterMap filterMap) {

        return Result.tryCatch(() -> {

            // If we have a sort on institution name, join the institution table
            // If we have a sort on lms setup name, join lms setup table
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>>.QueryExpressionWhereBuilder whereClause =
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

            final List<ExamRecord> records = whereClause
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

            return records;
        });
    }

    @Transactional
    public Result<ExamRecord> updateState(final Long examId, final ExamStatus status, final String updateId) {
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
                            null, null, null, null, null,
                            Utils.getMillisecondsNow());

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
                    null, // active
                    exam.examTemplateId,
                    Utils.getMillisecondsNow());

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(exam.id);
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
                    Utils.getMillisecondsNow());

            this.examRecordMapper.updateByPrimaryKeySelective(examRecord);
            return this.examRecordMapper.selectByPrimaryKey(examId);
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<ExamRecord> createNew(final Exam exam) {
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
                // if the same institution tries to import an exam that already exists throw an error
                if (exam.institutionId.equals(examRecord.getInstitutionId())) {
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
                    Utils.getMillisecondsNow());

            this.examRecordMapper.insert(examRecord);
            return examRecord;
        })
                .onError(TransactionHandler::rollback);
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> allForRunCheck() {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.active,
                            isEqualTo(BooleanUtils.toInteger(true)))
                    .and(
                            ExamRecordDynamicSqlSupport.status,
                            isNotEqualTo(ExamStatus.RUNNING.name()))
                    .and(
                            ExamRecordDynamicSqlSupport.updating,
                            isEqualTo(BooleanUtils.toInteger(false)))
                    .build()
                    .execute();
        });
    }

    @Transactional(readOnly = true)
    public Result<Collection<ExamRecord>> allForEndCheck() {
        return Result.tryCatch(() -> {
            return this.examRecordMapper.selectByExample()
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
