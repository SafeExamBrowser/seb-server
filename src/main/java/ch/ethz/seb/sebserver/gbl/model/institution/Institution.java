/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.INSTITUTION;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class Institution implements GrantEntity, Activatable {

    @JsonProperty(Domain.ATTR_ID)
    public final Long id;

    @JsonProperty(INSTITUTION.ATTR_NAME)
    @NotNull(message = "institution:name:notNull")
    @Size(min = 3, max = 255, message = "institution:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(INSTITUTION.ATTR_URL_SUFFIX)
    @Size(min = 3, max = 255, message = "institution:urlSuffix:size:{min}:{max}:${validatedValue}")
    public final String urlSuffix;

    @JsonProperty(INSTITUTION.ATTR_LOGO_IMAGE)
    public final String logoImage;

    @JsonProperty(INSTITUTION.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public Institution(
            @JsonProperty(Domain.ATTR_ID) final Long id,
            @JsonProperty(INSTITUTION.ATTR_NAME) final String name,
            @JsonProperty(INSTITUTION.ATTR_URL_SUFFIX) final String urlSuffix,
            @JsonProperty(INSTITUTION.ATTR_LOGO_IMAGE) final String logoImage,
            @JsonProperty(INSTITUTION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.name = name;
        this.urlSuffix = urlSuffix;
        this.logoImage = logoImage;
        this.active = active;
    }

    public Institution(final String modelId, final POSTMapper mapper) {
        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.name = mapper.getString(INSTITUTION.ATTR_NAME);
        this.urlSuffix = mapper.getString(INSTITUTION.ATTR_URL_SUFFIX);
        this.logoImage = mapper.getString(INSTITUTION.ATTR_LOGO_IMAGE);
        this.active = mapper.getBooleanObject(INSTITUTION.ATTR_ACTIVE);
    }

    @Override
    public EntityType entityType() {
        return EntityType.INSTITUTION;
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
        return this.id;
    }

    @Override
    public String getOwnerId() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getUrlSuffix() {
        return this.urlSuffix;
    }

    public String getLogoImage() {
        return this.logoImage;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public String toString() {
        return "Institution [id=" + this.id + ", name=" + this.name + ", urlSuffix=" + this.urlSuffix + ", logoImage="
                + this.logoImage
                + ", active=" + this.active + "]";
    }

}
