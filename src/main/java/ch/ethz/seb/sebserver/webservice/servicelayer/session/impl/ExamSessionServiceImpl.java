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
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionCacheService examSessionCacheService;
    private final ExamDAO examDAO;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO) {

        this.examSessionCacheService = examSessionCacheService;
        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
    }

    @Override
    public boolean isExamRunning(final Long examId) {
        return this.examSessionCacheService
                .isRunning(this.examSessionCacheService.getRunningExam(examId));
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
                this.examSessionCacheService.evict(exam);
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
                        .map(examId -> this.examSessionCacheService.getRunningExam(examId))
                        .filter(exam -> exam != null)
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

        final ClientConnection connection = this.clientConnectionDAO
                .byConnectionToken(institutionId, connectionToken)
                .getOrThrow();

        if (connection == null || connection.status != ConnectionStatus.ESTABLISHED) {
            log.warn("SEB exam configuration download request, no active ClientConnection found for token: {}",
                    connectionToken);
            throw new AccessDeniedException("Illegal connection token. No active ClientConnection found for token");
        }

        if (log.isDebugEnabled()) {
            log.debug("SEB exam configuration download request: {}", connection);
            log.debug("Trying to get exam form InMemorySebConfig");
        }

        final InMemorySebConfig sebConfigForExam = this.examSessionCacheService
                .getDefaultSebConfigForExam(connection.examId);

        if (log.isDebugEnabled()) {
            if (sebConfigForExam == null) {
                log.debug("Failed to get and cache InMemorySebConfig for connection: {}", connection);
            }
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

}
