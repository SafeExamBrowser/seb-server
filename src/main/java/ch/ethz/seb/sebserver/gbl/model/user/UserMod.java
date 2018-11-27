/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserMod {

    private final UserInfo userInfo;
    private final String newPassword;
    private final String retypedNewPassword;

    @JsonCreator
    public UserMod(
            @JsonProperty("userInfo") final UserInfo userInfo,
            @JsonProperty("newPassword") final String newPassword,
            @JsonProperty("retypedNewPassword") final String retypedNewPassword) {

        this.userInfo = userInfo;
        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
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
