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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PendingNotificationIndication;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.PingIntervalClientIndicator;

public class ClientConnectionDataInternal extends ClientConnectionData {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionDataInternal.class);

    final EnumMap<EventType, Collection<ClientIndicator>> indicatorMapping;

    PingIntervalClientIndicator pingIndicator = null;
    private final PendingNotificationIndication pendingNotificationIndication;

    protected ClientConnectionDataInternal(
            final ClientConnection clientConnection,
            final PendingNotificationIndication pendingNotificationIndication,
            final List<ClientIndicator> clientIndicators) {

        super(clientConnection, clientIndicators);
        this.pendingNotificationIndication = pendingNotificationIndication;

        this.indicatorMapping = new EnumMap<>(EventType.class);
        for (final ClientIndicator clientIndicator : clientIndicators) {
            if (clientIndicator instanceof PingIntervalClientIndicator) {
                if (this.pingIndicator != null) {
                    log.error("Currently only one ping indicator is allowed: {}", clientIndicator);
                    continue;
                }
                this.pingIndicator = (PingIntervalClientIndicator) clientIndicator;
            }
            for (final EventType eventType : clientIndicator.observedEvents()) {
                this.indicatorMapping
                        .computeIfAbsent(eventType, key -> new ArrayList<>())
                        .add(clientIndicator);
            }
        }
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        if (this.pingIndicator != null) {
            this.pingIndicator.notifyPing(timestamp, pingNumber);
        }
    }

    Collection<ClientIndicator> getIndicatorMapping(final EventType eventType) {
        return this.indicatorMapping.getOrDefault(
                eventType,
                Collections.emptyList());
    }

    @Override
    @JsonProperty(ATTR_MISSING_PING)
    public final Boolean getMissingPing() {
        return this.pingIndicator != null && this.pingIndicator.hasIncident();
    }

    @Override
    @JsonProperty(ATTR_PENDING_NOTIFICATION)
    public final Boolean pendingNotification() {
        return this.pendingNotificationIndication.notifictionPending();
    }

    @Override
    @JsonIgnore
    public final boolean hasAnyIncident() {
        return getMissingPing() || pendingNotification() || hasIncident();
    }

    private boolean hasIncident() {
        return this.indicatorMapping.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(ClientIndicator::hasIncident)
                .findFirst()
                .isPresent();
    }

}
