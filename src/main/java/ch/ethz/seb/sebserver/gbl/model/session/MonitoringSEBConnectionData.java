/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringSEBConnectionData {

    public static final String ATTR_CONNECTIONS = "connections";
    public static final String ATTR_STATUS_MAPPING = "statusMapping";

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID)
    public final Long examId;
    @JsonProperty(ATTR_CONNECTIONS)
    public final Collection<ClientConnectionData> connections;
    @JsonProperty(ATTR_STATUS_MAPPING)
    public final int[] connectionsPerStatus;

    @JsonCreator
    public MonitoringSEBConnectionData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(ATTR_CONNECTIONS) final Collection<ClientConnectionData> connections,
            @JsonProperty(ATTR_STATUS_MAPPING) final int[] connectionsPerStatus) {

        this.examId = examId;
        this.connections = connections;
        this.connectionsPerStatus = connectionsPerStatus;
    }

    public Long getExamId() {
        return this.examId;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.examId == null) ? 0 : this.examId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MonitoringSEBConnectionData other = (MonitoringSEBConnectionData) obj;
        if (this.examId == null) {
            if (other.examId != null)
                return false;
        } else if (!this.examId.equals(other.examId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MonitoringSEBConnectionData [examId=");
        builder.append(this.examId);
        builder.append(", connections=");
        builder.append(this.connections);
        builder.append(", connectionsPerStatus=");
        builder.append(Arrays.toString(this.connectionsPerStatus));
        builder.append("]");
        return builder.toString();
    }

}
