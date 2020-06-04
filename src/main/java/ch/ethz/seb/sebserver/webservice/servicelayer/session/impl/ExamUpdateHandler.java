/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;

@Lazy
@Service
@WebServiceProfile
class ExamUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamUpdateHandler.class);

    private final ExamDAO examDAO;
    private final SEBRestrictionService sebRestrictionService;
    private final String updatePrefix;
    private final Long examTimeSuffix;

    public ExamUpdateHandler(
            final ExamDAO examDAO,
            final SEBRestrictionService sebRestrictionService,
            final WebserviceInfo webserviceInfo,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix) {

        this.examDAO = examDAO;
        this.sebRestrictionService = sebRestrictionService;
        this.updatePrefix = webserviceInfo.getHostAddress()
                + "_" + webserviceInfo.getServerPort() + "_";
        this.examTimeSuffix = examTimeSuffix;
    }

    public SEBRestrictionService getSEBRestrictionService() {
        return this.sebRestrictionService;
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
                .flatMap(e -> this.examDAO.updateState(
                        exam.id,
                        ExamStatus.RUNNING,
                        updateId))
                .flatMap(this.sebRestrictionService::applySEBClientRestriction)
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .onError(error -> this.examDAO.forceUnlock(exam.id)
                        .onError(unlockError -> log.error("Failed to force unlock update look for exam: {}", exam.id)))
                .getOrThrow();
    }

    Exam setFinished(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as finished: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.updateState(
                        exam.id,
                        ExamStatus.FINISHED,
                        updateId))
                .flatMap(this.sebRestrictionService::releaseSEBClientRestriction)
                .flatMap(e -> this.examDAO.releaseLock(e.id, updateId))
                .onError(error -> this.examDAO.forceUnlock(exam.id))
                .getOrThrow();
    }

}
