/*
 * Copyright (c) 2022 ETH Zürich, IT Services
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

    public final int notificationFlag;
    public final boolean missingPing;
    public final boolean grantChecked;
    public final boolean grantDenied;
    public final boolean pendingNotification;
    public final boolean sebVersionDenied;

    @JsonCreator
    public ClientMonitoringData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(ATTR_STATUS) final ConnectionStatus status,
            @JsonProperty(ATTR_INDICATOR_VALUES) final Map<Long, String> indicatorVals,
            @JsonProperty(ATTR_NOTIFICATION_FLAG) final Integer notificationFlag) {

        this.id = id;
        this.status = status;
        this.indicatorVals = indicatorVals;
        this.notificationFlag = notificationFlag != null ? notificationFlag : -1;
        this.missingPing = notificationFlag != null && (notificationFlag & FLAG_MISSING_PING) > 0;
        this.grantChecked = notificationFlag == null || (notificationFlag & FLAG_GRANT_NOT_CHECKED) == 0;
        this.grantDenied = notificationFlag != null && (notificationFlag & FLAG_GRANT_DENIED) > 0;
        this.pendingNotification = notificationFlag != null && (notificationFlag & FLAG_PENDING_NOTIFICATION) > 0;
        this.sebVersionDenied = notificationFlag != null && (notificationFlag & FLAG_INVALID_SEB_VERSION) > 0;
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
    public Integer notificationFlag() {
        return this.notificationFlag;
    }

    public boolean hasChanged(final ClientMonitoringData other) {
        return this.status != other.status ||
                !Objects.equals(this.notificationFlag, other.notificationFlag);
    }

    public boolean indicatorValuesEquals(final ClientMonitoringData other) {
        return Objects.equals(this.indicatorVals, other.indicatorVals);
    }

}
