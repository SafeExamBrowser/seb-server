/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.EnumSet;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClientConnection implements GrantEntity {

    public enum ConnectionStatus {
        UNDEFINED(false),
        CONNECTION_REQUESTED(false),
        AUTHENTICATED(true),
        ACTIVE(true),
        CLOSED(false),
        DISABLED(false);

        public final boolean establishedStatus;

        private ConnectionStatus(final boolean establishedStatus) {
            this.establishedStatus = establishedStatus;
        }

    }

    public static final ClientConnection EMPTY_CLIENT_CONNECTION = new ClientConnection(
            -1L,
            -1L,
            -1L,
            ConnectionStatus.UNDEFINED,
            null,
            null,
            null,
            null,
            null);

    public static final String FILTER_ATTR_EXAM_ID = Domain.CLIENT_CONNECTION.ATTR_EXAM_ID;
    public static final String FILTER_ATTR_STATUS = Domain.CLIENT_CONNECTION.ATTR_STATUS;
    public static final String FILTER_ATTR_SESSION_ID = Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID)
    public final Long id;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_STATUS)
    public final ConnectionStatus status;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN)
    public final String connectionToken;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID)
    public final String userSessionId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS)
    public final String clientAddress;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_VIRTUAL_CLIENT_ADDRESS)
    public final String virtualClientAddress;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CREATION_TIME)
    private final Long creationTime;

    @JsonCreator
    public ClientConnection(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_STATUS) final ConnectionStatus status,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID) final String userSessionId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS) final String clientAddress,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_VIRTUAL_CLIENT_ADDRESS) final String virtualClientAddress,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CREATION_TIME) final Long creationTime) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.status = status;
        this.connectionToken = connectionToken;
        this.userSessionId = userSessionId;
        this.clientAddress = clientAddress;
        this.virtualClientAddress = virtualClientAddress;
        this.creationTime = creationTime;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_CONNECTION;
    }

    @Override
    public String getName() {
        return this.userSessionId;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getExamId() {
        return this.examId;
    }

    public ConnectionStatus getStatus() {
        return this.status;
    }

    public String getConnectionToken() {
        return this.connectionToken;
    }

    public String getClientAddress() {
        return this.clientAddress;
    }

    public String getUserSessionId() {
        return this.userSessionId;
    }

    public String getVirtualClientAddress() {
        return this.virtualClientAddress;
    }

    public Long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
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
        final ClientConnection other = (ClientConnection) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    public boolean dataEquals(final ClientConnection other) {
        if (other == null) {
            return true;
        }
        if (this.clientAddress == null) {
            if (other.clientAddress != null)
                return false;
        } else if (!this.clientAddress.equals(other.clientAddress))
            return false;
        if (this.status != other.status)
            return false;
        if (this.userSessionId == null) {
            if (other.userSessionId != null)
                return false;
        } else if (!this.userSessionId.equals(other.userSessionId))
            return false;
        if (this.virtualClientAddress == null) {
            if (other.virtualClientAddress != null)
                return false;
        } else if (!this.virtualClientAddress.equals(other.virtualClientAddress))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientConnection [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", status=");
        builder.append(this.status);
        builder.append(", connectionToken=");
        builder.append(this.connectionToken);
        builder.append(", userSessionId=");
        builder.append(this.userSessionId);
        builder.append(", clientAddress=");
        builder.append(this.clientAddress);
        builder.append(", virtualClientAddress=");
        builder.append(this.virtualClientAddress);
        builder.append(", creationTime=");
        builder.append(this.creationTime);
        builder.append("]");
        return builder.toString();
    }

    public static Predicate<ClientConnection> getStatusPredicate(final ConnectionStatus status) {
        return connection -> connection.status == status;
    }

    public static Predicate<ClientConnection> getStatusPredicate(final ConnectionStatus... status) {
        final EnumSet<ConnectionStatus> stati = EnumSet.allOf(ConnectionStatus.class);
        if (status != null) {
            for (int i = 0; i < status.length; i++) {
                stati.add(status[i]);
            }
        }
        return connection -> stati.contains(connection.status);
    }

}
