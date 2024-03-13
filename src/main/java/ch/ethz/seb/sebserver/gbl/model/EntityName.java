/*
 * Copyright (c) 2018 ETH Zürich, IT Services
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
public class EntityName extends EntityKey {

    private static final long serialVersionUID = 9577137222563155L;

    public static final String ATTR_NAME = "name";

    @JsonProperty(value = ATTR_NAME, required = true)
    public final String name;

    @JsonCreator
    public EntityName(
            @JsonProperty(value = API.PARAM_MODEL_ID, required = true) final String id,
            @JsonProperty(value = API.PARAM_ENTITY_TYPE, required = true) final EntityType entityType,
            @JsonProperty(value = ATTR_NAME) final String name) {

        super(id, entityType);
        this.name = name;
    }

    public EntityName(final EntityKey entityKey, final String name) {

        super(entityKey.modelId, entityKey.entityType);
        this.name = name;
    }

    @Override
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
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
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
