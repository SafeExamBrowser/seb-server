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
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;

public class ClientMonitoringData implements ClientMonitoringDataView {

    public final Long id;
    public final ConnectionStatus status;
    public final String connectionToken;
    public final String userSessionId;
    public final String info;
    public final Map<Long, String> indicatorVals;
    public final Set<Long> groups;
    public final boolean missingPing;
    public final boolean pendingNotification;

    public ClientMonitoringData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(ATTR_STATUS) final ConnectionStatus status,
            @JsonProperty(ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(ATTR_EXAM_USER_SESSION_ID) final String userSessionId,
            @JsonProperty(ATTR_INFO) final String info,
            @JsonProperty(ATTR_INDICATOR_VALUES) final Map<Long, String> indicatorVals,
            @JsonProperty(ATTR_CLIENT_GROUPS) final Set<Long> groups,
            @JsonProperty(ATTR_MISSING_PING) final boolean missingPing,
            @JsonProperty(ATTR_PENDING_NOTIFICATION) final boolean pendingNotification) {

        this.id = id;
        this.status = status;
        this.connectionToken = connectionToken;
        this.userSessionId = userSessionId;
        this.info = info;
        this.indicatorVals = indicatorVals;
        this.groups = groups;
        this.missingPing = missingPing;
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
    public String getConnectionToken() {
        return this.connectionToken;
    }

    @Override
    public String getUserSessionId() {
        return this.userSessionId;
    }

    @Override
    public String getInfo() {
        return this.info;
    }

    @Override
    public Map<Long, String> getIndicatorValues() {
        return this.indicatorVals;
    }

    @Override
    public Set<Long> getGroups() {
        return this.groups;
    }

    @Override
    public boolean isMissingPing() {
        return this.missingPing;
    }

    @Override
    public boolean isPendingNotification() {
        return this.pendingNotification;
    }

    public boolean dataEquals(final ClientMonitoringData other) {
        if (other == null) {
            return true;
        }
        if (this.connectionToken == null) {
            if (other.connectionToken != null)
                return false;
        } else if (!this.connectionToken.equals(other.connectionToken))
            return false;

        if (this.status != other.status)
            return false;

        if (this.userSessionId == null) {
            if (other.userSessionId != null)
                return false;
        } else if (!this.userSessionId.equals(other.userSessionId)) {
            return false;
        }

        if (!Objects.equals(this.groups, other.groups)) {
            return false;
        }

        return true;
    }

    public boolean indicatorValuesEquals(final ClientMonitoringData other) {
        return Objects.equals(this.indicatorVals, other.indicatorVals);
    }

}
