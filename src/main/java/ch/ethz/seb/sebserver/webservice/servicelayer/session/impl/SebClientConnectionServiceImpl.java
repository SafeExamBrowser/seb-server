/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SebClientConnectionService;
import io.micrometer.core.instrument.util.StringUtils;

public class SebClientConnectionServiceImpl implements SebClientConnectionService {

    private static final Logger log = LoggerFactory.getLogger(SebClientConnectionServiceImpl.class);

    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final EventHandlingStrategy eventHandlingStrategy;
    private final ClientConnectionDAO clientConnectionDAO;

    protected SebClientConnectionServiceImpl(
            final ExamSessionService examSessionService,
            final ExamSessionCacheService examSessionCacheService,
            final ClientConnectionDAO clientConnectionDAO,
            final Environment environment,
            final ApplicationContext applicationContext) {

        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionCacheService;
        this.clientConnectionDAO = clientConnectionDAO;

        String eventHandlingStrategyProperty =
                environment.getProperty(EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_CONFIG_PROPERTY_KEY);

        if (StringUtils.isBlank(eventHandlingStrategyProperty)) {
            eventHandlingStrategyProperty = EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_SINGLE_EVENT_STORE;
        }

        this.eventHandlingStrategy = applicationContext.getBean(
                eventHandlingStrategyProperty,
                EventHandlingStrategy.class);
    }

    @Override
    public Result<ClientConnection> createClientConnection(
            final Long institutionId,
            final String clientAddress,
            final Long examId) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("SEB client connection attempt, create ClientConnection for instituion {} and exam: {}",
                        institutionId,
                        examId);
            }

            // Integrity check: in case examId is provided is the specified exam running?
            if (examId != null && !this.examSessionService.isExamRunning(examId)) {
                examNotRunningException(examId);
            }

            // Create ClientConnection in status CONNECTION_REQUESTED for further processing
            final String connectionToken = createToken();
            final ClientConnection clientConnection = this.clientConnectionDAO.createNew(new ClientConnection(
                    null,
                    institutionId,
                    examId,
                    ClientConnection.ConnectionStatus.CONNECTION_REQUESTED,
                    connectionToken,
                    null,
                    clientAddress,
                    null))
                    .getOrThrow();

            if (log.isDebugEnabled()) {
                log.debug("New ClientConnection created: {}", clientConnection);
            }

            return clientConnection;
        });
    }

    @Override
    public Result<ClientConnection> establishClientConnection(
            final Long institutionId,
            final String connectionToken,
            final Long examId,
            final String clientAddress,
            final String userSessionId) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, establish ClientConnection for instituion {} and exam: {} and userSessionId: {}",
                        institutionId,
                        examId,
                        userSessionId);
            }

            // Integrity check: is the specified exam running?
            if (!this.examSessionService.isExamRunning(examId)) {
                examNotRunningException(examId);
            }

            final ClientConnection clientConnection = this.clientConnectionDAO
                    .byConnectionToken(institutionId, connectionToken)
                    .get(t -> {
                        // TODO: This indicates some irregularity on SEB-Client connection attempt.
                        //       Later we should handle this more accurately, and maybe indicate this to the monitoring board
                        //       For example; check if there is already a connection for the userIdentifier and
                        //       if true in which state it is.
                        log.debug("Unable to connect SEB-Client {} to exam {}",
                                clientAddress,
                                this.examSessionService.getRunningExam(examId).map(exam -> exam.name));
                        throw new IllegalStateException("Unable to connect SEB-Client to exam");
                    });

            // Integrity checks:
            if (!institutionId.equals(clientConnection.institutionId)) {
                log.error("Instituion integrity violation with institution: {} on clientConnection: {}",
                        institutionId,
                        clientConnection);
                throw new IllegalAccessError("Instituion integrity violation");
            }

            if (clientConnection.examId != null && !examId.equals(clientConnection.examId)) {
                log.error("Exam integrity violation with examId: {} on clientConnection: {}",
                        examId,
                        clientConnection);
                throw new IllegalAccessError("Exam integrity violation");
            }

            final ClientConnection updatedClientConnection = this.clientConnectionDAO.save(new ClientConnection(
                    clientConnection.id,
                    clientConnection.institutionId,
                    clientConnection.examId,
                    ClientConnection.ConnectionStatus.ESTABLISHED,
                    null,
                    userSessionId,
                    null,
                    null)).getOrThrow();

            return updatedClientConnection;
        });
    }

    @Override
    public Result<ClientConnection> closeConnection(final String connectionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyPing(final Long connectionId, final long timestamp, final int pingNumber) {
        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionId);

        if (activeClientConnection != null) {
            activeClientConnection.pingMappings
                    .stream()
                    .forEach(pingIndicator -> pingIndicator.notifyPing(timestamp, pingNumber));
        }
    }

    @Override
    public void notifyClientEvent(final ClientEvent event, final Long connectionId) {
        this.eventHandlingStrategy.accept(event);

        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionId);

        if (activeClientConnection != null) {
            activeClientConnection.getindicatorMapping(event.eventType)
                    .stream()
                    .forEach(indicator -> indicator.notifyValueChange(event));
        }
    }

    // TODO maybe we need a stronger connectionToken but for now a simple UUID is used
    private String createToken() {
        return UUID.randomUUID().toString();
    }

    private void examNotRunningException(final Long examId) {
        log.error("The exam {} is not running", examId);
        throw new IllegalStateException("The exam " + examId + " is not running");
    }

}
