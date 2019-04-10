/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ROLE;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserMod implements UserAccount {

    @JsonProperty(USER.ATTR_UUID)
    public final String uuid;

    /** The foreign key identifier to the institution where the User belongs to */
    @NotNull
    @JsonProperty(USER.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    /** Full name of the user */
    @NotNull(message = "user:name:notNull")
    @Size(min = 3, max = 255, message = "user:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_NAME)
    public final String name;

    /** The internal user name */
    @NotNull(message = "user:username:notNull")
    @Size(min = 3, max = 255, message = "user:username:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_USERNAME)
    public final String username;

    /** E-mail address of the user */
    @Email(message = "user:email:email:_:_:${validatedValue}")
    @JsonProperty(USER.ATTR_EMAIL)
    public final String email;

    /** The users locale */
    @NotNull(message = "user:language:notNull")
    @JsonProperty(USER.ATTR_LANGUAGE)
    public final Locale language;

    /** The users time zone */
    @NotNull(message = "user:timeZone:notNull")
    @JsonProperty(USER.ATTR_TIMEZONE)
    public final DateTimeZone timeZone;

    /** The users roles in a unmodifiable set */
    @NotNull(message = "user:userRoles:notNull")
    @NotEmpty(message = "user:userRoles:notNull")
    @JsonProperty(USER_ROLE.REFERENCE_NAME)
    public final Set<String> roles;

    @NotNull(message = "user:newPassword:notNull")
    @Size(min = 8, max = 255, message = "user:password:size:{min}:{max}:${validatedValue}")
    @JsonProperty(PasswordChange.ATTR_NAME_NEW_PASSWORD)
    private final String newPassword;

    @NotNull(message = "user:retypedNewPassword:notNull")
    @JsonProperty(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD)
    private final String confirmNewPassword;

    @JsonCreator
    public UserMod(
            @JsonProperty(USER.ATTR_UUID) final String uuid,
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(PasswordChange.ATTR_NAME_NEW_PASSWORD) final String newPassword,
            @JsonProperty(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD) final String confirmNewPassword,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_LANGUAGE) final Locale language,
            @JsonProperty(USER.ATTR_TIMEZONE) final DateTimeZone timeZone,
            @JsonProperty(USER_ROLE.REFERENCE_NAME) final Set<String> roles) {

        this.uuid = uuid;
        this.institutionId = institutionId;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
        this.name = name;
        this.username = username;
        this.email = email;
        this.language = (language != null) ? language : Locale.ENGLISH;
        this.timeZone = (timeZone != null) ? timeZone : DateTimeZone.UTC;
        this.roles = (roles != null)
                ? Collections.unmodifiableSet(roles)
                : Collections.emptySet();
    }

    public UserMod(final String modelId, final POSTMapper postAttrMapper) {
        this.uuid = modelId;
        this.institutionId = postAttrMapper.getLong(USER.ATTR_INSTITUTION_ID);
        this.newPassword = postAttrMapper.getString(PasswordChange.ATTR_NAME_NEW_PASSWORD);
        this.confirmNewPassword = postAttrMapper.getString(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD);
        this.name = postAttrMapper.getString(USER.ATTR_NAME);
        this.username = postAttrMapper.getString(USER.ATTR_USERNAME);
        this.email = postAttrMapper.getString(USER.ATTR_EMAIL);
        this.language = postAttrMapper.getLocale(USER.ATTR_LANGUAGE);
        this.timeZone = postAttrMapper.getDateTimeZone(USER.ATTR_TIMEZONE);
        this.roles = postAttrMapper.getStringSet(USER_ROLE.REFERENCE_NAME);
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

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public Locale getLanguage() {
        return this.language;
    }

    @Override
    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    @Override
    @JsonIgnore
    public EnumSet<UserRole> getUserRoles() {
        return EnumSet.copyOf(
                getRoles().stream()
                        .map(r -> UserRole.valueOf(r))
                        .collect(Collectors.toList()));
    }

    public String getRetypedNewPassword() {
        return this.confirmNewPassword;
    }

    public boolean passwordChangeRequest() {
        return this.newPassword != null;
    }

    public boolean newPasswordMatch() {
        return passwordChangeRequest() && this.newPassword.equals(this.confirmNewPassword);
    }

    @Override
    public Boolean getActive() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @JsonIgnore
    @Override
    public EntityKey getEntityKey() {
        if (StringUtils.isBlank(this.uuid)) {
            return null;
        }
        return new EntityKey(this.uuid, entityType());
    }

    @Override
    public String toString() {
        return "UserMod [uuid=" + this.uuid + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", username="
                + this.username + ", email=" + this.email + ", language=" + this.language + ", timeZone="
                + this.timeZone
                + ", roles=" + this.roles
                + ", newPassword=" + this.newPassword + ", retypedNewPassword=" + this.confirmNewPassword + "]";
    }

    public static UserMod createNew(final Long institutionId) {
        return new UserMod(
                UUID.randomUUID().toString(),
                institutionId,
                null, null, null, null, null, null, null, null);
    }

}
