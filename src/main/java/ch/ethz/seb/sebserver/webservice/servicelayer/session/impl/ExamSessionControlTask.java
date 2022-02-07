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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;

@Service
@WebServiceProfile
public class ExamSessionControlTask implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionControlTask.class);

    private final ExamDAO examDAO;
    private final SEBClientConnectionService sebClientConnectionService;
    private final ExamUpdateHandler examUpdateHandler;
    private final ExamProctoringRoomService examProcotringRoomService;
    private final WebserviceInfo webserviceInfo;

    private final Long examTimePrefix;
    private final Long examTimeSuffix;
    private final String examTaskCron;
    private final long pingUpdateRate;

    protected ExamSessionControlTask(
            final ExamDAO examDAO,
            final SEBClientConnectionService sebClientConnectionService,
            final ExamUpdateHandler examUpdateHandler,
            final ExamProctoringRoomService examProcotringRoomService,
            final WebserviceInfo webserviceInfo,
            @Value("${sebserver.webservice.api.exam.time-prefix:3600000}") final Long examTimePrefix,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix,
            @Value("${sebserver.webservice.api.exam.update-interval:1 * * * * *}") final String examTaskCron,
            @Value("${sebserver.webservice.api.exam.update-ping:5000}") final Long pingUpdateRate) {

        this.examDAO = examDAO;
        this.sebClientConnectionService = sebClientConnectionService;
        this.examUpdateHandler = examUpdateHandler;
        this.webserviceInfo = webserviceInfo;
        this.examTimePrefix = examTimePrefix;
        this.examTimeSuffix = examTimeSuffix;
        this.examTaskCron = examTaskCron;
        this.pingUpdateRate = pingUpdateRate;
        this.examProcotringRoomService = examProcotringRoomService;
    }

    @EventListener(SEBServerInitEvent.class)
    public void init() {
        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Activate exam run controller background task");
        SEBServerInit.INIT_LOGGER.info("--------> Task runs on an cron-job interval of {}", this.examTaskCron);
        SEBServerInit.INIT_LOGGER.info(
                "--------> Real exam running time span is expanded on {} before start and {} milliseconds after ending",
                this.examTimePrefix,
                this.examTimeSuffix);

        this.updateMaster();

        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info(
                "------> Activate SEB lost-ping-event update background task on a fix rate of: {} milliseconds",
                this.pingUpdateRate);
    }

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.exam.update-interval:60000}",
            initialDelay = 10000)
    public void examRunUpdateTask() {

        if (!this.webserviceInfo.isMaster()) {
            return;
        }

        final String updateId = this.examUpdateHandler.createUpdateId();

        if (log.isDebugEnabled()) {
            log.debug("Run exam update task with Id: {}", updateId);
        }

        controlExamStart(updateId);
        controlExamEnd(updateId);
        this.examDAO.releaseAgedLocks();
    }

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.seb.lostping.update:5000}",
            initialDelay = 5000)
    public void examSessionUpdateTask() {

        updateMaster();

        if (!this.webserviceInfo.isMaster()) {
            return;
        }

        this.sebClientConnectionService.updatePingEvents();

        if (log.isTraceEnabled()) {
            log.trace("Run exam session update task");
        }

        this.sebClientConnectionService.cleanupInstructions();
        this.examProcotringRoomService.updateProctoringCollectingRooms();
    }

    @Scheduled(
            fixedRateString = "${sebserver.webservice.api.exam.session-cleanup:30000}",
            initialDelay = 30000)
    public void examSessionCleanupTask() {

        if (!this.webserviceInfo.isMaster()) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Run exam session cleanup task");
        }

        this.sebClientConnectionService.cleanupInstructions();
    }

    private void controlExamStart(final String updateId) {
        if (log.isTraceEnabled()) {
            log.trace("Check starting exams: {}", updateId);
        }

        try {

            final DateTime now = DateTime.now(DateTimeZone.UTC);
            final Map<Long, String> updated = this.examDAO.allForRunCheck()
                    .getOrThrow()
                    .stream()
                    .filter(exam -> exam.startTime.minus(this.examTimePrefix).isBefore(now))
                    .filter(exam -> exam.endTime != null && exam.endTime.plus(this.examTimeSuffix).isAfter(now))
                    .flatMap(exam -> Result.skipOnError(this.examUpdateHandler.setRunning(exam, updateId)))
                    .collect(Collectors.toMap(Exam::getId, Exam::getName));

            if (!updated.isEmpty()) {
                log.info("Updated exams to running state: {}", updated);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update exams: ", e);
        }
    }

    private void controlExamEnd(final String updateId) {
        if (log.isTraceEnabled()) {
            log.trace("Check ending exams: {}", updateId);
        }

        try {

            final DateTime now = DateTime.now(DateTimeZone.UTC);

            final Map<Long, String> updated = this.examDAO.allForEndCheck()
                    .getOrThrow()
                    .stream()
                    .filter(exam -> exam.endTime != null && exam.endTime.plus(this.examTimeSuffix).isBefore(now))
                    .flatMap(exam -> Result.skipOnError(this.examUpdateHandler.setFinished(exam, updateId)))
                    .flatMap(exam -> Result.skipOnError(this.examProcotringRoomService.disposeRoomsForExam(exam)))
                    .collect(Collectors.toMap(Exam::getId, Exam::getName));

            if (!updated.isEmpty()) {
                log.info("Updated exams to finished state: {}", updated);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update exams: ", e);
        }
    }

    private void updateMaster() {
        this.webserviceInfo.updateMaster();
    }

    @Override
    public void destroy() {
        // TODO try to reset master
    }

}
