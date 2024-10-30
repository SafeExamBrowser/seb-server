/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionIssueStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringDataView;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringSEBConnectionData {

    public static final String ATTR_CONNECTIONS = "cons";
    public static final String ATTR_STATUS_MAPPING = "sm";
    public static final String ATTR_CLIENT_GROUP_MAPPING = "cgm";
    public static final String ATTR_ISSUE_MAPPING = "im";

    @JsonProperty(ATTR_CONNECTIONS)
    public final Collection<? extends ClientMonitoringDataView> monitoringData;

    @JsonProperty(ATTR_STATUS_MAPPING)
    public final int[] connectionsPerStatus;

    @JsonProperty(ATTR_CLIENT_GROUP_MAPPING)
    public final Map<Long, Integer> connectionsPerClientGroup;

    @JsonProperty(ATTR_ISSUE_MAPPING)
    public final int[] connectionPerIssue;


    @JsonCreator
    public MonitoringSEBConnectionData(
            @JsonProperty(ATTR_CONNECTIONS) final Collection<ClientMonitoringData> connections,
            @JsonProperty(ATTR_STATUS_MAPPING) final int[] connectionsPerStatus,
            @JsonProperty(ATTR_ISSUE_MAPPING) final int[] connectionPerIssue,
            @JsonProperty(ATTR_CLIENT_GROUP_MAPPING) final Map<Long, Integer> connectionsPerClientGroup) {

        this.monitoringData = connections;
        this.connectionsPerStatus = connectionsPerStatus;
        this.connectionPerIssue = connectionPerIssue;
        this.connectionsPerClientGroup = connectionsPerClientGroup;
    }

    public MonitoringSEBConnectionData(
            final int[] connectionsPerStatus,
            final Map<Long, Integer> connectionsPerClientGroup,
            final int[] connectionsPerIssue,
            final Collection<? extends ClientMonitoringDataView> connections) {

        this.connectionsPerStatus = connectionsPerStatus;
        this.connectionsPerClientGroup = connectionsPerClientGroup;
        this.connectionPerIssue = connectionsPerIssue;
        this.monitoringData = connections;
    }

    public Collection<? extends ClientMonitoringDataView> getMonitoringData() {
        return this.monitoringData;
    }

    public int[] getConnectionsPerStatus() {
        return this.connectionsPerStatus;
    }

    @JsonIgnore
    public int getNumberOfConnection(final ConnectionStatus status) {
        if (this.connectionsPerStatus == null || this.connectionsPerStatus.length <= status.code) {
            return -1;
        }
        return this.connectionsPerStatus[status.code];
    }

    @JsonIgnore
    public int getNumberOfConnection(final Long clientGroupId) {
        if (this.connectionsPerClientGroup == null || !this.connectionsPerClientGroup.containsKey(clientGroupId)) {
            return 0;
        }
        return this.connectionsPerClientGroup.get(clientGroupId);
    }

    @JsonIgnore
    public int getNumberOfConnection(final ConnectionIssueStatus connectionIssueStatus) {
        if (this.connectionPerIssue == null || this.connectionPerIssue.length <= connectionIssueStatus.code) {
            return 0;
        }
        return this.connectionPerIssue[connectionIssueStatus.code];
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MonitoringSEBConnectionData [connections=");
        builder.append(this.monitoringData);
        builder.append(", connectionsPerStatus=");
        builder.append(Arrays.toString(this.connectionsPerStatus));
        builder.append(", connectionsPerClientGroup=");
        builder.append(this.connectionsPerClientGroup);
        builder.append("]");
        return builder.toString();
    }

}
