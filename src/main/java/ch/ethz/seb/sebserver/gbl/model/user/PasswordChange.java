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

    public static final String ATTR_NAME_PASSWORD = "password";
    public static final String ATTR_NAME_NEW_PASSWORD = "newPassword";
    public static final String ATTR_NAME_CONFIRM_NEW_PASSWORD = "confirmNewPassword";

    @NotNull
    @JsonProperty(USER.ATTR_UUID)
    public final String userId;

    @NotNull(message = "user:password:notNull")
    @JsonProperty(ATTR_NAME_PASSWORD)
    private final String password;

    @NotNull(message = "user:newPassword:notNull")
    @Size(min = 8, max = 255, message = "user:newPassword:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @NotNull(message = "user:confirmNewPassword:notNull")
    @JsonProperty(ATTR_NAME_CONFIRM_NEW_PASSWORD)
    private final String confirmNewPassword;

    @JsonCreator
    public PasswordChange(
            @JsonProperty(USER.ATTR_UUID) final String userId,
            @JsonProperty(ATTR_NAME_PASSWORD) final String password,
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(ATTR_NAME_CONFIRM_NEW_PASSWORD) final String confirmNewPassword) {

        this.userId = userId;
        this.password = password;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    public String getPassword() {
        return this.password;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public String getConfirmNewPassword() {
        return this.confirmNewPassword;
    }

    public boolean newPasswordMatch() {
        return this.newPassword.equals(this.confirmNewPassword);
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
