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

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER;
import ch.ethz.seb.sebserver.gbl.model.Entity;

public class PasswordChange implements Entity {

    public static final String ATTR_NAME_OLD_PASSWORD = "oldPassword";
    public static final String ATTR_NAME_NEW_PASSWORD = "newPassword";
    public static final String ATTR_NAME_RETYPED_NEW_PASSWORD = "retypedNewPassword";

    @NotNull
    @JsonProperty(USER.ATTR_UUID)
    public final String userId;

    @NotNull(message = "user:password:notNull")
    @JsonProperty(ATTR_NAME_OLD_PASSWORD)
    private final String oldPassword;

    @NotNull(message = "user:password:notNull")
    @Size(min = 8, max = 255, message = "user:newPassword:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD)
    private final String retypedNewPassword;

    @JsonCreator
    public PasswordChange(
            @JsonProperty(USER.ATTR_UUID) final String userId,
            @JsonProperty(ATTR_NAME_OLD_PASSWORD) final String oldPassword,
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD) final String retypedNewPassword) {

        this.userId = userId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
    }

    public String getOldPassword() {
        return this.oldPassword;
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

    @Override
    public String getModelId() {
        return this.userId;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER;
    }

    @Override
    public String getName() {
        return "PasswordChange";
    }

}
