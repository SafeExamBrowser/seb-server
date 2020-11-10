/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PingHandlingStrategy;

@Lazy
@Component
@WebServiceProfile
public class SingleServerPingHandler implements PingHandlingStrategy {

    private final ExamSessionCacheService examSessionCacheService;

    protected SingleServerPingHandler(final ExamSessionCacheService examSessionCacheService) {
        this.examSessionCacheService = examSessionCacheService;
    }

    @Override
    public void notifyPing(final String connectionToken, final long timestamp, final int pingNumber) {
        // update ping indicators
        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getClientConnection(connectionToken);

        if (activeClientConnection != null) {
            activeClientConnection.notifyPing(timestamp, pingNumber);
        }
    }

    @Override
    public void initForConnection(final Long connectionId, final String connectionToken) {
        // nothing to do here
    }

}
