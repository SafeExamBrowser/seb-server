/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringDataView;
import ch.ethz.seb.sebserver.gbl.model.session.ClientStaticData;
import ch.ethz.seb.sebserver.gbl.monitoring.IndicatorValue;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PendingNotificationIndication;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.PingIntervalClientIndicator;

public class ClientConnectionDataInternal extends ClientConnectionData {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionDataInternal.class);

    // TODO why list for type? Is it possible to restrict to one per type?
    final EnumMap<EventType, Collection<ClientIndicator>> indicatorMapping;
    final EnumMap<Indicator.IndicatorType, ClientIndicator> indicatorTypeMapping;

    PingIntervalClientIndicator pingIndicator = null;
    private final PendingNotificationIndication pendingNotificationIndication;

    private final Boolean grantDenied;
    private final Boolean sebVersionDenied;

    public ClientConnectionDataInternal(
            final ClientConnection clientConnection,
            final PendingNotificationIndication pendingNotificationIndication,
            final List<ClientIndicator> clientIndicators,
            final Set<Long> groups) {

        super(clientConnection, clientIndicators, groups);
        this.pendingNotificationIndication = pendingNotificationIndication;

        this.indicatorMapping = new EnumMap<>(EventType.class);
        this.indicatorTypeMapping = new EnumMap<>(Indicator.IndicatorType.class);
        for (final ClientIndicator clientIndicator : clientIndicators) {
            indicatorTypeMapping.put(clientIndicator.getType(), clientIndicator);
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

        if (clientConnection.securityCheckGranted == null) {
            this.grantDenied = null;
        } else {
            this.grantDenied = !clientConnection.securityCheckGranted;
        }

        if (clientConnection.clientVersionGranted == null) {
            this.sebVersionDenied = null;
        } else {
            this.sebVersionDenied = !clientConnection.clientVersionGranted;
        }
    }

    public final void notifyPing(final long timestamp) {
        if (this.pingIndicator != null) {
            this.pingIndicator.notifyPing(timestamp);
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

    @JsonIgnore
    public final boolean hasIncident(final Indicator.IndicatorType type) {
        final ClientIndicator clientIndicator = indicatorTypeMapping.get(type);
        return clientIndicator != null && clientIndicator.hasIncident();
    }

    @JsonIgnore
    public final boolean hasWarning(final Indicator.IndicatorType type) {
        final ClientIndicator clientIndicator = indicatorTypeMapping.get(type);
        return clientIndicator != null && clientIndicator.hasWarning();
    }

    @JsonIgnore
    public final double getValueByType(final Indicator.IndicatorType type) {
        final ClientIndicator clientIndicator = indicatorTypeMapping.get(type);
        return clientIndicator != null ? clientIndicator.getValue() : Double.NaN;
    }

    @JsonIgnore
    public Indicator.DataMap getIndicatorDataMap(final Indicator.IndicatorType type) {
        final ClientIndicator clientIndicator = indicatorTypeMapping.get(type);
        return clientIndicator != null ? clientIndicator.getDataMap() : null;
    }

    private boolean hasIncident() {
        return this.indicatorMapping.values()
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(ClientIndicator::hasIncident);
    }

    /** This is a wrapper for the live monitoring data view of this client connection data */
    @JsonIgnore
    public final ClientMonitoringDataView monitoringDataView = new ClientMonitoringDataView() {

        @Override
        public Long getId() {
            return ClientConnectionDataInternal.this.clientConnection.id;
        }

        @Override
        public ConnectionStatus getStatus() {
            return ClientConnectionDataInternal.this.clientConnection.status;
        }

        @Override
        public Map<Long, String> getIndicatorValues() {
            return ClientConnectionDataInternal.this.indicatorValues
                    .stream()
                    .collect(Collectors.toMap(
                            IndicatorValue::getIndicatorId,
                            IndicatorValue::getDisplayValue));
        }

        @Override
        public Integer notificationFlag() {
            final int flag = 0
                    | (isMissingPing() ? ClientMonitoringDataView.FLAG_MISSING_PING : 0)
                    | (isPendingNotification() ? ClientMonitoringDataView.FLAG_PENDING_NOTIFICATION : 0)
                    | (!isGrantChecked() ? ClientMonitoringDataView.FLAG_GRANT_NOT_CHECKED : 0)
                    | (isGrantDenied() ? ClientMonitoringDataView.FLAG_GRANT_DENIED : 0)
                    | (isSEBVersionDenied() ? ClientMonitoringDataView.FLAG_INVALID_SEB_VERSION : 0);
            
            return (flag > 0) ? flag : null;
        }

        @Override
        @JsonIgnore
        public boolean isMissingPing() {
            return BooleanUtils.isTrue(getMissingPing());
        }

        @Override
        @JsonIgnore
        public boolean isPendingNotification() {
            return BooleanUtils.isTrue(pendingNotification());
        }

        @Override
        @JsonIgnore
        public boolean isGrantChecked() {
            return ClientConnectionDataInternal.this.grantDenied != null;
        }

        @Override
        @JsonIgnore
        public boolean isGrantDenied() {
            return BooleanUtils.isTrue(ClientConnectionDataInternal.this.grantDenied);
        }

        @Override
        @JsonIgnore
        public boolean isSEBVersionDenied() {
            return BooleanUtils.isTrue(ClientConnectionDataInternal.this.sebVersionDenied);
        }
    };

    /** This is a static monitoring connection data wrapper/holder */
    @JsonIgnore
    public final ClientStaticData clientStaticData =
            new ClientStaticData(
                    ClientConnectionDataInternal.this.clientConnection.id,
                    ClientConnectionDataInternal.this.clientConnection.connectionToken,
                    ClientConnectionDataInternal.this.clientConnection.userSessionId,
                    ClientConnectionDataInternal.this.clientConnection.ask,
                    ClientConnectionDataInternal.this.clientConnection.info,
                    this.groups);

    
}
