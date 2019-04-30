/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.VIEW;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class View implements Entity {

    @JsonProperty(VIEW.ATTR_ID)
    public final Long id;

    @JsonProperty(VIEW.ATTR_NAME)
    public final String name;

    @NotNull
    @JsonProperty(VIEW.ATTR_POSITION)
    public final Integer position;

    public View(
            @JsonProperty(VIEW.ATTR_ID) final Long id,
            @JsonProperty(VIEW.ATTR_NAME) final String name,
            @JsonProperty(VIEW.ATTR_POSITION) final Integer position) {

        this.id = id;
        this.name = name;
        this.position = position;
    }

    public View(final POSTMapper postParams) {
        this.id = null;
        this.name = postParams.getString(Domain.VIEW.ATTR_NAME);
        this.position = postParams.getInteger(Domain.VIEW.ATTR_POSITION);
    }

    public Integer getPosition() {
        return this.position;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.VIEW;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("View [id=");
        builder.append(this.id);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", position=");
        builder.append(this.position);
        builder.append("]");
        return builder.toString();
    }

}
