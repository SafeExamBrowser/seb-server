/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_CLIENT_CONFIGURATION;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class SebClientConfig implements GrantEntity, Activatable {

    public static final String FILTER_ATTR_FROM = "from";

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME)
    public final String name;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE)
    public final DateTime date;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE)
    public final Boolean active;

    public SebClientConfig(
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID) final Long id,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME) final String name,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE) final DateTime date,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.date = date;
        this.active = active;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_CLIENT_CONFIGURATION;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return null;
    }

    public Long getId() {
        return this.id;
    }

    public DateTime getDate() {
        return this.date;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public String toString() {
        return "SEBClientConfig [id=" + this.id + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", date=" + this.date
                + ", active=" + this.active + "]";
    }

}
