/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamConfigUpdateServiceImpl implements ExamConfigUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigUpdateServiceImpl.class);

    private final ExamDAO examDAO;
    private final ConfigurationDAO configurationDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ExamSessionService examSessionService;
    private final ExamUpdateHandler examUpdateHandler;
    private final ExamAdminService examAdminService;

    protected ExamConfigUpdateServiceImpl(
            final ExamDAO examDAO,
            final ConfigurationDAO configurationDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ExamSessionService examSessionService,
            final ExamUpdateHandler examUpdateHandler,
            final ExamAdminService examAdminService) {

        this.examDAO = examDAO;
        this.configurationDAO = configurationDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.examSessionService = examSessionService;
        this.examUpdateHandler = examUpdateHandler;
        this.examAdminService = examAdminService;
    }

    // processing:
    // check running exam integrity (No running exam with active SEB client-connection available)
    // if OK, create an update-id and for each exam, create an update-lock on DB (This also prevents new SEB client connection attempts during update)
    // check running exam integrity again after lock to ensure there where no SEB Client connection attempts in the meantime
    // store the new configuration values (into history) so that they take effect
    // generate the new Config Key and update the Config Key within the LMSSetup API for each exam (delete old Key and add new Key)
    // evict each Exam from cache and release the update-lock on DB
    @Override
    public Result<Collection<Long>> processExamConfigurationChange(final Long configurationNodeId) {

        final String updateId = this.examUpdateHandler.createUpdateId();

        if (log.isDebugEnabled()) {
            log.debug("Process SEB Exam Configuration update for: {} with update-id {}",
                    configurationNodeId,
                    updateId);
        }

        return Result.tryCatch(() -> {

            // check running exam integrity (No running exam with active SEB client-connection available)
            final Collection<Long> examIdsFirstCheck = checkRunningExamIntegrity(configurationNodeId)
                    .getOrThrow();

            if (log.isDebugEnabled()) {
                log.debug("Involved exams on fist integrity check: {}", examIdsFirstCheck);
            }

            // if OK, create an update-lock on DB (This also prevents new SEB client connection attempts during update)
            final Collection<Exam> exams = lockForUpdate(examIdsFirstCheck, updateId)
                    .stream()
                    .map(Result::getOrThrow)
                    .collect(Collectors.toList());

            final Collection<Long> examsIds = exams
                    .stream()
                    .map(Exam::getId)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Update-Lock successfully placed for all involved exams: {}", examsIds);
            }

            // check running exam integrity again after lock to ensure there where no SEB Client connection attempts in the meantime
            final Collection<Long> examIdsSecondCheck = checkRunningExamIntegrity(configurationNodeId)
                    .getOrThrow();

            checkIntegrityDoubleCheck(
                    examIdsFirstCheck,
                    examIdsSecondCheck);

            if (log.isDebugEnabled()) {
                log.debug("Involved exams on second integrity check: {}", examIdsSecondCheck);
            }

            // store the new configuration values (into history) so that they take effect
            final Configuration configuration = this.configurationDAO
                    .saveToHistory(configurationNodeId)
                    .getOrThrow();

            if (log.isDebugEnabled()) {
                log.debug("Successfully save SEB Exam Configuration: {}", configuration);
            }

            // generate the new Config Key and update the Config Key within the LMSSetup API for each exam (delete old Key and add new Key)
            for (final Exam exam : exams) {
                if (exam.getStatus() == ExamStatus.RUNNING || this.examAdminService.isRestricted(exam).getOr(false)) {

                    this.examUpdateHandler
                            .getSEBRestrictionService()
                            .applySEBClientRestriction(exam)
                            .onError(t -> log.error("Failed to update SEB Client restriction for Exam: {}", exam, t));
                }
            }

            // evict each Exam from cache and release the update-lock on DB
            for (final Exam exam : exams) {
                this.examSessionService
                        .flushCache(exam)
                        .onError(t -> log.error("Failed to flush Exam from cache: {}", exam, t));
            }

            // release the update-locks on involved exams
            for (final Long examId : examIdsFirstCheck) {
                this.examDAO.releaseLock(examId, updateId)
                        .onError(t -> log.error("Failed to release lock for Exam: {}", examId, t));
            }

            return examIdsFirstCheck;
        })
                .onError(t -> this.examDAO.forceUnlockAll(updateId));
    }

    @Override
    public <T> Result<T> processExamConfigurationMappingChange(
            final ExamConfigurationMap mapping,
            final Function<ExamConfigurationMap, Result<T>> changeAction) {

        return this.examDAO.byPK(mapping.examId)
                .map(exam -> {

                    // if the exam is not currently running just apply the action
                    if (exam.status != ExamStatus.RUNNING) {
                        return changeAction
                                .apply(mapping)
                                .getOrThrow();
                    }

                    // if the exam is running...
                    final String updateId = this.examUpdateHandler.createUpdateId();
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Process SEB Exam Configuration mapping update for: {} with update-id {}",
                                mapping,
                                updateId);
                    }

                    // check if there are no active client connections for this exam
                    checkActiveClientConnections(exam);

                    // lock the exam
                    this.examDAO
                            .placeLock(exam.id, updateId)
                            .getOrThrow();

                    // check again if there are no new active client connections in the meantime
                    checkActiveClientConnections(exam);

                    // apply the referenced change action. On error the change is rolled back and
                    // this processing returns immediately with the error
                    final T result = changeAction
                            .apply(mapping)
                            .onError(t -> log.error("Failed to save exam configuration: {}",
                                    mapping.configurationNodeId))
                            .getOrThrow();

                    // update seb client restriction if the feature is activated for the exam
                    if (this.examAdminService.isRestricted(exam).getOr(false)) {
                        this.examUpdateHandler
                                .getSEBRestrictionService()
                                .applySEBClientRestriction(exam)
                                .onError(t -> log.error(
                                        "Failed to update SEB Client restriction for Exam: {}",
                                        exam,
                                        t));
                    }

                    // flush the exam cache. If there was an error during flush, it is logged but this process goes on
                    // and the saved changes are not rolled back
                    this.examSessionService
                            .flushCache(exam)
                            .onError(t -> log.error("Failed to flush cache for exam: {}", exam));

                    // release the exam lock
                    this.examDAO
                            .releaseLock(exam.id, updateId)
                            .onError(t -> log.error("Failed to release lock for exam: {}", exam));

                    return result;
                })
                .onError(t -> this.examDAO.forceUnlock(mapping.examId));

    }

    private void checkActiveClientConnections(final Exam exam) {
        if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
            throw new APIMessage.APIMessageException(
                    ErrorMessage.INTEGRITY_VALIDATION,
                    "Integrity violation: There are currently active SEB Client connection.");
        }
    }

    @Override
    public void forceReleaseUpdateLocks(final Long configurationId) {

        log.warn(" **** Force release of update-locks for all exams that are related to configuration: {}",
                configurationId);

        try {
            final Configuration config = this.configurationDAO
                    .byPK(configurationId)
                    .getOrThrow();

            final Collection<Long> involvedExams = this.examConfigurationMapDAO
                    .getExamIdsForConfigNodeId(config.configurationNodeId)
                    .getOrThrow();

            final Collection<Long> examsIds = forceReleaseUpdateLocks(involvedExams)
                    .stream()
                    .map(Result::getOrThrow)
                    .collect(Collectors.toList());

            log.info("Successfully released update-locks for exams: {}", examsIds);

        } catch (final Exception e) {
            log.error("Failed to release update-locks for exam(s)", e);
        }

    }

    @Override
    public Collection<Result<Long>> forceReleaseUpdateLocks(final Collection<Long> examIds) {
        return examIds
                .stream()
                .map(this.examDAO::forceUnlock)
                .collect(Collectors.toList());
    }

    @Override
    public Result<Collection<Long>> checkRunningExamIntegrity(final Long configurationNodeId) {
        final Collection<Long> involvedExams = this.examConfigurationMapDAO
                .getExamIdsForConfigNodeId(configurationNodeId)
                .getOrThrow();

        if (involvedExams == null || involvedExams.isEmpty()) {
            return Result.of(Collections.emptyList());
        }

        // check if the configuration is attached to a running exams with active client connections
        final long activeConnections = involvedExams
                .stream()
                .flatMap(examId -> this.examSessionService.getConnectionData(examId, Objects::nonNull)
                        .getOrThrow()
                        .stream())
                .filter(ExamSessionService::isActiveConnection)
                .count();

        // if we have active SEB client connection on any running exam that
        // is involved within the specified configuration change, the change is denied
        if (activeConnections > 0) {
            return Result.ofError(new APIMessage.APIMessageException(
                    ErrorMessage.INTEGRITY_VALIDATION,
                    "Integrity violation: There are currently active SEB Client connection."));
        } else {
            // otherwise we return the involved identifiers exams to further processing
            return Result.of(involvedExams);
        }
    }

    private void checkIntegrityDoubleCheck(
            final Collection<Long> examIdsFirstCheck,
            final Collection<Long> examIdsSecondCheck) {

        if (examIdsFirstCheck.size() != examIdsSecondCheck.size()) {
            throw new IllegalStateException("Running Exam integrity check missmatch. examIdsFirstCheck: "
                    + examIdsFirstCheck + " examIdsSecondCheck: " + examIdsSecondCheck);
        }
    }

    private Collection<Result<Exam>> lockForUpdate(final Collection<Long> examIds, final String update) {
        return examIds.stream()
                .map(id -> this.examDAO.placeLock(id, update))
                .collect(Collectors.toList());
    }

}
