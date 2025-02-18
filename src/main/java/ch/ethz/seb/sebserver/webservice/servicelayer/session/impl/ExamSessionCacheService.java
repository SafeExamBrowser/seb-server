/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.session.ProctoringGroupMonitoringData;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;

/** Handles caching for exam session and defines caching for following object:
 * <p>
 * - Running exams (examId -> Exam)
 * - in-memory exam configuration (examId -> InMemorySEBConfig)
 * - active client connections (connectionToken -> ClientConnectionDataInternal)
 * - client event records for last ping store (connectionToken -> ReusableClientEventRecord) */
@Lazy
@Service
@WebServiceProfile
public class ExamSessionCacheService {

    public static final Object CLIENT_CONNECTION_CREATION_LOCK = new Object();

    public static final String CACHE_NAME_RUNNING_EXAM = "RUNNING_EXAM";
    public static final String CACHE_NAME_ACTIVE_CLIENT_CONNECTION = "ACTIVE_CLIENT_CONNECTION";
    public static final String CACHE_NAME_SEB_CONFIG_EXAM = "SEB_CONFIG_EXAM";
    public static final String CACHE_NAME_SCREEN_PROCTORING_GROUPS = "SCREEN_PROCTORING_GROUPS";

    private static final Logger log = LoggerFactory.getLogger(ExamSessionCacheService.class);

    private final ExamDAO examDAO;
    private final ClientGroupDAO clientGroupDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final InternalClientConnectionDataFactory internalClientConnectionDataFactory;
    private final ExamConfigService sebExamConfigService;
    private final ScreenProctoringGroupDAO screenProctoringGroupDAO;

    protected ExamSessionCacheService(
            final ExamDAO examDAO,
            final ClientGroupDAO clientGroupDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final InternalClientConnectionDataFactory internalClientConnectionDataFactory,
            final ExamConfigService sebExamConfigService,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ScreenProctoringGroupDAO screenProctoringGroupDAO) {

        this.examDAO = examDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.internalClientConnectionDataFactory = internalClientConnectionDataFactory;
        this.sebExamConfigService = sebExamConfigService;
        this.screenProctoringGroupDAO = screenProctoringGroupDAO;
    }

    @Cacheable(
            cacheNames = CACHE_NAME_RUNNING_EXAM,
            key = "#examId",
            unless = "#result == null")
    public synchronized Exam getRunningExam(final Long examId) {

        if (log.isDebugEnabled()) {
            log.debug("Verify running exam for id: {}", examId);
        }

        final Result<Exam> byPK = this.examDAO.byPK(examId);
        if (byPK.hasError()) {
            log.error("Failed to find/load Exam with id {} cause: {}", examId, byPK.getError().getMessage());
            return null;
        }

        final Exam exam = byPK.get();
        if (!isRunning(exam)) {
            return null;
        }

        return exam;
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_RUNNING_EXAM,
            key = "#exam.id")
    public Exam evict(final Exam exam) {

        if (log.isTraceEnabled()) {
            log.trace("Conditional eviction of running Exam from cache: {}", isRunning(exam));
        }

        this.clientGroupDAO.evictCacheForExam(exam.id);
        return exam;
    }

    public boolean isRunning(final Exam exam) {
        if (exam == null || !exam.active) {
            return false;
        }

        return exam.status == Exam.ExamStatus.RUNNING || exam.status == Exam.ExamStatus.TEST_RUN;
    }

    @Cacheable(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken",
            unless = "#result == null")
    public ClientConnectionDataInternal getClientConnection(final String connectionToken) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Verify ClientConnection for running exam for caching by connectionToken: {}",
                        connectionToken);
            }

            final ClientConnection clientConnection = getClientConnectionByToken(connectionToken);
            if (clientConnection == null || (clientConnection.examId != null && !examDAO.isRunning(clientConnection.examId))) {
                return null;
            } else {
                return this.internalClientConnectionDataFactory.createClientConnectionData(clientConnection);
            }
        } catch (final Exception e) {
            log.error("Failed to get client connection: ", e);
            return null;
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken")
    public void evictClientConnection(final String connectionToken) {
        if (log.isTraceEnabled()) {
            log.trace("Eviction of ClientConnectionData from cache: {}", connectionToken);
        }
    }
    
// TODO currently caching is not enabled because difficulty with distributed setup and size update task on master
//    @Cacheable(
//            cacheNames = CACHE_NAME_SCREEN_PROCTORING_GROUPS,
//            key = "#examId",
//            unless = "#result == null")
    public Result<Collection<ProctoringGroupMonitoringData>> getScreenProctoringGroups(final Long examId) {

        // TODO get it directly from new DAO method
        final Result<Collection<ProctoringGroupMonitoringData>> result = screenProctoringGroupDAO
                .getCollectingGroups(examId)
                .map(list -> (Collection<ProctoringGroupMonitoringData>) list
                        .stream()
                        .map(g -> new ProctoringGroupMonitoringData(g.uuid, g.name, g.size))
                        .toList())
                .onError(error -> log.error(
                        "Failed to screen proctoring groups for exam: {}, cause: {}",
                        examId,
                        error.getMessage()));

        if (result.hasError()) {
            return null;
        }

        return result;
    }

//    @CacheEvict(
//            cacheNames = CACHE_NAME_SCREEN_PROCTORING_GROUPS,
//            key = "#examId")
    public void evictScreenProctoringGroups(final Long examId) {
        if (log.isTraceEnabled()) {
            log.trace("Eviction of ScreenProctoringGroups from cache for exam: {}", examId);
        }
    }

    @Cacheable(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#examId",
            sync = true)
    public InMemorySEBConfig getDefaultSEBConfigForExam(final Long examId, final Long institutionId) {
        try {

            final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            final Long configId = this.sebExamConfigService.exportForExam(
                    byteOut,
                    institutionId,
                    examId);
            final Long followupId = this.sebExamConfigService
                    .getFollowupConfigurationId(configId)
                    .onError(error -> log.error("Failed to get follow-up id for config node: {}", configId, error))
                    .getOr(-1L);

            return new InMemorySEBConfig(configId, followupId, examId, byteOut.toByteArray());

        } catch (final Exception e) {
            log.error("Unexpected error while getting default exam configuration for running exam; {}", examId, e);
            throw e;
        }
    }

    public boolean isUpToDate(final InMemorySEBConfig inMemorySEBConfig) {
        try {
            final Long followupId = this.sebExamConfigService
                    .getFollowupConfigurationId(inMemorySEBConfig.configId)
                    .getOrThrow();

            return followupId.equals(inMemorySEBConfig.follwupId);
        } catch (final Exception e) {
            log.error("Failed to check if InMemorySEBConfig is up to date for: {}", inMemorySEBConfig);
            return true;
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#examId")
    public void evictDefaultSEBConfig(final Long examId) {
        if (log.isTraceEnabled()) {
            log.trace("Eviction of default SEB Configuration from cache for exam: {}", examId);
        }
    }

    private ClientConnection getClientConnectionByToken(final String connectionToken) {
        final Result<ClientConnection> result = this.clientConnectionDAO
                .byConnectionToken(connectionToken);

        if (result.hasError()) {
            log.error("Failed to find/load ClientConnection with connectionToken {}",
                    connectionToken,
                    result.getError());
            return null;
        }
        return result.get();
    }
    
}
