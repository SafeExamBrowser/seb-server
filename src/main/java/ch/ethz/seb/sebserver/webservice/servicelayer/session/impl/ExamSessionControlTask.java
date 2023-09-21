/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamUpdateTask;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SessionUpdateTask;

@Service
@WebServiceProfile
public class ExamSessionControlTask implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionControlTask.class);

    private final WebserviceInfo webserviceInfo;
    private final List<ExamUpdateTask> examUpdateTasks;
    private final List<SessionUpdateTask> sessionUpdateTasks;

    private final Long examTimePrefix;
    private final Long examTimeSuffix;
    private final String examUpdateRate;
    private final long pingUpdateRate;

    protected ExamSessionControlTask(
            final WebserviceInfo webserviceInfo,
            final Collection<ExamUpdateTask> examUpdateTasks,
            final Collection<SessionUpdateTask> sessionUpdateTasks,
            @Value("${sebserver.webservice.api.exam.time-prefix:3600000}") final Long examTimePrefix,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix,
            @Value("${sebserver.webservice.api.exam.update-interval:60000}") final String examUpdateRate,
            @Value("${sebserver.webservice.api.exam.update-ping:5000}") final Long pingUpdateRate) {

        this.webserviceInfo = webserviceInfo;
        this.examTimePrefix = examTimePrefix;
        this.examTimeSuffix = examTimeSuffix;
        this.examUpdateRate = examUpdateRate;
        this.pingUpdateRate = pingUpdateRate;

        this.examUpdateTasks = new ArrayList<>(examUpdateTasks);
        this.examUpdateTasks.sort((t1, t2) -> Integer.compare(
                t1.examUpdateTaskProcessingOrder(),
                t2.examUpdateTaskProcessingOrder()));

        this.sessionUpdateTasks = new ArrayList<>(sessionUpdateTasks);
        this.sessionUpdateTasks.sort((t1, t2) -> Integer.compare(
                t1.sessionUpdateTaskProcessingOrder(),
                t2.sessionUpdateTaskProcessingOrder()));
    }

    @EventListener(SEBServerInitEvent.class)
    private void init() {
        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Activate exam run controller background task");
        SEBServerInit.INIT_LOGGER.info("--------> Task runs on an interval of {} milliseconds", this.examUpdateRate);
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
    private void examRunUpdateTask() {

        if (!this.webserviceInfo.isMaster()) {
            return;
        }

        this.examUpdateTasks
                .stream()
                .forEach(ExamUpdateTask::processExamUpdateTask);
    }

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.seb.lostping.update:5000}",
            initialDelay = 5000)
    private void examSessionUpdateTask() {

        updateMaster();

        if (!this.webserviceInfo.isMaster()) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Run exam session update task");
        }

        this.sessionUpdateTasks
                .stream()
                .forEach(SessionUpdateTask::processSessionUpdateTask);
    }

    private void updateMaster() {
        this.webserviceInfo.updateMaster();
    }

    @Override
    public void destroy() {
        // TODO try to reset master
    }

}
