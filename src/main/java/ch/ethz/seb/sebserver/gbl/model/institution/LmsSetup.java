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

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class LmsSetup implements GrantEntity {

    public enum LMSType {
        MOCKUP,
        MOODLE,
        OPEN_EDX
    }

    @JsonProperty(Domain.ATTR_ID)
    public final Long id;

    @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID)
    @NotNull
    public final Long institutionId;

    @JsonProperty(LMS_SETUP.ATTR_NAME)
    @NotNull
    @Size(min = 3, max = 255, message = "lmsSetup:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE)
    @NotNull
    public final LMSType lmsType;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME)
    @Size(min = 3, max = 255, message = "lmsSetup:lmsAuthName:size:{min}:{max}:${validatedValue}")
    public final String lmsAuthName;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET)
    @Size(min = 8, max = 255, message = "lmsSetup:lmsAuthSecret:size:{min}:{max}:${validatedValue}")
    public final String lmsAuthSecret;

    @JsonProperty(LMS_SETUP.ATTR_LMS_URL)
    @NotNull
    public final String lmsApiUrl;

    @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN)
    public final String lmsRestApiToken;

    @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTNAME)
    @Size(min = 3, max = 255, message = "lmsSetup:sebAuthName:size:{min}:{max}:${validatedValue}")
    public final String sebAuthName;

    @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTSECRET)
    @Size(min = 8, max = 255, message = "lmsSetup:sebAuthSecret:size:{min}:{max}:${validatedValue}")
    public final String sebAuthSecret;

    @JsonCreator
    public LmsSetup(
            @JsonProperty(Domain.ATTR_ID) final Long id,
            @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(LMS_SETUP.ATTR_NAME) final String name,
            @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE) final LMSType lmsType,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME) final String lmsAuthName,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET) final String lmsAuthSecret,
            @JsonProperty(LMS_SETUP.ATTR_LMS_URL) final String lmsApiUrl,
            @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN) final String lmsRestApiToken,
            @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTNAME) final String sebAuthName,
            @JsonProperty(LMS_SETUP.ATTR_SEB_CLIENTSECRET) final String sebAuthSecret) {

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
    }

    @Override
    public EntityType entityType() {
        return EntityType.LMS_SETUP;
    }

    @JsonIgnore
    @Override
    public String getOwnerUUID() {
        return null;
    }

    public Long getId() {
        return this.id;
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

    public String getName() {
        return this.name;
    }

    public LMSType getLmsType() {
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

    @Override
    public String toString() {
        return "LmsSetup [id=" + this.id + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", lmsType=" + this.lmsType
                + ", lmsAuthName=" + this.lmsAuthName + ", lmsAuthSecret=" + this.lmsAuthSecret + ", lmsApiUrl="
                + this.lmsApiUrl
                + ", lmsRestApiToken=" + this.lmsRestApiToken + ", sebAuthName=" + this.sebAuthName + ", sebAuthSecret="
                + this.sebAuthSecret + "]";
    }

}
