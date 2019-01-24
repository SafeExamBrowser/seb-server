/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain.USER;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ROLE;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class UserMod implements GrantEntity {

    public static final String ATTR_NAME_NEW_PASSWORD = "newPassword";
    public static final String ATTR_NAME_RETYPED_NEW_PASSWORD = "retypedNewPassword";

    public final String uuid;

    /** The foreign key identifier to the institution where the User belongs to */
    @NotNull
    @JsonProperty(USER.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    /** Full name of the user */
    @NotNull
    @Size(min = 3, max = 255, message = "user:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_NAME)
    public final String name;

    /** The internal user name */
    @Size(min = 3, max = 255, message = "user:username:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_USERNAME)
    public final String username;

    /** E-mail address of the user */
    @Email(message = "user:email:email:_:_:${validatedValue}")
    @JsonProperty(USER.ATTR_EMAIL)
    public final String email;

    /** The users locale */
    @JsonProperty(USER.ATTR_LOCALE)
    public final Locale locale;

    /** The users time zone */
    @JsonProperty(USER.ATTR_TIMEZONE)
    public final DateTimeZone timeZone;

    /** The users roles in a unmodifiable set */
    @JsonProperty(USER_ROLE.REFERENCE_NAME)
    public final Set<String> roles;

    @Size(min = 8, max = 255, message = "user:password:size:{min}:{max}:${validatedValue}")
    @JsonProperty(ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD)
    private final String retypedNewPassword;

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public UserMod(
            @JsonProperty(USER.ATTR_UUID) final String uuid,
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(ATTR_NAME_RETYPED_NEW_PASSWORD) final String retypedNewPassword,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(USER.ATTR_LOCALE) final Locale locale,
            @JsonProperty(USER.ATTR_TIMEZONE) final DateTimeZone timeZone,
            @JsonProperty(USER_ROLE.REFERENCE_NAME) final Set<String> roles) {

        this.uuid = uuid;
        this.institutionId = institutionId;
        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
        this.name = name;
        this.username = username;
        this.email = email;
        this.locale = (locale != null) ? locale : Locale.ROOT;
        this.timeZone = (timeZone != null) ? timeZone : DateTimeZone.UTC;
        this.roles = (roles != null)
                ? Collections.unmodifiableSet(roles)
                : Collections.emptySet();
    }

    public UserMod(final UserInfo userInfo, final String newPassword, final String retypedNewPassword) {
        this.uuid = userInfo.uuid;
        this.institutionId = userInfo.institutionId;
        this.newPassword = newPassword;
        this.retypedNewPassword = retypedNewPassword;
        this.name = userInfo.name;
        this.username = userInfo.username;
        this.email = userInfo.email;
        this.locale = userInfo.locale;
        this.timeZone = userInfo.timeZone;
        this.roles = userInfo.roles;
    }

    @Override
    public String getModelId() {
        return this.uuid;
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return this.uuid;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    public Set<String> getRoles() {
        return this.roles;
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

    @Override
    public String toString() {
        return "UserMod [uuid=" + this.uuid + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", username="
                + this.username + ", email=" + this.email + ", locale=" + this.locale + ", timeZone=" + this.timeZone
                + ", roles=" + this.roles
                + ", newPassword=" + this.newPassword + ", retypedNewPassword=" + this.retypedNewPassword + "]";
    }

}
