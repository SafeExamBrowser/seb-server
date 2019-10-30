/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;

@Service
class ExamSessionControlTask {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionControlTask.class);

    private final ExamDAO examDAO;
    private final Long examTimePrefix;
    private final Long examTimeSuffix;
    private final String updatePrefix;

    protected ExamSessionControlTask(
            final ExamDAO examDAO,
            final WebserviceInfo webserviceInfo,
            @Value("${sebserver.webservice.api.exam.time-prefix:3600000}") final Long examTimePrefix,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix) {

        this.examDAO = examDAO;
        this.examTimePrefix = examTimePrefix;
        this.examTimeSuffix = examTimeSuffix;
        this.updatePrefix = webserviceInfo.getHostAddress()
                + "_" + webserviceInfo.getServerPort() + "_";
    }

    @Async
    @Scheduled(cron = "${sebserver.webservice.api.exam.update-interval:1 * * * * *}")
    @Transactional
    public void execTask() {

        final String updateId = this.createUpdateId();

        if (log.isDebugEnabled()) {
            log.debug("Run exam runtime update task with Id: {}", updateId);
        }

        controlStart(updateId);

        controlEnd(updateId);
    }

    private void controlStart(final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Check starting exams: {}", updateId);
        }

        try {

            final DateTime now = DateTime.now(DateTimeZone.UTC);

            final Map<Long, String> updated = this.examDAO.allForRunCheck()
                    .getOrThrow()
                    .stream()
                    .filter(exam -> exam.startTime.minus(this.examTimePrefix).isBefore(now))
                    .map(exam -> this.setRunning(exam, updateId))
                    .collect(Collectors.toMap(Exam::getId, Exam::getName));

            if (!updated.isEmpty()) {
                log.info("Updated exams to running state: {}", updated);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update exams: ", e);
        }
    }

    public Result<Exam> updateRunning(final Long examId) {
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

    public String createUpdateId() {
        return this.updatePrefix + Utils.getMillisecondsNow();
    }

    private Exam setRunning(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as running: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.save(new Exam(
                        exam.id,
                        ExamStatus.RUNNING)))
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .getOrThrow();
    }

    private void controlEnd(final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Check ending exams: {}", updateId);
        }

        try {

            final DateTime now = DateTime.now(DateTimeZone.UTC);

            final Map<Long, String> updated = this.examDAO.allForEndCheck()
                    .getOrThrow()
                    .stream()
                    .filter(exam -> exam.endTime.plus(this.examTimeSuffix).isBefore(now))
                    .map(exam -> this.setFinished(exam, updateId))
                    .collect(Collectors.toMap(Exam::getId, Exam::getName));

            if (!updated.isEmpty()) {
                log.info("Updated exams to finished state: {}", updated);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update exams: ", e);
        }
    }

    private Exam setFinished(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as finished: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.save(new Exam(
                        exam.id,
                        ExamStatus.FINISHED)))
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .getOrThrow();
    }

}
