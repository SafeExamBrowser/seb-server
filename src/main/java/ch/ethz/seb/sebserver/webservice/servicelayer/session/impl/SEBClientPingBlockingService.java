/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;

@Lazy
@Component
@WebServiceProfile
@ConditionalOnExpression("'${sebserver.webservice.ping.service.strategy}'.equals('BLOCKING')")
public class SEBClientPingBlockingService implements SEBClientPingService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientPingBlockingService.class);

    private final ExamSessionCacheService examSessionCacheService;
    private final SEBClientInstructionService sebClientInstructionService;

    public SEBClientPingBlockingService(
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientInstructionService sebClientInstructionService) {

        this.examSessionCacheService = examSessionCacheService;
        this.sebClientInstructionService = sebClientInstructionService;
    }

    @Override
    public PingServiceType pingServiceType() {
        return PingServiceType.BLOCKING;
    }

    @Override
    public String notifyPing(final String connectionToken, final String instructionConfirm) {
        if (connectionToken == null) {
            return null;
        }

        final ClientConnectionDataInternal connectionData = this.examSessionCacheService
                .getClientConnection(connectionToken);

        if (connectionData != null) {
            if (connectionData.clientConnection.status == ClientConnection.ConnectionStatus.DISABLED) {
                // SEBSERV-440 send quit instruction to SEB
                sebClientInstructionService.registerInstruction(
                        connectionData.clientConnection.examId,
                        ClientInstruction.InstructionType.SEB_QUIT,
                        Collections.emptyMap(),
                        connectionData.clientConnection.connectionToken,
                        false,
                        false
                );
            }

            connectionData.notifyPing(Utils.getMillisecondsNow());
        } else {
            log.error("Failed to get ClientConnectionDataInternal for: {}", connectionToken);
        }

        if (StringUtils.isNotBlank(instructionConfirm)) {
            this.sebClientInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        return this.sebClientInstructionService.getInstructionJSON(connectionToken);
    }

}
