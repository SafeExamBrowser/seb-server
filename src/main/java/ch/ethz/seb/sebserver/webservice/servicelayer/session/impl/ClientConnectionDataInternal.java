/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public class ClientConnectionDataInternal extends ClientConnectionData {

    final Collection<AbstractPingIndicator> pingMappings;
    final EnumMap<EventType, Collection<ClientIndicator>> indicatorMapping;

    PingIntervalClientIndicator pingIndicator = null;

    protected ClientConnectionDataInternal(
            final ClientConnection clientConnection,
            final List<ClientIndicator> clientIndicators) {

        super(clientConnection, clientIndicators);

        this.indicatorMapping = new EnumMap<>(EventType.class);
        this.pingMappings = new ArrayList<>();
        for (final ClientIndicator clientIndicator : clientIndicators) {
            if (clientIndicator instanceof AbstractPingIndicator) {
                if (clientIndicator instanceof PingIntervalClientIndicator) {
                    this.pingIndicator = (PingIntervalClientIndicator) clientIndicator;
                    if (!this.pingIndicator.hidden) {
                        this.pingMappings.add((AbstractPingIndicator) clientIndicator);
                    }
                } else {
                    this.pingMappings.add((AbstractPingIndicator) clientIndicator);
                }
            }
            for (final EventType eventType : clientIndicator.observedEvents()) {
                this.indicatorMapping
                        .computeIfAbsent(eventType, key -> new ArrayList<>())
                        .add(clientIndicator);
            }
        }
    }

    Collection<ClientIndicator> getindicatorMapping(final EventType eventType) {
        if (!this.indicatorMapping.containsKey(eventType)) {
            return Collections.emptyList();
        }

        return this.indicatorMapping.get(eventType);
    }

    @Override
    @JsonProperty("missingPing")
    public Boolean getMissingPing() {
        return this.pingIndicator.missingPing;
    }

}
