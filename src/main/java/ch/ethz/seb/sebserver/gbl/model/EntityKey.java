/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;

/** An EntityKey uniquely identifies a domain entity within the SEB Server's domain model.
 * An EntityKey consists of the model identifier of a domain entity and the type of the entity. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityKey implements ModelIdAware, Serializable {

    private static final long serialVersionUID = -2368065921846821061L;

    /** The model identifier of the entity */
    @JsonProperty(value = "modelId", required = true)
    @NotNull
    public final String modelId;

    /** The type of the entity */
    @JsonProperty(value = "entityType", required = true)
    @NotNull
    public final EntityType entityType;

    /** pre-calculated hash value. Since EntityKey is fully immutable this is a valid optimization */
    private final int hash;

    @JsonCreator
    public EntityKey(
            @JsonProperty(value = "modelId", required = true) final String modelId,
            @JsonProperty(value = "entityType", required = true) final EntityType entityType) {

        if (modelId == null) {
            throw new IllegalArgumentException("modelId has null reference");
        }
        if (entityType == null) {
            throw new IllegalArgumentException("entityType has null reference");
        }

        this.modelId = modelId;
        this.entityType = entityType;

        final int prime = 31;
        int result = 1;
        result = prime * result + this.entityType.hashCode();
        result = prime * result + this.modelId.hashCode();
        this.hash = result;
    }

    public EntityKey(
            final Long pk,
            final EntityType entityType) {

        this(String.valueOf(pk), entityType);
        if (pk == null) {
            throw new IllegalArgumentException("modelId has null reference");
        }
    }

    /** Get the model identifier of this EntityKey
     *
     * @return the model identifier of this EntityKey */
    @Override
    public String getModelId() {
        return this.modelId;
    }

    /** Get the entity type EntityKey
     *
     * @return the model identifier of this EntityKey */
    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final EntityKey other = (EntityKey) obj;
        if (this.entityType != other.entityType)
            return false;
        if (this.modelId == null) {
            if (other.modelId != null)
                return false;
        } else if (!this.modelId.equals(other.modelId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EntityKey [modelId=");
        builder.append(this.modelId);
        builder.append(", entityType=");
        builder.append(this.entityType);
        builder.append("]");
        return builder.toString();
    }

}
