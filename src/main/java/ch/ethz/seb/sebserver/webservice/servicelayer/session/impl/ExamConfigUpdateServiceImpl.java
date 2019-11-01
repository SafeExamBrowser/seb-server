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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
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

    protected ExamConfigUpdateServiceImpl(
            final ExamDAO examDAO,
            final ConfigurationDAO configurationDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ExamSessionService examSessionService,
            final ExamUpdateHandler examUpdateHandler) {

        this.examDAO = examDAO;
        this.configurationDAO = configurationDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.examSessionService = examSessionService;
        this.examUpdateHandler = examUpdateHandler;
    }

    // processing:
    // check running exam integrity (No running exam with active SEB client-connection available)
    // if OK, create an update-id and for each exam, create an update-lock on DB (This also prevents new SEB client connection attempts during update)
    // check running exam integrity again after lock to ensure there where no SEB Client connection attempts in the meantime
    // store the new configuration values (into history) so that they take effect
    // generate the new Config Key and update the Config Key within the LMSSetup API for each exam (delete old Key and add new Key)
    // evict each Exam from cache and release the update-lock on DB
    @Override
    @Transactional
    public Result<Collection<Long>> processSEBExamConfigurationChange(final Long configurationNodeId) {

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
            final Collection<Long> updatedExams = updateLmsSebRestriction(exams)
                    .stream()
                    .map(Result::get)
                    .filter(Objects::nonNull)
                    .map(Exam::getId)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Successfully updated ConfigKey for Exams: {}", updatedExams);
            }

            // evict each Exam from cache and release the update-lock on DB
            final Collection<Long> evictedExams = evictFromCache(exams)
                    .stream()
                    .filter(Result::hasValue)
                    .map(Result::get)
                    .map(Exam::getId)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Successfully evicted Exams from cache: {}", evictedExams);
            }

            // release the update-locks on involved exams
            final Collection<Long> releasedLocks = releaseUpdateLocks(examIdsFirstCheck, updateId)
                    .stream()
                    .map(Result::getOrThrow)
                    .map(Exam::getId)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Successfully released update-locks on Exams: {}", releasedLocks);
            }

            return examIdsFirstCheck;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    public Result<Long> processSEBExamConfigurationAttachmentChange(final Long examId) {
        return this.examDAO.byPK(examId)
                .map(exam -> {
                    if (exam.status != ExamStatus.RUNNING) {
                        return examId;
                    }

                    // TODO Lock??
                    // TODO flush cache
                    // TODO update seb restriction if on
                    // TODO unlock?

                    return examId;
                });

    }

    @Override
    public void forceReleaseUpdateLocks(final Long configurationId) {

        log.warn(" **** Force release of update-locks for all exams that are related to configuration: {}",
                configurationId);

        try {
            final Configuration config = this.configurationDAO.byPK(configurationId)
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
        return examIds.stream()
                .map(this.examDAO::forceUnlock)
                .collect(Collectors.toList());
    }

    @Override
    public Result<Exam> applySebClientRestriction(final Exam exam) {
        return this.examUpdateHandler.applySebClientRestriction(exam)
                .onError(error -> log.error("Failed to apply SEB Client restriction for Exam: {}", exam, error));
    }

    @Override
    public Result<Exam> updateSebClientRestriction(final Exam exam) {
        return this.examUpdateHandler.updateSebClientRestriction(exam)
                .onError(error -> log.error("Failed to update SEB Client restriction for Exam: {}", exam, error));
    }

    @Override
    public Result<Exam> releaseSebClientRestriction(final Exam exam) {
        return this.examUpdateHandler.releaseSebClientRestriction(exam)
                .onError(error -> log.error("Failed to release SEB Client restriction for Exam: {}", exam, error));
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

    private Collection<Result<Exam>> releaseUpdateLocks(final Collection<Long> examIds, final String update) {
        return examIds.stream()
                .map(id -> this.examDAO.releaseLock(id, update))
                .collect(Collectors.toList());
    }

    private Collection<Result<Exam>> updateLmsSebRestriction(final Collection<Exam> exams) {
        return exams
                .stream()
                .filter(exam -> exam.getStatus() == ExamStatus.RUNNING)
                .map(this::updateSebClientRestriction)
                .collect(Collectors.toList());
    }

    private Collection<Result<Exam>> evictFromCache(final Collection<Exam> exams) {
        return exams
                .stream()
                .map(this.examSessionService::flushCache)
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
                .flatMap(examId -> {
                    return this.examSessionService.getConnectionData(examId)
                            .getOrThrow()
                            .stream();
                })
                .filter(ExamConfigUpdateServiceImpl::isActiveConnection)
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

    @Override
    public boolean hasActiveSebClientConnections(final Long examId) {
        if (examId == null || !this.examSessionService.isExamRunning(examId)) {
            return false;
        }

        return this.examSessionService.getConnectionData(examId)
                .getOrThrow()
                .stream()
                .filter(ExamConfigUpdateServiceImpl::isActiveConnection)
                .findFirst()
                .isPresent();
    }

    private static boolean isActiveConnection(final ClientConnectionData connection) {
        if (connection.clientConnection.status == ConnectionStatus.ESTABLISHED
                || connection.clientConnection.status == ConnectionStatus.AUTHENTICATED) {
            return true;
        }

        if (connection.clientConnection.status == ConnectionStatus.CONNECTION_REQUESTED) {
            final Long creationTime = connection.clientConnection.getCreationTime();
            final long millisecondsNow = Utils.getMillisecondsNow();
            if (millisecondsNow - creationTime < 30 * Constants.SECOND_IN_MILLIS) {
                return true;
            }
        }

        return false;
    }

}
