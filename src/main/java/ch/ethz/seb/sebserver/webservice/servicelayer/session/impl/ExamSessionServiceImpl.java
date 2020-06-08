/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final IndicatorDAO indicatorDAO;
    private final ExamSessionCacheService examSessionCacheService;
    private final ExamDAO examDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final CacheManager cacheManager;
    private final LmsAPIService lmsAPIService;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ExamDAO examDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final IndicatorDAO indicatorDAO,
            final CacheManager cacheManager,
            final LmsAPIService lmsAPIService) {

        this.examSessionCacheService = examSessionCacheService;
        this.examDAO = examDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.cacheManager = cacheManager;
        this.indicatorDAO = indicatorDAO;
        this.lmsAPIService = lmsAPIService;
    }

    @Override
    public ExamDAO getExamDAO() {
        return this.examDAO;
    }

    @Override
    public ClientConnectionDAO getClientConnectionDAO() {
        return this.clientConnectionDAO;
    }

    @Override
    public ExamSessionCacheService getExamSessionCacheService() {
        return this.examSessionCacheService;
    }

    @Override
    public CacheManager getCacheManager() {
        return this.cacheManager;
    }

    @Override
    public Result<Collection<APIMessage>> checkRunningExamConsistency(final Long examId) {
        return Result.tryCatch(() -> {
            final Collection<APIMessage> result = new ArrayList<>();

            if (isExamRunning(examId)) {
                final Exam exam = getRunningExam(examId)
                        .getOrThrow();

                // check exam supporter
                if (exam.getSupporter().isEmpty()) {
                    result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SUPPORTER.of(exam.getModelId()));
                }

                // check SEB configuration
                this.examConfigurationMapDAO.getDefaultConfigurationNode(examId)
                        .get(t -> {
                            result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_CONFIG.of(exam.getModelId()));
                            return null;
                        });

                // check SEB restriction available and restricted
                // if SEB restriction is not available no consistency violation message is added
                final LmsSetup lmsSetup = this.lmsAPIService.getLmsSetup(exam.lmsSetupId)
                        .getOr(null);
                if (lmsSetup != null && lmsSetup.lmsType.features.contains(Features.SEB_RESTRICTION)) {
                    this.lmsAPIService.getLmsAPITemplate(exam.lmsSetupId)
                            .map(t -> {
                                if (t.testCourseRestrictionAPI().isOk()) {
                                    return t;
                                } else {
                                    throw new NoSEBRestrictionException();
                                }
                            })
                            .flatMap(t -> t.getSEBClientRestriction(exam))
                            .onError(error -> {
                                if (error instanceof NoSEBRestrictionException) {
                                    result.add(
                                            ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION
                                                    .of(exam.getModelId()));
                                } else {
                                    throw new RuntimeException("Unexpected error: ", error);
                                }
                            });
                }

                // check indicator exists
                if (this.indicatorDAO.allForExam(examId)
                        .getOrThrow()
                        .isEmpty()) {

                    result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_INDICATOR.of(exam.getModelId()));
                }
            }

            return result;
        });
    }

    @Override
    public boolean hasActiveSEBClientConnections(final Long examId) {
        if (examId == null || !this.isExamRunning(examId)) {
            return false;
        }

        return !this.getConnectionData(examId, ExamSessionService::isActiveConnection)
                .getOrThrow()
                .isEmpty();
    }

    @Override
    public boolean hasDefaultConfigurationAttached(final Long examId) {
        return !this.examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .hasError();
    }

    @Override
    public boolean isExamRunning(final Long examId) {
        return !getRunningExam(examId).hasError();
    }

    @Override
    public boolean isExamLocked(final Long examId) {
        final Result<Boolean> locked = this.examDAO.isLocked(examId);

        if (locked.hasError()) {
            log.error("Unexpected Error while trying to verify lock for Exam: {}", examId);
        }

        return locked.hasError() || BooleanUtils.toBoolean(locked.get());
    }

    @Override
    public Result<Exam> getRunningExam(final Long examId) {
        if (log.isTraceEnabled()) {
            log.trace("Running exam request for exam {}", examId);
        }

        updateExamCache(examId);
        final Exam exam = this.examSessionCacheService.getRunningExam(examId);

        if (this.examSessionCacheService.isRunning(exam)) {
            if (log.isTraceEnabled()) {
                log.trace("Exam {} is running and cached", examId);
            }

            return Result.of(exam);
        } else {
            if (exam != null) {
                flushCache(exam);
            }

            log.info("Exam {} is not currently running", examId);

            return Result.ofError(new NoSuchElementException(
                    "No currently running exam found for id: " + examId));
        }
    }

    @Override
    public Result<Collection<Exam>> getRunningExamsForInstitution(final Long institutionId) {
        return this.examDAO.allIdsOfInstitution(institutionId)
                .map(col -> col.stream()
                        .map(this::getRunningExam)
                        .filter(Result::hasValue)
                        .map(Result::get)
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<Exam>> getFilteredRunningExams(
            final FilterMap filterMap,
            final Predicate<Exam> predicate) {

        filterMap
                .putIfAbsent(Exam.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .putIfAbsent(Exam.FILTER_ATTR_STATUS, ExamStatus.RUNNING.name());

        return this.examDAO.allMatching(filterMap, predicate)
                .map(col -> col.stream()
                        .map(exam -> this.examSessionCacheService.getRunningExam(exam.id))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Override
    public void streamDefaultExamConfig(
            final String connectionToken,
            final OutputStream out) {

        if (log.isDebugEnabled()) {
            log.debug("SEB exam configuration download request, connectionToken: {}", connectionToken);
        }

        final ClientConnection connection = this.clientConnectionDAO
                .byConnectionToken(connectionToken)
                .getOrThrow();

        if (connection == null) {
            log.warn("SEB exam configuration download request, no active ClientConnection found for token: {}",
                    connectionToken);
            throw new AccessDeniedException("Illegal connection token. No active ClientConnection found for token");
        }

        // exam integrity check
        if (connection.examId == null || !isExamRunning(connection.examId)) {
            log.error("Missing exam identifier or requested exam is not running for connection: {}", connection);
            throw new IllegalStateException("Missing exam identifier or requested exam is not running");
        }

        if (log.isDebugEnabled()) {
            log.debug("Trying to get exam from InMemorySEBConfig");
        }

        final Exam exam = this.getRunningExam(connection.examId)
                .getOrThrow();

        final InMemorySEBConfig sebConfigForExam = this.examSessionCacheService
                .getDefaultSEBConfigForExam(exam);

        if (sebConfigForExam == null) {
            log.error("Failed to get and cache InMemorySEBConfig for connection: {}", connection);
            return;
        }

        try {

            if (log.isDebugEnabled()) {
                log.debug("SEB exam configuration download request, start writing SEB exam configuration");
            }

            out.write(sebConfigForExam.getData());

            if (log.isDebugEnabled()) {
                log.debug("SEB exam configuration download request, finished writing SEB exam configuration");
            }

        } catch (final IOException e) {
            log.error("SEB exam configuration download request, failed to write SEB exam configuration: ", e);
        }
    }

    @Override
    public Result<ClientConnectionData> getConnectionData(final String connectionToken) {
        return Result.tryCatch(() -> {
            final Cache cache = this.cacheManager.getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
            return cache.get(connectionToken, ClientConnectionData.class);
        });
    }

    @Override
    public Result<Collection<ClientConnectionData>> getConnectionData(
            final Long examId,
            final Predicate<ClientConnectionData> filter) {

        return Result.tryCatch(() -> this.clientConnectionDAO
                .getConnectionTokens(examId)
                .getOrThrow()
                .stream()
                .map(this.examSessionCacheService::getActiveClientConnection)
                .filter(filter)
                .collect(Collectors.toList()));
    }

    @Override
    public Result<Exam> updateExamCache(final Long examId) {
        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        if (exam == null) {
            return Result.ofEmpty();
        }

        final Boolean isUpToDate = this.examDAO.upToDate(examId, exam.lastUpdate)
                .onError(t -> log.error("Failed to verify if cached exam is up to date: {}", exam, t))
                .getOr(false);

        if (!BooleanUtils.toBoolean(isUpToDate)) {
            return flushCache(exam);
        } else {
            return Result.of(exam);
        }
    }

    @Override
    public Result<Exam> flushCache(final Exam exam) {
        return Result.tryCatch(() -> {
            this.examSessionCacheService.evict(exam);
            this.examSessionCacheService.evictDefaultSEBConfig(exam);
            this.clientConnectionDAO
                    .getConnectionTokens(exam.id)
                    .getOrElse(Collections::emptyList)
                    .forEach(token -> {
                        // evict client connection
                        this.examSessionCacheService.evictClientConnection(token);
                        // evict also cached ping record
                        this.examSessionCacheService.evictPingRecord(token);
                    });

            return exam;
        });
    }

}
