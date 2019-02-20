/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordChange {

    public static final String ATTR_NAME_NEW_PASSWORD = "newPassword";
    public static final String ATTR_NAME_RETYPED_NEW_PASSWORD = "retypedNewPassword";

    @NotNull(message = "user:password:notNull")
    @Size(min = 8, max = 255, message = "user:password:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD)
    private final String retypedNewPassword;

    @JsonCreator
    public PasswordChange(
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD) final String retypedNewPassword) {

        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public String getRetypedNewPassword() {
        return this.retypedNewPassword;
    }

    public boolean newPasswordMatch() {
        return this.newPassword.equals(this.retypedNewPassword);
    }

}
