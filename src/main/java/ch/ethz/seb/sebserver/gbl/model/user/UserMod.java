/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class UserMod implements GrantEntity {

    public static final String ATTR_NAME_USER_INFO = "userInfo";
    public static final String ATTR_NAME_NEW_PASSWORD = "newPassword";
    public static final String ATTR_NAME_RETYPED_NEW_PASSWORD = "retypedNewPassword";

    @JsonProperty(ATTR_NAME_USER_INFO)
    private final UserInfo userInfo;

    @Size(min = 8, max = 255, message = "userInfo:password:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD)
    private final String retypedNewPassword;

    @JsonCreator
    public UserMod(
            @JsonProperty(ATTR_NAME_USER_INFO) final UserInfo userInfo,
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD) final String retypedNewPassword) {

        this.userInfo = userInfo;
        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return this.userInfo.getId();
    }

    @Override
    @JsonIgnore
    public EntityType entityType() {
        return this.userInfo.entityType();
    }

    @Override
    @JsonIgnore
    public Long getInstitutionId() {
        return this.userInfo.getInstitutionId();
    }

    @Override
    @JsonIgnore
    public String getOwnerUUID() {
        return this.userInfo.getOwnerUUID();
    }

    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public String getRetypedNewPassword() {
        return this.retypedNewPassword;
    }

    public boolean passwordChangeRequest() {
        return this.newPassword != null;
    }

    public boolean newPasswordMatch() {
        return passwordChangeRequest() && this.newPassword.equals(this.retypedNewPassword);
    }

    public boolean createNew() {
        return this.userInfo.uuid == null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.userInfo == null) ? 0 : this.userInfo.hashCode());
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
        final UserMod other = (UserMod) obj;
        if (this.userInfo == null) {
            if (other.userInfo != null)
                return false;
        } else if (!this.userInfo.equals(other.userInfo))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UserMod [userInfo=" + this.userInfo + "]";
    }
}
