/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PingHandlingStrategy;

@Lazy
@Service
@WebServiceProfile
public class PingHandlingStrategyFactory {

    private final SingleServerPingHandler singleServerPingHandler;
    private final DistributedServerPingHandler distributedServerPingHandler;
    private final WebserviceInfo webserviceInfo;

    protected PingHandlingStrategyFactory(
            final SingleServerPingHandler singleServerPingHandler,
            final DistributedServerPingHandler distributedServerPingHandler,
            final WebserviceInfo webserviceInfo) {

        this.singleServerPingHandler = singleServerPingHandler;
        this.distributedServerPingHandler = distributedServerPingHandler;
        this.webserviceInfo = webserviceInfo;
    }

    public PingHandlingStrategy get() {
        if (this.webserviceInfo.isDistributed()) {
            return this.distributedServerPingHandler;
        } else {
            return this.singleServerPingHandler;
        }
    }
}
