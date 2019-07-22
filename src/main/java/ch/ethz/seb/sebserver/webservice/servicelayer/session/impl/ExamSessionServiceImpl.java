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
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionCacheService examSessionCacheService;
    private final ExamDAO examDAO;
    private final CacheManager cacheManager;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final CacheManager cacheManager) {

        this.examSessionCacheService = examSessionCacheService;
        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean isExamRunning(final Long examId) {
        return !getRunningExam(examId).hasError();
    }

    @Override
    public Result<Exam> getRunningExam(final Long examId) {
        if (log.isDebugEnabled()) {
            log.debug("Running exam request for exam {}", examId);
        }

        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        if (this.examSessionCacheService.isRunning(exam)) {
            if (log.isDebugEnabled()) {
                log.debug("Exam {} is running and cached", examId);
            }

            return Result.of(exam);
        } else {
            if (exam != null) {
                flushCache(exam);
            }

            log.warn("Exam {} is not currently running", examId);

            return Result.ofError(new NoSuchElementException(
                    "No currenlty running exam found for id: " + examId));
        }
    }

    @Override
    public Result<Collection<Exam>> getRunningExamsForInstitution(final Long institutionId) {
        return this.examDAO.allIdsOfInstituion(institutionId)
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

        return this.examDAO.allMatching(filterMap, predicate)
                .map(col -> col.stream()
                        .map(exam -> this.examSessionCacheService.getRunningExam(exam.id))
                        .filter(exam -> exam != null)
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
            log.error("Missing exam identifer or requested exam is not running for connection: {}", connection);
            throw new IllegalStateException("Missing exam identider or requested exam is not running");
        }

        if (log.isDebugEnabled()) {
            log.debug("Trying to get exam from InMemorySebConfig");
        }

        final InMemorySebConfig sebConfigForExam = this.examSessionCacheService
                .getDefaultSebConfigForExam(connection.examId);

        if (sebConfigForExam == null) {
            log.error("Failed to get and cache InMemorySebConfig for connection: {}", connection);
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
    public Result<Collection<ClientConnectionData>> getConnectionData(final Long examId) {
        return Result.tryCatch(() -> {
            final Cache cache = this.cacheManager.getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
            return this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrThrow()
                    .stream()
                    .map(token -> cache.get(token, ClientConnectionData.class))
                    .filter(data -> data != null)
                    .collect(Collectors.toList());
        });
    }

    private void flushCache(final Exam exam) {
        try {
            this.examSessionCacheService.evict(exam);
            this.examSessionCacheService.evictDefaultSebConfig(exam.id);
            this.clientConnectionDAO
                    .getConnectionTokens(exam.id)
                    .getOrElse(() -> Collections.emptyList())
                    .forEach(token -> {
                        // evict client connection
                        this.examSessionCacheService.evictClientConnection(token);
                        // evict also cached ping record
                        this.examSessionCacheService.evictPingRecord(token);
                    });
        } catch (Exception e) {
            log.error("Unexpected error while trying to flush cache for exam: ", exam, e);
        }
    }

}
