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

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityKeyAndName implements ModelIdAware, ModelNameAware {

    @JsonProperty(value = "entityType", required = true)
    public final EntityType entityType;
    @JsonProperty(value = Domain.ATTR_ID, required = true)
    public final String modelId;
    @JsonProperty(value = "name", required = true)
    public final String name;

    @JsonCreator
    public EntityKeyAndName(
            @JsonProperty(value = "entityType", required = true) final EntityType entityType,
            @JsonProperty(value = Domain.ATTR_ID, required = true) final String id,
            @JsonProperty(value = "name", required = true) final String name) {

        this.entityType = entityType;
        this.modelId = id;
        this.name = name;
    }

    public EntityKeyAndName(final EntityKey entityKey, final String name) {

        this.entityType = entityKey.entityType;
        this.modelId = entityKey.modelId;
        this.name = name;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    @JsonIgnore
    public String getModelId() {
        return this.modelId;
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
        final EntityKeyAndName other = (EntityKeyAndName) obj;
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
        return "EntityIdAndName [entityType=" + this.entityType + ", id=" + this.modelId + ", name=" + this.name + "]";
    }

}
