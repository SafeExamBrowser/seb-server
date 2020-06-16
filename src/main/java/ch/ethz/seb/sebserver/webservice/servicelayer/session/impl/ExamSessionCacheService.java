/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.io.ByteArrayOutputStream;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;

/** Handles caching for exam session and defines caching for following object:
 *
 * - Running exams (examId -> Exam)
 * - in-memory exam configuration (examId -> InMemorySEBConfig)
 * - active client connections (connectionToken -> ClientConnectionDataInternal)
 * - client event records for last ping store (connectionToken -> ReusableClientEventRecord) */
@Lazy
@Service
@WebServiceProfile
public class ExamSessionCacheService {

    public static final String CACHE_NAME_RUNNING_EXAM = "RUNNING_EXAM";
    public static final String CACHE_NAME_ACTIVE_CLIENT_CONNECTION = "ACTIVE_CLIENT_CONNECTION";
    public static final String CACHE_NAME_SEB_CONFIG_EXAM = "SEB_CONFIG_EXAM";
    public static final String CACHE_NAME_PING_RECORD = "CACHE_NAME_PING_RECORD";

    private static final Logger log = LoggerFactory.getLogger(ExamSessionCacheService.class);

    private final ExamDAO examDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final ExamConfigService sebExamConfigService;
    private final ClientEventRecordMapper clientEventRecordMapper;
    private final ExamUpdateHandler examUpdateHandler;

    protected ExamSessionCacheService(
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ClientIndicatorFactory clientIndicatorFactory,
            final ExamConfigService sebExamConfigService,
            final ClientEventRecordMapper clientEventRecordMapper,
            final ExamUpdateHandler examUpdateHandler) {

        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.sebExamConfigService = sebExamConfigService;
        this.clientEventRecordMapper = clientEventRecordMapper;
        this.examUpdateHandler = examUpdateHandler;
    }

    @Cacheable(
            cacheNames = CACHE_NAME_RUNNING_EXAM,
            key = "#examId",
            unless = "#result == null")
    public Exam getRunningExam(final Long examId) {

        if (log.isDebugEnabled()) {
            log.debug("Verify running exam for id: {}", examId);
        }

        final Result<Exam> byPK = this.examDAO.byPK(examId);
        if (byPK.hasError()) {
            log.error("Failed to find/load Exam with id {}", examId, byPK.getError());
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

        if (log.isDebugEnabled()) {
            log.debug("Conditional eviction of running Exam from cache: {}", isRunning(exam));
        }

        return exam;
    }

    public boolean isRunning(final Exam exam) {
        if (exam == null) {
            return false;
        }

        switch (exam.status) {
            case RUNNING: {
                return true;
            }
            case UP_COMING: {
                return this.examUpdateHandler.updateRunning(exam.id)
                        .map(e -> e.status == ExamStatus.RUNNING)
                        .getOr(false);
            }
            default: {
                return false;
            }
        }
    }

    @Cacheable(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken",
            unless = "#result == null")
    public ClientConnectionDataInternal getActiveClientConnection(final String connectionToken) {

        if (log.isDebugEnabled()) {
            log.debug("Verify ClientConnection for running exam for caching by connectionToken: {}", connectionToken);
        }

        final ClientConnection clientConnection = getClientConnectionByToken(connectionToken);
        if (clientConnection == null) {
            return null;
        } else {
            return new ClientConnectionDataInternal(
                    clientConnection,
                    this.clientIndicatorFactory.createFor(clientConnection));
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken")
    public void evictClientConnection(final String connectionToken) {
        if (log.isDebugEnabled()) {
            log.debug("Eviction of ClientConnectionData from cache: {}", connectionToken);
        }
    }

    @Cacheable(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#exam.id",
            sync = true)
    public InMemorySEBConfig getDefaultSEBConfigForExam(final Exam exam) {
        try {

            final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            final Long configId = this.sebExamConfigService.exportForExam(
                    byteOut,
                    exam.institutionId,
                    exam.id);

            return new InMemorySEBConfig(configId, exam.id, byteOut.toByteArray());

        } catch (final Exception e) {
            log.error("Unexpected error while getting default exam configuration for running exam; {}", exam, e);
            throw e;
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#exam.id")
    public void evictDefaultSEBConfig(final Exam exam) {
        if (log.isDebugEnabled()) {
            log.debug("Eviction of default SEB Configuration from cache for exam: {}", exam.id);
        }
    }

    @Cacheable(
            cacheNames = CACHE_NAME_PING_RECORD,
            key = "#connectionToken",
            unless = "#result == null")
    @Transactional
    public ClientEventRecord getPingRecord(final String connectionToken) {
        if (log.isDebugEnabled()) {
            log.debug("Verify ClientConnection for ping record to cache by connectionToken: {}", connectionToken);
        }

        final ClientConnection clientConnection = getClientConnectionByToken(connectionToken);
        if (clientConnection == null) {
            return null;
        } else {
            try {
                return this.clientEventRecordMapper.selectByExample()
                        .where(
                                ClientEventRecordDynamicSqlSupport.clientConnectionId,
                                SqlBuilder.isEqualTo(clientConnection.getId()))
                        .and(
                                ClientEventRecordDynamicSqlSupport.type,
                                SqlBuilder.isEqualTo(EventType.LAST_PING.id))
                        .build()
                        .execute()
                        .stream()
                        .collect(Utils.toSingleton());

            } catch (final Exception e) {
                log.error("Unexpected error: ", e);
                return null;
            }
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_PING_RECORD,
            key = "#connectionToken")
    public void evictPingRecord(final String connectionToken) {
        if (log.isDebugEnabled()) {
            log.debug("Eviction of ReusableClientEventRecord from cache for connection token: {}", connectionToken);
        }
    }

    private ClientConnection getClientConnectionByToken(final String connectionToken) {
        final Result<ClientConnection> byPK = this.clientConnectionDAO
                .byConnectionToken(connectionToken);

        if (byPK.hasError()) {
            log.error("Failed to find/load ClientConnection with connectionToken {}", connectionToken, byPK.getError());
            return null;
        }
        return byPK.get();
    }

}
