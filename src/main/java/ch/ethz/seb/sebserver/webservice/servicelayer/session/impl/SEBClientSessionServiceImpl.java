/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientVersionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.SEBClientEventBatchService.EventData;

@Lazy
@Service
@WebServiceProfile
public class SEBClientSessionServiceImpl implements SEBClientSessionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientSessionServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionService examSessionService;
    private final SEBClientEventBatchService sebClientEventBatchStore;
    private final SEBClientInstructionService sebInstructionService;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final InternalClientConnectionDataFactory internalClientConnectionDataFactory;
    private final SecurityKeyService securityKeyService;
    private final SEBClientVersionService sebClientVersionService;
    private final SEBClientPingService sebClientPingService;

    public SEBClientSessionServiceImpl(
            final ClientConnectionDAO clientConnectionDAO,
            final ExamSessionService examSessionService,
            final SEBClientEventBatchService sebClientEventBatchStore,
            final SEBClientInstructionService sebInstructionService,
            final ClientIndicatorFactory clientIndicatorFactory,
            final InternalClientConnectionDataFactory internalClientConnectionDataFactory,
            final SecurityKeyService securityKeyService,
            final SEBClientVersionService sebClientVersionService,
            final SEBClientPingService sebClientPingService) {

        this.clientConnectionDAO = clientConnectionDAO;
        this.examSessionService = examSessionService;
        this.sebClientEventBatchStore = sebClientEventBatchStore;
        this.sebInstructionService = sebInstructionService;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.internalClientConnectionDataFactory = internalClientConnectionDataFactory;
        this.securityKeyService = securityKeyService;
        this.sebClientVersionService = sebClientVersionService;
        this.sebClientPingService = sebClientPingService;
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
            final int pingNumber,
            final String instructionConfirm) {

        return this.sebClientPingService.notifyPing(connectionToken, instructionConfirm);
    }

    @Override
    public final void notifyClientEvent(final String connectionToken, final String jsonBody) {
        this.sebClientEventBatchStore.accept(connectionToken, jsonBody);
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

    private void missingPingUpdate(final ClientConnectionDataInternal connection) {
        if (connection.pingIndicator.changeOnIncident()) {

            final boolean missingPing = connection.getMissingPing();
            final long millisecondsNow = Utils.getMillisecondsNow();
            final String textValue = (missingPing) ? "Missing Client Ping" : "Client Ping Back To Normal";
            final double numValue = connection.pingIndicator.getValue();

            final EventData eventData = new EventData(
                    connection.getClientConnection().connectionToken,
                    millisecondsNow,
                    new ClientEvent(
                            null,
                            connection.getConnectionId(),
                            (missingPing) ? EventType.ERROR_LOG : EventType.INFO_LOG,
                            millisecondsNow,
                            millisecondsNow,
                            numValue,
                            textValue));

            // store missing-ping or ping-back event
            this.sebClientEventBatchStore.accept(eventData);

            // update indicators
            if (EventType.ERROR_LOG == eventData.event.eventType) {
                connection.getIndicatorMapping(EventType.ERROR_LOG)
                        .forEach(indicator -> indicator.notifyValueChange(textValue, numValue));
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
