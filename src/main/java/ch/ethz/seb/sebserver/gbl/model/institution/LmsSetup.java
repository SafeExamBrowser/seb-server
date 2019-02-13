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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.INSTITUTION;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class LmsSetup implements GrantEntity, Activatable {

    public static final String FILTER_ATTR_LMS_TYPE = "lms_type";

    public enum LmsType {
        MOCKUP,
        MOODLE,
        OPEN_EDX
    }

    @JsonProperty(LMS_SETUP.ATTR_ID)
    public final Long id;

    @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID)
    @NotNull
    public final Long institutionId;

    @JsonProperty(LMS_SETUP.ATTR_NAME)
    @NotNull(message = "lmsSetup:name:notNull")
    @Size(min = 3, max = 255, message = "lmsSetup:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE)
    @NotNull(message = "lmsSetup:lmsType:notNull")
    public final LmsType lmsType;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME)
    @Size(min = 3, max = 255, message = "lmsSetup:lmsAuthName:size:{min}:{max}:${validatedValue}")
    public final String lmsAuthName;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET)
    @Size(min = 8, max = 255, message = "lmsSetup:lmsAuthSecret:size:{min}:{max}:${validatedValue}")
    public final String lmsAuthSecret;

    @JsonProperty(LMS_SETUP.ATTR_LMS_URL)
    public final String lmsApiUrl;

    @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN)
    public final String lmsRestApiToken;

    @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTNAME)
    @Size(min = 3, max = 255, message = "lmsSetup:sebAuthName:size:{min}:{max}:${validatedValue}")
    public final String sebAuthName;

    @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTSECRET)
    @Size(min = 8, max = 255, message = "lmsSetup:sebAuthSecret:size:{min}:{max}:${validatedValue}")
    public final String sebAuthSecret;

    /** Indicates whether this LmsSetup is active or not */
    @JsonProperty(LMS_SETUP.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public LmsSetup(
            @JsonProperty(LMS_SETUP.ATTR_ID) final Long id,
            @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(LMS_SETUP.ATTR_NAME) final String name,
            @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE) final LmsType lmsType,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME) final String lmsAuthName,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET) final String lmsAuthSecret,
            @JsonProperty(LMS_SETUP.ATTR_LMS_URL) final String lmsApiUrl,
            @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN) final String lmsRestApiToken,
            @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTNAME) final String sebAuthName,
            @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTSECRET) final String sebAuthSecret,
            @JsonProperty(INSTITUTION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.lmsType = lmsType;
        this.lmsAuthName = lmsAuthName;
        this.lmsAuthSecret = lmsAuthSecret;
        this.lmsApiUrl = lmsApiUrl;
        this.lmsRestApiToken = lmsRestApiToken;
        this.sebAuthName = sebAuthName;
        this.sebAuthSecret = sebAuthSecret;
        this.active = (active != null) ? active : Boolean.FALSE;
    }

    public LmsSetup(final String modelId, final POSTMapper mapper) {
        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.institutionId = mapper.getLong(LMS_SETUP.ATTR_INSTITUTION_ID);
        this.name = mapper.getString(LMS_SETUP.ATTR_NAME);
        this.lmsType = mapper.getEnum(LMS_SETUP.ATTR_LMS_TYPE, LmsType.class);
        this.lmsAuthName = mapper.getString(LMS_SETUP.ATTR_LMS_CLIENTNAME);
        this.lmsAuthSecret = mapper.getString(LMS_SETUP.ATTR_LMS_CLIENTSECRET);
        this.lmsApiUrl = mapper.getString(LMS_SETUP.ATTR_LMS_URL);
        this.lmsRestApiToken = mapper.getString(LMS_SETUP.ATTR_LMS_REST_API_TOKEN);
        this.sebAuthName = mapper.getString(LMS_SETUP.ATTR_SEB_CLIENTNAME);
        this.sebAuthSecret = mapper.getString(LMS_SETUP.ATTR_SEB_CLIENTSECRET);
        this.active = mapper.getBooleanObject(LMS_SETUP.ATTR_ACTIVE);
    }

    @Override
    public EntityType entityType() {
        return EntityType.LMS_SETUP;
    }

    @JsonIgnore
    @Override
    public String getOwnerId() {
        return null;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @JsonIgnore
    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public LmsType getLmsType() {
        return this.lmsType;
    }

    public String getLmsAuthName() {
        return this.lmsAuthName;
    }

    public String getLmsAuthSecret() {
        return this.lmsAuthSecret;
    }

    public String getLmsApiUrl() {
        return this.lmsApiUrl;
    }

    public String getLmsRestApiToken() {
        return this.lmsRestApiToken;
    }

    public String getSebAuthName() {
        return this.sebAuthName;
    }

    public String getSebAuthSecret() {
        return this.sebAuthSecret;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public String toString() {
        return "LmsSetup [id=" + this.id + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", lmsType=" + this.lmsType
                + ", lmsAuthName=" + this.lmsAuthName + ", lmsAuthSecret=" + this.lmsAuthSecret + ", lmsApiUrl="
                + this.lmsApiUrl
                + ", lmsRestApiToken=" + this.lmsRestApiToken + ", sebAuthName=" + this.sebAuthName + ", sebAuthSecret="
                + this.sebAuthSecret + ", active=" + this.active + "]";
    }

    public static EntityName toName(final LmsSetup lmsSetup) {
        return new EntityName(
                EntityType.LMS_SETUP,
                String.valueOf(lmsSetup.id),
                lmsSetup.name);
    }

}
