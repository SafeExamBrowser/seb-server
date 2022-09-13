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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamResetEvent;
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
    private final ClientGroupDAO clientGroupDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final CacheManager cacheManager;
    private final SEBRestrictionService sebRestrictionService;
    private final boolean checkExamSupporter;
    private final boolean distributedSetup;
    private final long distributedConnectionUpdate;

    private long lastConnectionTokenCacheUpdate = 0;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ExamDAO examDAO,
            final ClientGroupDAO clientGroupDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final IndicatorDAO indicatorDAO,
            final CacheManager cacheManager,
            final SEBRestrictionService sebRestrictionService,
            @Value("${sebserver.webservice.exam.check.supporter:false}") final boolean checkExamSupporter,
            @Value("${sebserver.webservice.distributed:false}") final boolean distributedSetup,
            @Value("${sebserver.webservice.distributed.connectionUpdate:2000}") final long distributedConnectionUpdate) {

        this.examSessionCacheService = examSessionCacheService;
        this.examDAO = examDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.cacheManager = cacheManager;
        this.indicatorDAO = indicatorDAO;
        this.sebRestrictionService = sebRestrictionService;
        this.checkExamSupporter = checkExamSupporter;
        this.distributedSetup = distributedSetup;
        this.distributedConnectionUpdate = distributedConnectionUpdate;
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
    public LmsAPIService getLmsAPIService() {
        return this.sebRestrictionService.getLmsAPIService();
    }

    @Override
    public Result<Collection<APIMessage>> checkExamConsistency(final Long examId) {
        return Result.tryCatch(() -> {
            final Collection<APIMessage> result = new ArrayList<>();

            final Exam exam = this.examDAO
                    .byPK(examId)
                    .getOrThrow();

            // check lms connection
            if (!exam.isLmsAvailable()) {
                result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_LMS_CONNECTION.of(exam.getModelId()));
            }

            if (exam.status == ExamStatus.RUNNING) {
                // check exam supporter
                if (this.checkExamSupporter && exam.getSupporter().isEmpty()) {
                    result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SUPPORTER.of(exam.getModelId()));
                }

                // check SEB configuration
                this.examConfigurationMapDAO.getDefaultConfigurationNode(examId)
                        .get(t -> {
                            result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_CONFIG.of(exam.getModelId()));
                            return null;
                        });

                if (!this.sebRestrictionService.checkSebRestrictionSet(exam)) {
                    result.add(
                            ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION
                                    .of(exam.getModelId()));
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
    public synchronized Result<Exam> getRunningExam(final Long examId) {

        if (log.isTraceEnabled()) {
            log.trace("Running exam request for exam {}", examId);
        }

        if (this.distributedSetup) {
            updateExamCache(examId);
        }

        final Exam exam = this.examSessionCacheService.getRunningExam(examId);

        if (this.examSessionCacheService.isRunning(exam)) {
            if (log.isTraceEnabled()) {
                log.trace("Exam {} is running and cached", examId);
            }

            return Result.of(exam);
        } else {
            if (exam != null) {
                log.info("Exam {} is not running anymore. Flush caches", exam);
                flushCache(exam);
            }

            log.info("Exam {} is not currently running", examId);

            return Result.ofError(new NoSuchElementException(
                    "No currently running exam found for id: " + examId));
        }
    }

    @Override
    public Result<Collection<Exam>> getRunningExamsForInstitution(final Long institutionId) {
        return this.examDAO.allIdsOfRunning(institutionId)
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
                        .map(exam -> {
                            final Exam runningExam = this.examSessionCacheService.getRunningExam(exam.id);
                            if (runningExam == null) {
                                return null;
                            }
                            if (!isUpToDate(exam, runningExam)) {
                                // If the cached exam-quiz data differs from the one of the currently loaded exam, cache is updated
                                this.examSessionCacheService.evict(exam);
                                return this.examSessionCacheService.getRunningExam(exam.id);
                            } else {
                                return runningExam;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<Exam>> getFilteredFinishedExams(
            final FilterMap filterMap,
            final Predicate<Exam> predicate) {

        filterMap.putIfAbsent(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING);

        return this.examDAO.getExamsForStatus(
                filterMap,
                predicate,
                ExamStatus.FINISHED,
                ExamStatus.ARCHIVED);
    }

    @Override
    public void streamDefaultExamConfig(
            final Long institutionId,
            final String connectionToken,
            final OutputStream out) {

        if (log.isDebugEnabled()) {
            log.debug("SEB exam configuration download request, connectionToken: {}", connectionToken);
        }

        final ClientConnectionData clientConnectionData = this.getConnectionData(connectionToken)
                .getOrThrow();

        if (clientConnectionData == null || clientConnectionData.clientConnection == null) {
            log.warn("SEB exam configuration download request, no active ClientConnection found for token: {}",
                    connectionToken);
            throw new AccessDeniedException("Illegal connection token. No active ClientConnection found for token");
        }

        final ClientConnection connection = clientConnectionData.clientConnection;

        // exam integrity check
        if (connection.examId == null || !isExamRunning(connection.examId)) {
            log.error("Missing exam identifier or requested exam is not running for connection: {}", connection);
            throw new IllegalStateException("Missing exam identifier or requested exam is not running");
        }

        if (log.isTraceEnabled()) {
            log.trace("Trying to get exam from InMemorySEBConfig");
        }

        InMemorySEBConfig sebConfigForExam = this.examSessionCacheService
                .getDefaultSEBConfigForExam(connection.examId, institutionId);

        if (sebConfigForExam == null) {
            log.error("Failed to get and cache InMemorySEBConfig for connection: {}", connection);
            return;
        }

        // for distributed setups check if cached config is still up to date. Flush and reload if not.
        if (this.distributedSetup && !this.examSessionCacheService.isUpToDate(sebConfigForExam)) {

            if (log.isDebugEnabled()) {
                log.debug("Detected new version of exam configuration for exam {} ...flush cache", connection.examId);
            }

            this.examSessionCacheService.evictDefaultSEBConfig(connection.examId);
            sebConfigForExam = this.examSessionCacheService
                    .getDefaultSEBConfigForExam(connection.examId, institutionId);
        }

        if (sebConfigForExam == null) {
            log.error("Failed to get and cache InMemorySEBConfig for connection: {}", connection);
            return;
        }

        try {

            if (log.isTraceEnabled()) {
                log.trace("SEB exam configuration download request, start writing SEB exam configuration");
            }

            out.write(sebConfigForExam.getData());

            if (log.isTraceEnabled()) {
                log.trace("SEB exam configuration download request, finished writing SEB exam configuration");
            }

        } catch (final IOException e) {
            log.error("SEB exam configuration download request, failed to write SEB exam configuration: ", e);
        }
    }

    @Override
    public ClientConnectionDataInternal getConnectionDataInternal(final String connectionToken) {
        synchronized (ExamSessionCacheService.CLIENT_CONECTION_CREATION_LOCK) {
            return this.examSessionCacheService.getClientConnection(connectionToken);
        }
    }

    @Override
    public Result<ClientConnectionData> getConnectionData(final String connectionToken) {

        return Result.tryCatch(() -> {

            final ClientConnectionDataInternal activeClientConnection =
                    getConnectionDataInternal(connectionToken);

            if (activeClientConnection == null) {
                throw new NoSuchElementException("Client Connection with token: " + connectionToken);
            }

            return activeClientConnection;

        });
    }

    @Override
    public Result<Collection<ClientConnectionData>> getConnectionData(
            final Long examId,
            final Predicate<ClientConnectionData> filter) {

        return Result.tryCatch(() -> {

            updateClientConnections(examId);

            return this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrThrow()
                    .stream()
                    .map(token -> getConnectionData(token).getOr(null))
                    .filter(Objects::nonNull)
                    .filter(filter)
                    .collect(Collectors.toList());

        });
    }

    @Override
    public Result<MonitoringSEBConnectionData> getMonitoringSEBConnectionsData(
            final Long examId,
            final Predicate<ClientConnectionData> filter) {

        return Result.tryCatch(() -> {

            // needed to store connection numbers per status
            final int[] statusMapping = new int[ConnectionStatus.values().length];
            for (int i = 0; i < statusMapping.length; i++) {
                statusMapping[i] = 0;
            }
            // needed to store connection numbers per client group too
            final Collection<ClientGroup> groups = this.clientGroupDAO.allForExam(examId).getOr(null);
            final Map<Long, Integer> clientGroupMapping = (groups != null && !groups.isEmpty())
                    ? new HashMap<>()
                    : null;

            updateClientConnections(examId);

            final List<ClientConnectionData> filteredConnections = this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrThrow()
                    .stream()
                    .map(token -> getConnectionDataInternal(token))
                    .filter(Objects::nonNull)
                    .map(c -> {
                        statusMapping[c.clientConnection.status.code]++;
                        processClientGroupMapping(c.groups, clientGroupMapping);
                        return c;
                    })
                    .filter(filter)
                    .collect(Collectors.toList());

            return new MonitoringSEBConnectionData(
                    examId,
                    filteredConnections,
                    statusMapping,
                    clientGroupMapping);
        });
    }

    @Override
    public Result<Collection<String>> getActiveConnectionTokens(final Long examId) {
        return this.clientConnectionDAO
                .getActiveConnectionTokens(examId);
    }

    @Override
    public Result<Collection<String>> getAllActiveConnectionTokens(final Long examId) {
        return this.clientConnectionDAO
                .getAllActiveConnectionTokens(examId);
    }

    @EventListener
    public void notifyExamRest(final ExamResetEvent event) {
        log.info("ExamResetEvent received, process exam session cleanup...");

        try {
            if (!isExamRunning(event.exam.id)) {
                this.flushCache(event.exam);
                if (this.distributedSetup) {
                    this.clientConnectionDAO
                            .deleteClientIndicatorValues(event.exam)
                            .getOrThrow();
                }
            }
        } catch (final Exception e) {
            log.error("Failed to cleanup on reset exam: {}", event.exam, e);
        }
    }

    @EventListener
    public void notifyExamFinished(final ExamFinishedEvent event) {

        log.info("ExamFinishedEvent received, process exam session cleanup...");

        try {
            if (!isExamRunning(event.exam.id)) {
                this.flushCache(event.exam);
                if (this.distributedSetup) {
                    this.clientConnectionDAO
                            .deleteClientIndicatorValues(event.exam)
                            .getOrThrow();
                }
            }
        } catch (final Exception e) {
            log.error("Failed to cleanup on finished exam: {}", event.exam, e);
        }
    }

    @Override
    public Result<Exam> updateExamCache(final Long examId) {

        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        if (exam == null) {
            return Result.ofEmpty();
        }

        final Boolean isUpToDate = this.examDAO.upToDate(exam)
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

        if (log.isDebugEnabled()) {
            log.debug("Flush monitoring session caches for exam: {}", exam);
        }

        return Result.tryCatch(() -> {
            this.examSessionCacheService.evict(exam);
            this.examSessionCacheService.evictDefaultSEBConfig(exam.id);
            this.clientConnectionDAO
                    .getConnectionTokens(exam.id)
                    .getOrElse(Collections::emptyList)
                    .forEach(token -> {
                        // evict client connection
                        this.examSessionCacheService.evictClientConnection(token);
                    });

            return exam;
        });
    }

    // If we are in a distributed setup the active connection token cache get flushed
    // in specified time interval. This allows caching over multiple monitoring requests but
    // ensure an update every now and then for new incoming connections
    private void updateClientConnections(final Long examId) {
        try {
            final long currentTimeMillis = System.currentTimeMillis();
            if (this.distributedSetup &&
                    currentTimeMillis - this.lastConnectionTokenCacheUpdate > this.distributedConnectionUpdate) {

                // go trough all client connection and update the ones that not up to date
                this.clientConnectionDAO.evictConnectionTokenCache(examId);

                final Set<Long> timestamps = this.clientConnectionDAO
                        .getConnectionTokens(examId)
                        .getOrThrow()
                        .stream()
                        .map(this::getConnectionDataInternal)
                        .filter(Objects::nonNull)
                        .map(cc -> cc.getClientConnection().updateTime)
                        .collect(Collectors.toSet());

                this.clientConnectionDAO.getClientConnectionsOutOfSyc(examId, timestamps)
                        .getOrElse(() -> Collections.emptySet())
                        .stream()
                        .forEach(this.examSessionCacheService::evictClientConnection);

                this.lastConnectionTokenCacheUpdate = currentTimeMillis;
            }
        } catch (final Exception e) {
            log.error("Unexpected error while trying to update client connections: ", e);
        }
    }

    private boolean isUpToDate(final Exam exam, final Exam runningExam) {
        return Objects.equals(exam.lastModified, runningExam.lastModified)
                && Objects.equals(exam.startTime, runningExam.startTime)
                && Objects.equals(exam.endTime, runningExam.endTime)
                && Objects.equals(exam.name, runningExam.name);
    }

    private void processClientGroupMapping(final Set<Long> groups, final Map<Long, Integer> clientGroupMapping) {
        if (groups == null || clientGroupMapping == null) {
            return;
        }

        groups.forEach(id -> {
            if (clientGroupMapping.containsKey(id)) {
                clientGroupMapping.put(id, clientGroupMapping.get(id) + 1);
            } else {
                clientGroupMapping.put(id, 1);
            }
        });
    }

}
