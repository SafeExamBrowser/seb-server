/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringSEBConnectionData {

    public static final String ATTR_CONNECTIONS = "connections";
    public static final String ATTR_STATUS_MAPPING = "statusMapping";
    public static final String ATTR_CLIENT_GROUP_MAPPING = "clientGroupMapping";

    @JsonProperty(ATTR_CONNECTIONS)
    public final Collection<ClientConnectionData> connections;
    @JsonProperty(ATTR_STATUS_MAPPING)
    public final int[] connectionsPerStatus;
    @JsonProperty(ATTR_CLIENT_GROUP_MAPPING)
    public final Map<Long, Integer> connectionsPerClientGroup;

    @JsonCreator
    public MonitoringSEBConnectionData(
            @JsonProperty(ATTR_CONNECTIONS) final Collection<ClientConnectionData> connections,
            @JsonProperty(ATTR_STATUS_MAPPING) final int[] connectionsPerStatus,
            @JsonProperty(ATTR_CLIENT_GROUP_MAPPING) final Map<Long, Integer> connectionsPerClientGroup) {

        this.connections = connections;
        this.connectionsPerStatus = connectionsPerStatus;
        this.connectionsPerClientGroup = connectionsPerClientGroup;
    }

    public Collection<ClientConnectionData> getConnections() {
        return this.connections;
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
            return -1;
        }
        return this.connectionsPerClientGroup.get(clientGroupId);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MonitoringSEBConnectionData [connections=");
        builder.append(this.connections);
        builder.append(", connectionsPerStatus=");
        builder.append(Arrays.toString(this.connectionsPerStatus));
        builder.append(", connectionsPerClientGroup=");
        builder.append(this.connectionsPerClientGroup);
        builder.append("]");
        return builder.toString();
    }

}
