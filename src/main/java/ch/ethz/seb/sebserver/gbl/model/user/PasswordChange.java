/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
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

    @NotEmpty(message = "user:password:notNull")
    @JsonProperty(ATTR_NAME_PASSWORD)
    private final CharSequence password;

    @NotEmpty(message = "user:newPassword:notNull")
    @Size(min = 8, max = 255, message = "user:newPassword:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final CharSequence newPassword;

    @NotEmpty(message = "user:confirmNewPassword:notNull")
    @JsonProperty(ATTR_NAME_CONFIRM_NEW_PASSWORD)
    private final CharSequence confirmNewPassword;

    @JsonCreator
    public PasswordChange(
            @JsonProperty(USER.ATTR_UUID) final String userId,
            @JsonProperty(ATTR_NAME_PASSWORD) final CharSequence password,
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final CharSequence newPassword,
            @JsonProperty(ATTR_NAME_CONFIRM_NEW_PASSWORD) final CharSequence confirmNewPassword) {

        this.userId = userId;
        this.password = password;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    public CharSequence getPassword() {
        return this.password;
    }

    public CharSequence getNewPassword() {
        return this.newPassword;
    }

    public CharSequence getConfirmNewPassword() {
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

    @Override
    public Entity printSecureCopy() {
        return new PasswordChange(
                this.userId,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE);
    }

}
