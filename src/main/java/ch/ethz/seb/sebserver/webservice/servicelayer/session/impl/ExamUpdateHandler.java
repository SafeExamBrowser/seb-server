/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SebRestrictionData;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;

@Lazy
@Service
@WebServiceProfile
class ExamUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamUpdateHandler.class);

    private final ExamDAO examDAO;
    private final SebExamConfigService sebExamConfigService;
    private final LmsAPIService lmsAPIService;
    private final String updatePrefix;
    private final Long examTimeSuffix;

    public ExamUpdateHandler(
            final ExamDAO examDAO,
            final SebExamConfigService sebExamConfigService,
            final LmsAPIService lmsAPIService,
            final WebserviceInfo webserviceInfo,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix) {

        this.examDAO = examDAO;
        this.sebExamConfigService = sebExamConfigService;
        this.lmsAPIService = lmsAPIService;
        this.updatePrefix = webserviceInfo.getHostAddress()
                + "_" + webserviceInfo.getServerPort() + "_";
        this.examTimeSuffix = examTimeSuffix;
    }

    String createUpdateId() {
        return this.updatePrefix + Utils.getMillisecondsNow();
    }

    Result<Exam> updateRunning(final Long examId) {
        return this.examDAO.byPK(examId)
                .map(exam -> {
                    final DateTime now = DateTime.now(DateTimeZone.UTC);
                    if (exam.getStatus() == ExamStatus.UP_COMING
                            && exam.endTime.plus(this.examTimeSuffix).isBefore(now)) {
                        return setRunning(exam, this.createUpdateId());
                    } else {
                        return exam;
                    }
                });
    }

    Exam setRunning(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as running: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.save(new Exam(
                        exam.id,
                        ExamStatus.RUNNING)))
                .flatMap(this::applySebClientRestriction)
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .onError(error -> this.examDAO.forceUnlock(exam.id))
                .getOrThrow();
    }

    Exam setFinished(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as finished: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.save(new Exam(
                        exam.id,
                        ExamStatus.FINISHED)))
                .flatMap(this::releaseSebClientRestriction)
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .onError(error -> this.examDAO.forceUnlock(exam.id))
                .getOrThrow();
    }

    Result<Exam> applySebClientRestriction(final Exam exam) {
        if (!exam.lmsSebRestriction) {
            if (log.isDebugEnabled()) {
                log.debug("Skip SEB Client restrictions for exam: {}", exam);
            }

            return Result.of(exam);
        }

        if (log.isDebugEnabled()) {
            log.debug("Apply SEB Client restrictions for exam: {}", exam);
        }

        return Result.tryCatch(() -> {

            final SebRestrictionData sebRestrictionData = createSebRestrictionData(exam);

            if (log.isDebugEnabled()) {
                log.debug("Appling SEB Client restriction on LMS with: {}", sebRestrictionData);
            }

            return this.lmsAPIService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .flatMap(lmsTemplate -> lmsTemplate.applySebClientRestriction(sebRestrictionData))
                    .map(data -> exam)
                    .getOrThrow();
        });
    }

    Result<Exam> updateSebClientRestriction(final Exam exam) {
        if (!exam.lmsSebRestriction) {
            if (log.isDebugEnabled()) {
                log.debug("Skip SEB Client restrictions update for exam: {}", exam);
            }

            return Result.of(exam);
        }

        return Result.tryCatch(() -> {

            final SebRestrictionData sebRestrictionData = createSebRestrictionData(exam);

            if (log.isDebugEnabled()) {
                log.debug("Update SEB Client restrictions for exam: {}", exam);
            }

            return this.lmsAPIService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .flatMap(lmsTemplate -> lmsTemplate.updateSebClientRestriction(sebRestrictionData))
                    .map(data -> exam)
                    .getOrThrow();
        });
    }

    Result<Exam> releaseSebClientRestriction(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Release SEB Client restrictions for exam: {}", exam);
        }

        return this.lmsAPIService
                .getLmsAPITemplate(exam.lmsSetupId)
                .flatMap(template -> template.releaseSebClientRestriction(exam));
    }

//    @Transactional
//    public <T> Result<T> updateExamConfigurationMappingChange(
//            final ExamConfigurationMap mapping,
//            final Function<ExamConfigurationMap, Result<T>> changeAction,
//            final Exam exam) {
//
//        return Result.tryCatch(() -> {
//
//            return result;
//        })
//                .onError(TransactionHandler::rollback);
//    }

    private SebRestrictionData createSebRestrictionData(final Exam exam) {
        final Collection<String> configKeys = this.sebExamConfigService
                .generateConfigKeys(exam.institutionId, exam.id)
                .getOrThrow();

        final Collection<String> browserExamKeys = new ArrayList<>();
        final String browserExamKeysString = exam.getBrowserExamKeys();
        if (StringUtils.isNotBlank(browserExamKeysString)) {
            browserExamKeys.addAll(Arrays.asList(StringUtils.split(
                    browserExamKeysString,
                    Constants.LIST_SEPARATOR)));
        }

        final SebRestrictionData sebRestrictionData = new SebRestrictionData(
                exam,
                configKeys,
                browserExamKeys,
                // TODO when we have more restriction details available form the Exam, put it to the map
                Collections.emptyMap());
        return sebRestrictionData;
    }

}
