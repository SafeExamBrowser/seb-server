/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;

@Lazy
@Component
@WebServiceProfile
@ConditionalOnExpression("'${sebserver.webservice.ping.service.strategy}'.equals('BATCH')")
public class SEBClientPingBatchService implements SEBClientPingService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientPingBatchService.class);

    private final ExamSessionCacheService examSessionCacheService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final ClientConnectionDAO clientConnectionDAO;

    private final Set<String> pingKeys = new HashSet<>();
    private final Map<String, String> pings = new ConcurrentHashMap<>();
    private final Map<String, String> instructions = new ConcurrentHashMap<>();

    public SEBClientPingBatchService(
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientInstructionService sebClientInstructionService,
            final ClientConnectionDAO clientConnectionDAO) {

        this.examSessionCacheService = examSessionCacheService;
        this.sebClientInstructionService = sebClientInstructionService;
        this.clientConnectionDAO = clientConnectionDAO;
    }

    @Scheduled(fixedDelayString = "${sebserver.webservice.api.exam.session.ping.batch.interval:500}")
    public void processPings() {
        if (this.pings.isEmpty()) {
            return;
        }

        final int size = this.pings.size();
        if (size > 1000) {
            log.warn("----> There are more then 1000 SEB client logs in the waiting queue: {}", size);
        }

        try {
            this.pingKeys.clear();
            this.pingKeys.addAll(this.pings.keySet());
            this.pingKeys.forEach(cid -> processPing(
                    cid,
                    this.pings.remove(cid),
                    Utils.getMillisecondsNow()));

        } catch (final Exception e) {
            log.error("Failed to process SEB pings from pingDataQueue: ", e);
        }
    }

    @Override
    public PingServiceType pingServiceType() {
        return PingServiceType.BATCH;
    }

    @Override
    public final String notifyPing(
            final String connectionToken,
            final String instructionConfirm) {

        final String instruction = this.instructions.remove(connectionToken);

        if (instructionConfirm != null) {

            this.pings.put(connectionToken, instructionConfirm);
            if (instruction != null && instruction.contains("\"instruction-confirm\":\"" + instructionConfirm + "\"")) {
                return null;
            }
        } else if (!this.pings.containsKey(connectionToken)) {
            this.pings.put(connectionToken, StringUtils.EMPTY);
        }

        return instruction;
    }

    private void processPing(
            final String connectionToken,
            final String instructionConfirm,
            final long timestamp) {

        if (connectionToken == null) {
            return;
        }

        final ClientConnectionDataInternal connectionData = this.examSessionCacheService
                .getClientConnection(connectionToken);

        if (connectionData != null) {
            if (connectionData.clientConnection.status == ClientConnection.ConnectionStatus.DISABLED) {
                // SEBSERV-440 send quit instruction to SEB
                sendQuitInstruction(connectionToken, connectionData.clientConnection.examId);
            }

            connectionData.notifyPing(timestamp);
        } else {
            log.warn("Failed to get ClientConnectionDataInternal probably due to finished Exam for: {}.", connectionToken);
            sendQuitInstruction(connectionToken,null);
        }

        if (StringUtils.isNotBlank(instructionConfirm)) {
            this.sebClientInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        if (this.instructions.containsKey(connectionToken)) {
            return;
        }

        final String instructionJSON = this.sebClientInstructionService.getInstructionJSON(connectionToken);
        if (instructionJSON != null) {
            this.instructions.put(connectionToken, instructionJSON);
        }
    }

    private void sendQuitInstruction(final String connectionToken, final Long examId) {

        Long _examId = examId;
        if (examId == null) {
            final Result<ClientConnection> clientConnectionResult = clientConnectionDAO
                    .byConnectionToken(connectionToken);

            if (clientConnectionResult.hasError()) {
                log.error(
                        "Failed to get examId for client connection token: {} error: {}",
                        connectionToken,
                        clientConnectionResult.getError().getMessage());
            }

            _examId = clientConnectionResult.get().examId;
        }

        if (_examId != null) {

            log.info("Send automated quit instruction to SEB for connection token: {}", connectionToken);

            // TODO add SEB event log that SEB Server has automatically send quit instruction to SEB

            sebClientInstructionService.registerInstruction(
                    _examId,
                    ClientInstruction.InstructionType.SEB_QUIT,
                    Collections.emptyMap(),
                    connectionToken,
                    false,
                    false
            );
        }
    }
}
