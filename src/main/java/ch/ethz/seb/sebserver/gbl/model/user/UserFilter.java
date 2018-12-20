/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain.USER;

@JsonInclude(Include.NON_NULL)
public final class UserFilter {

    @JsonProperty(USER.ATTR_ACTIVE)
    public final Boolean active;
    @JsonProperty(USER.ATTR_INSTITUTION_ID)
    public final Long institutionId;
    @JsonProperty(USER.ATTR_NAME)
    public final String name;
    @JsonProperty(USER.ATTR_USERNAME)
    public final String username;
    @JsonProperty(USER.ATTR_EMAIL)
    public final String email;
    @JsonProperty(USER.ATTR_LOCALE)
    public final String locale;

    public UserFilter(
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(USER.ATTR_LOCALE) final String locale) {

        this.institutionId = institutionId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.active = (active != null) ? active : true;
        this.locale = locale;
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public String getName() {
        return this.name;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public Boolean getActive() {
        return this.active;
    }

    public String getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return "UserFilter [institutionId=" + this.institutionId + ", name=" + this.name + ", username=" + this.username
                + ", email="
                + this.email + ", active=" + this.active + ", locale=" + this.locale + "]";
    }

}
