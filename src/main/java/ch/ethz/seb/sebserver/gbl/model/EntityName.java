/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityName implements ModelIdAware {

    @JsonProperty(value = API.PARAM_ENTITY_TYPE, required = true)
    public final EntityType entityType;
    @JsonProperty(value = API.PARAM_MODEL_ID, required = true)
    public final String modelId;
    @JsonProperty(value = "name", required = true)
    public final String name;

    @JsonCreator
    public EntityName(
            @JsonProperty(value = API.PARAM_ENTITY_TYPE, required = true) final EntityType entityType,
            @JsonProperty(value = API.PARAM_MODEL_ID, required = true) final String id,
            @JsonProperty(value = "name", required = true) final String name) {

        this.entityType = entityType;
        this.modelId = id;
        this.name = name;
    }

    public EntityName(final EntityKey entityKey, final String name) {

        this.entityType = entityKey.entityType;
        this.modelId = entityKey.modelId;
        this.name = name;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    public String getName() {
        return this.name;
    }

    @Override
    @JsonIgnore
    public String getModelId() {
        return this.modelId;
    }

    @JsonIgnore
    public EntityKey getEntityKey() {
        return new EntityKey(getModelId(), getEntityType());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.entityType == null) ? 0 : this.entityType.hashCode());
        result = prime * result + ((this.modelId == null) ? 0 : this.modelId.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        final EntityName other = (EntityName) obj;
        if (this.entityType != other.entityType)
            return false;
        if (this.modelId == null) {
            if (other.modelId != null)
                return false;
        } else if (!this.modelId.equals(other.modelId))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        } else if (!this.name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EntityName [entityType=");
        builder.append(this.entityType);
        builder.append(", modelId=");
        builder.append(this.modelId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append("]");
        return builder.toString();
    }

}
