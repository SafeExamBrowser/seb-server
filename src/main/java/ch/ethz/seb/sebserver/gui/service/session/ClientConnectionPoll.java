/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

public class ClientConnectionPoll implements Consumer<ServerPushContext> {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionPoll.class);

    private final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder;
    private final ClientConnectionTable clientConnectionTable;
    private final long pollInterval;

    public ClientConnectionPoll(
            final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder,
            final ClientConnectionTable clientConnectionTable,
            final long pollInterval) {

        this.restCallBuilder = restCallBuilder;
        this.clientConnectionTable = clientConnectionTable;
        this.pollInterval = pollInterval;
    }

    @Override
    public void accept(final ServerPushContext pushContext) {
        try {
            Thread.sleep(this.pollInterval);
        } catch (final Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("unexpected error while sleep: ", e);
            }
        }

        this.clientConnectionTable.updateValues(this.restCallBuilder
                .call()
                .get(t -> {
                    log.error("Error poll connection data: ", t);
                    return Collections.emptyList();
                }));
    }

}
