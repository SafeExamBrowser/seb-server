/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticClientConnectionData implements Entity {

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID)
    public final Long id;
    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN)
    public final String connectionToken;
    @JsonProperty(ClientConnection.ATTR_INFO)
    public final String info;
    @JsonProperty(ClientConnectionData.ATTR_CLIENT_GROUPS)
    public Set<Long> groups = null;

    @JsonCreator
    public StaticClientConnectionData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(ClientConnection.ATTR_INFO) final String info,
            @JsonProperty(ClientConnectionData.ATTR_CLIENT_GROUPS) final Set<Long> groups) {

        this.id = id;
        this.connectionToken = connectionToken;
        this.info = info;
        this.groups = groups;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_CONNECTION;
    }

    @Override
    public String getName() {
        return this.connectionToken;
    }

    public Set<Long> getGroups() {
        return this.groups;
    }

    public void setGroups(final Set<Long> groups) {
        this.groups = groups;
    }

    public Long getId() {
        return this.id;
    }

    public String getConnectionToken() {
        return this.connectionToken;
    }

    public String getInfo() {
        return this.info;
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
        final StaticClientConnectionData other = (StaticClientConnectionData) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

}
