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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientConnectionMinMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientConnectionMinMapper.ClientConnectionMinRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.IndicatorDistributedRequestCache;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientConnectionMinMapper clientConnectionMinMapper;
    private final IndicatorDAO indicatorDAO;
    private final ExamSessionCacheService examSessionCacheService;
    private final ExamDAO examDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final CacheManager cacheManager;
    private final LmsAPIService lmsAPIService;
    private final IndicatorDistributedRequestCache indicatorDistributedRequestCache;
    private final boolean distributedSetup;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ClientConnectionMinMapper clientConnectionMinMapper,
            final ExamDAO examDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final IndicatorDAO indicatorDAO,
            final CacheManager cacheManager,
            final LmsAPIService lmsAPIService,
            final IndicatorDistributedRequestCache indicatorDistributedRequestCache,
            @Value("${sebserver.webservice.distributed:false}") final boolean distributedSetup) {

        this.examSessionCacheService = examSessionCacheService;
        this.clientConnectionMinMapper = clientConnectionMinMapper;
        this.examDAO = examDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.cacheManager = cacheManager;
        this.indicatorDAO = indicatorDAO;
        this.lmsAPIService = lmsAPIService;
        this.indicatorDistributedRequestCache = indicatorDistributedRequestCache;
        this.distributedSetup = distributedSetup;
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
        return this.lmsAPIService;
    }

    @Override
    public Result<Collection<APIMessage>> checkExamConsistency(final Long examId) {
        return Result.tryCatch(() -> {
            final Collection<APIMessage> result = new ArrayList<>();

            final Exam exam = this.examDAO.byPK(examId)
                    .getOrThrow();

            // check lms connection
            if (exam.status == ExamStatus.CORRUPT_NO_LMS_CONNECTION) {
                result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_LMS_CONNECTION.of(exam.getModelId()));
            }
            if (exam.status == ExamStatus.CORRUPT_INVALID_ID) {
                result.add(ErrorMessage.EXAM_CONSISTENCY_VALIDATION_INVALID_ID_REFERENCE.of(exam.getModelId()));
            }

            if (exam.status == ExamStatus.RUNNING) {
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
        // NOTE: we evict the exam from the cache (if present) to ensure user is seeing always the current state of the Exam
        return this.examDAO.allIdsOfInstitution(institutionId)
                .map(col -> col.stream()
                        .map(this.examSessionCacheService::evict)
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

        // NOTE: we evict the exam from the cache (if present) to ensure user is seeing always the current state of the Exam
        return this.examDAO.allMatching(filterMap, predicate)
                .map(col -> col.stream()
                        .map(exam -> {
                            this.examSessionCacheService.evict(exam);
                            return this.examSessionCacheService.getRunningExam(exam.id);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
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

        if (log.isDebugEnabled()) {
            log.debug("Trying to get exam from InMemorySEBConfig");
        }

        final InMemorySEBConfig sebConfigForExam = this.examSessionCacheService
                .getDefaultSEBConfigForExam(connection.examId, institutionId);

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

            final ClientConnectionDataInternal activeClientConnection = this.examSessionCacheService
                    .getClientConnection(connectionToken);
            if (activeClientConnection == null) {
                throw new NoSuchElementException("Client Connection with token: " + connectionToken);
            }

            if (this.distributedSetup) {

                final Boolean upToDate = this.clientConnectionDAO
                        .isUpToDate(activeClientConnection.clientConnection)
                        .getOr(false);
                if (!upToDate) {
                    this.examSessionCacheService.evictClientConnection(connectionToken);
                    return this.examSessionCacheService.getClientConnection(connectionToken);
                }
            }

            return activeClientConnection;

        });
    }

    @Override
    public Result<Collection<ClientConnectionData>> getConnectionData(
            final Long examId,
            final Predicate<ClientConnectionData> filter) {

        if (this.distributedSetup) {

            // if we run in distributed mode, we have to get the connection tokens of the exam
            // always from the persistent storage and update the client connection cache
            // before by remove out-dated client connection. This is done within the update_time
            // of the client connection record that is set on every update in the persistent
            // storage. So if the update_time of the cached client connection doesen't match the
            // update_time from persistent, we need to flush this particular client connection from the cache
            this.indicatorDistributedRequestCache.evictPingTimes(examId);
            return Result.tryCatch(() -> this.clientConnectionMinMapper.selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.examId,
                            SqlBuilder.isEqualTo(examId))
                    .build()
                    .execute()
                    .stream()
                    .map(this.distributedClientConnectionUpdateFunction(filter))
                    .filter(filter)
                    .collect(Collectors.toList()));

        } else {
            return Result.tryCatch(() -> this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrThrow()
                    .stream()
                    .map(this.examSessionCacheService::getClientConnection)
                    .filter(filter)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public Result<Collection<String>> getActiveConnectionTokens(final Long examId) {
        return this.clientConnectionDAO
                .getActiveConnctionTokens(examId);
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
            this.examSessionCacheService.evictDefaultSEBConfig(exam.id);
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

    private Function<ClientConnectionMinRecord, ClientConnectionDataInternal> distributedClientConnectionUpdateFunction(
            final Predicate<ClientConnectionData> filter) {

        return cd -> {
            ClientConnectionDataInternal clientConnection = this.examSessionCacheService
                    .getClientConnection(cd.connection_token);

            if (filter.test(clientConnection)) {
                if (cd.update_time != null &&
                        !cd.update_time.equals(clientConnection.clientConnection.updateTime)) {

                    this.examSessionCacheService.evictClientConnection(cd.connection_token);
                    clientConnection = this.examSessionCacheService
                            .getClientConnection(cd.connection_token);
                }
            }
            return clientConnection;
        };
    }

}
