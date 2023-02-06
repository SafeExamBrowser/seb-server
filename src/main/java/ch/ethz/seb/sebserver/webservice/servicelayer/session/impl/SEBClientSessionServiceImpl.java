/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientVersionService;

@Lazy
@Service
@WebServiceProfile
public class SEBClientSessionServiceImpl implements SEBClientSessionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final EventHandlingStrategy eventHandlingStrategy;
    private final SEBClientInstructionService sebInstructionService;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final InternalClientConnectionDataFactory internalClientConnectionDataFactory;
    private final SecurityKeyService securityKeyService;
    private final SEBClientVersionService sebClientVersionService;

    public SEBClientSessionServiceImpl(
            final ClientConnectionDAO clientConnectionDAO,
            final ExamSessionService examSessionService,
            final EventHandlingStrategyFactory eventHandlingStrategyFactory,
            final SEBClientInstructionService sebInstructionService,
            final ClientIndicatorFactory clientIndicatorFactory,
            final InternalClientConnectionDataFactory internalClientConnectionDataFactory,
            final SecurityKeyService securityKeyService,
            final SEBClientVersionService sebClientVersionService) {

        this.clientConnectionDAO = clientConnectionDAO;
        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionService.getExamSessionCacheService();
        this.eventHandlingStrategy = eventHandlingStrategyFactory.get();
        this.sebInstructionService = sebInstructionService;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.internalClientConnectionDataFactory = internalClientConnectionDataFactory;
        this.securityKeyService = securityKeyService;
        this.sebClientVersionService = sebClientVersionService;
    }

    @Override
    public void updatePingEvents() {
        try {

            this.examSessionService
                    .getExamDAO()
                    .allRunningExamIds()
                    .getOrThrow()
                    .stream()
                    .flatMap(examId -> this.clientConnectionDAO
                            .getAllActiveConnectionTokens(examId)
                            .getOr(Collections.emptyList())
                            .stream())
                    .map(this.examSessionService::getConnectionDataInternal)
                    .filter(Objects::nonNull)
                    .filter(connection -> connection.pingIndicator != null)
                    .forEach(this::missingPingUpdate);

        } catch (final Exception e) {
            log.error("Failed to update ping events: ", e);
        }
    }

    @Override
    public void updateASKGrants() {
        this.examSessionService
                .getExamDAO()
                .allRunningExamIds()
                .onSuccess(ids -> ids.stream().forEach(examId -> updateGrants(examId)))
                .onError(error -> log.error("Unexpected error while trying to updateASKGrants: ", error));
    }

    @Override
    public void cleanupInstructions() {
        this.sebInstructionService.cleanupInstructions();
    }

    @Override
    public String notifyPing(
            final String connectionToken,
            final long timestamp,
            final int pingNumber,
            final String instructionConfirm) {

        processPing(connectionToken, timestamp, pingNumber);

        if (instructionConfirm != null) {
            this.sebInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        return this.sebInstructionService.getInstructionJSON(connectionToken);
    }

    @Override
    public void notifyClientEvent(
            final String connectionToken,
            final ClientEvent event) {

        try {
            final ClientConnectionDataInternal activeClientConnection =
                    this.examSessionService.getConnectionDataInternal(connectionToken);

            if (activeClientConnection != null) {

                // store event
                this.eventHandlingStrategy.accept(ClientEvent.toRecord(
                        event,
                        activeClientConnection.getConnectionId()));

                // handle indicator update
                activeClientConnection
                        .getIndicatorMapping(event.eventType)
                        .forEach(indicator -> indicator.notifyValueChange(event));

            } else {
                log.warn("No active ClientConnection found for connectionToken: {}", connectionToken);
            }
        } catch (final Exception e) {
            log.error("Failed to process SEB client event: ", e);
        }
    }

    @Override
    public void confirmInstructionDone(final String connectionToken, final String instructionConfirm) {
        this.sebInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
    }

    @Override
    public Result<ClientConnectionData> getIndicatorValues(final ClientConnection clientConnection) {
        return Result.tryCatch(() -> new ClientConnectionData(
                clientConnection,
                this.clientIndicatorFactory.getIndicatorValues(clientConnection),
                this.internalClientConnectionDataFactory.getGroupIds(clientConnection)));
    }

    private void processPing(final String connectionToken, final long timestamp, final int pingNumber) {

        ClientConnectionDataInternal activeClientConnection = null;
        synchronized (ExamSessionCacheService.CLIENT_CONNECTION_CREATION_LOCK) {
            activeClientConnection = this.examSessionCacheService.getClientConnection(connectionToken);
        }

        if (activeClientConnection != null) {
            activeClientConnection.notifyPing(timestamp, pingNumber);
        }
    }

    private void missingPingUpdate(final ClientConnectionDataInternal connection) {
        if (connection.pingIndicator.changeOnIncident()) {

            final boolean missingPing = connection.getMissingPing();
            final long millisecondsNow = Utils.getMillisecondsNow();
            final ClientEventRecord clientEventRecord = new ClientEventRecord(
                    null,
                    connection.getConnectionId(),
                    (missingPing) ? EventType.ERROR_LOG.id : EventType.INFO_LOG.id,
                    millisecondsNow,
                    millisecondsNow,
                    new BigDecimal(connection.pingIndicator.getValue()),
                    (missingPing) ? "Missing Client Ping" : "Client Ping Back To Normal");

            // store event and and flush cache
            this.eventHandlingStrategy.accept(clientEventRecord);

            // update indicators
            if (clientEventRecord.getType() != null && EventType.ERROR_LOG.id == clientEventRecord.getType()) {
                connection.getIndicatorMapping(EventType.ERROR_LOG)
                        .forEach(indicator -> indicator.notifyValueChange(clientEventRecord));
            }
        }
    }

    private void updateGrants(final Long examId) {
        try {
            updateASKGrant(examId);
        } catch (final Exception e) {
            log.error("Failed to update ASK grant for exam: {}", examId, e);
        }
        try {
            updateAllowedSEBVersionGrant(examId);
        } catch (final Exception e) {
            log.error("Failed to update SEB client version grant for exam: {}", examId, e);
        }
    }

    private void updateASKGrant(final Long examId) {
        if (this.examSessionService
                .getRunningExam(examId)
                .map(exam -> exam.checkASK)
                .getOr(true)) {

            this.clientConnectionDAO
                    .getAllActiveNotGranted(examId)
                    .onError(error -> log.error(
                            "Failed to get none granted active client connections: ",
                            error))
                    .getOr(Collections.emptyList())
                    .forEach(this.securityKeyService::updateAppSignatureKeyGrant);
        }
    }

    private void updateAllowedSEBVersionGrant(final Long examId) {
        final List<AllowedSEBVersion> allowedSEBVersions = this.examSessionService
                .getRunningExam(examId)
                .map(exam -> exam.allowedSEBVersions)
                .getOr(Collections.emptyList());

        if (allowedSEBVersions != null && !allowedSEBVersions.isEmpty()) {
            this.clientConnectionDAO
                    .getAllActiveNoSEBVersionCheck(examId)
                    .onError(error -> log.error(
                            "Failed to get none SEB version checked active client connections: ",
                            error))
                    .getOr(Collections.emptyList())
                    .forEach(cc -> this.sebClientVersionService.checkVersionAndUpdateClientConnection(
                            cc,
                            allowedSEBVersions));
        }
    }
}
