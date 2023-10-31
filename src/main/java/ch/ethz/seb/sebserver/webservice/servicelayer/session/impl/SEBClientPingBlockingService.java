/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;

@Lazy
@Component
@WebServiceProfile
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

        final ClientConnectionDataInternal activeClientConnection = this.examSessionCacheService
                .getClientConnection(connectionToken);

        if (activeClientConnection != null) {
            activeClientConnection.notifyPing(Utils.getMillisecondsNow());
        } else {
            log.error("Failed to get ClientConnectionDataInternal for: {}", connectionToken);
        }

        if (instructionConfirm != StringUtils.EMPTY) {
            this.sebClientInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        return this.sebClientInstructionService.getInstructionJSON(connectionToken);
    }

}
