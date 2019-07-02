/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionCacheService {

    public static final String CACHE_NAME_RUNNING_EXAM = "RUNNING_EXAM";
    public static final String CACHE_NAME_ACTIVE_CLIENT_CONNECTION = "ACTIVE_CLIENT_CONNECTION";
    public static final String CACHE_NAME_SEB_CONFIG_EXAM = "SEB_CONFIG_EXAM";

    private static final Logger log = LoggerFactory.getLogger(ExamSessionCacheService.class);

    private final ExamDAO examDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final SebExamConfigService sebExamConfigService;

    protected ExamSessionCacheService(
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ClientIndicatorFactory clientIndicatorFactory,
            final SebExamConfigService sebExamConfigService) {

        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.sebExamConfigService = sebExamConfigService;
    }

    @Cacheable(
            cacheNames = CACHE_NAME_RUNNING_EXAM,
            key = "#examId",
            unless = "#result == null")
    Exam getRunningExam(final Long examId) {

        if (log.isDebugEnabled()) {
            log.debug("Verify running exam for id: {}" + examId);
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
            key = "#exam.id",
            condition = "#target.isRunning(#result)")
    Exam evict(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Conditional eviction of running Exam from cache: {}", isRunning(exam));
        }

        return exam;
    }

    boolean isRunning(final Exam exam) {
        if (exam == null) {
            return false;
        }
        return ((exam.startTime.isEqualNow() || exam.startTime.isBeforeNow()) &&
                exam.endTime.isAfterNow());
    }

    @Cacheable(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken",
            unless = "#result == null")
    ClientConnectionDataInternal getActiveClientConnection(final String connectionToken) {

        if (log.isDebugEnabled()) {
            log.debug("Verify ClientConnection for running exam for caching by connectionToken: ", connectionToken);
        }

        final Result<ClientConnection> byPK = this.clientConnectionDAO
                .byConnectionToken(connectionToken);

        if (byPK.hasError()) {
            log.error("Failed to find/load ClientConnection with connectionToken {}", connectionToken, byPK.getError());
            return null;
        }

        final ClientConnection clientConnection = byPK.get();

        // verify connection is established
        if (clientConnection.status != ConnectionStatus.ESTABLISHED) {
            log.error("Illegal state: ClientConnection is not in expected state; ESTABLISHED. ClientConnection: ",
                    clientConnection);
            return null;
        }

        // verify exam is running
        if (getRunningExam(clientConnection.examId) == null) {
            log.error("Exam for ClientConnection with id { is not currently running}", clientConnection.id);
            return null;
        }

        return new ClientConnectionDataInternal(
                clientConnection,
                this.clientIndicatorFactory.createFor(clientConnection));
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken")
    void evictClientConnection(final String connectionToken) {
        if (log.isDebugEnabled()) {
            log.debug("Eviction of ClientConnectionData from cache: {}", connectionToken);
        }
    }

    @Cacheable(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#examId",
            unless = "#result == null")
    InMemorySebConfig getDefaultSebConfigForExam(final Long examId) {
        final Exam runningExam = this.getRunningExam(examId);
        final PipedOutputStream pipOut = new PipedOutputStream();
        try {
            final Long configId = this.sebExamConfigService
                    .getDefaultConfigurationIdForExam(runningExam.id)
                    .getOrThrow();

            // TODO add header, zip and encrypt if needed

            final BufferedInputStream in = new BufferedInputStream(new PipedInputStream(pipOut));
            this.sebExamConfigService.exportPlainXML(pipOut, runningExam.institutionId, configId);

            final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, byteOut);

            return new InMemorySebConfig(configId, runningExam.id, byteOut.toByteArray());

        } catch (final IOException e) {
            log.error("Unexpected error while getting default exam configuration for running exam; {}", runningExam, e);
            return null;
        }
    }

    @CacheEvict(
            cacheNames = CACHE_NAME_SEB_CONFIG_EXAM,
            key = "#examId")
    void evictDefaultSebConfig(final Long examId) {
        if (log.isDebugEnabled()) {
            log.debug("Eviction of default SEB Configuration from cache for exam: {}", examId);
        }
    }

}
