/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientMonitoringData implements ClientMonitoringDataView {

    public final Long id;
    public final ConnectionStatus status;
    public final Map<Long, String> indicatorVals;
    public final boolean missingPing;
    public final boolean missingGrant;
    public final boolean pendingNotification;

    @JsonCreator
    public ClientMonitoringData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(ATTR_STATUS) final ConnectionStatus status,
            @JsonProperty(ATTR_INDICATOR_VALUES) final Map<Long, String> indicatorVals,
            @JsonProperty(ATTR_MISSING_PING) final boolean missingPing,
            @JsonProperty(ATTR_MISSING_GRANT) final boolean missingGrant,
            @JsonProperty(ATTR_PENDING_NOTIFICATION) final boolean pendingNotification) {

        this.id = id;
        this.status = status;
        this.indicatorVals = indicatorVals;
        this.missingPing = missingPing;
        this.missingGrant = missingGrant;
        this.pendingNotification = pendingNotification;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public ConnectionStatus getStatus() {
        return this.status;
    }

    @Override
    public Map<Long, String> getIndicatorValues() {
        return this.indicatorVals;
    }

    @Override
    public boolean isMissingPing() {
        return this.missingPing;
    }

    @Override
    public boolean isMissingGrant() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPendingNotification() {
        return this.pendingNotification;
    }

    public boolean indicatorValuesEquals(final ClientMonitoringData other) {
        return Objects.equals(this.indicatorVals, other.indicatorVals);
    }

}
