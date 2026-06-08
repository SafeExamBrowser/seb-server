/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "UserMod", description = "User account creation and self-registration form model.")
public final class UserMod implements UserAccount {

    @Schema(description = "User UUID. Omit this for new user creation.", nullable = true)
    @JsonProperty(USER.ATTR_UUID)
    public final String uuid;

    /** The foreign key identifier to the institution where the User belongs to */
    @Schema(description = "Institution identifier the user belongs to.", example = "1")
    @NotNull(message = "user:institutionId:notNull")
    @JsonProperty(USER.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    /** first (or full) name of the user */
    @Schema(description = "First name of the user.", example = "Ada")
    @NotNull(message = "user:name:notNull")
    @Size(max = 255, message = "user:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_NAME)
    public final String name;

    /** surname of the user */
    @Schema(description = "Surname of the user.", example = "Lovelace")
    @NotNull(message = "user:surname:notNull")
    @Size(max = 255, message = "user:surname:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_SURNAME)
    public final String surname;

    /** The internal user name */
    @Schema(description = "Login username.", example = "ada.lovelace")
    @NotNull(message = "user:username:notNull")
    @Size(min = 3, max = 255, message = "user:username:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_USERNAME)
    public final String username;

    /** E-mail address of the user */
    @Schema(description = "User email address.", format = "email", example = "ada@example.org", nullable = true)
    @Email(message = "user:email:email:_:_:${validatedValue}")
    @JsonProperty(USER.ATTR_EMAIL)
    public final String email;

    /** The users locale */
    @Schema(description = "User locale.", type = "string", example = "en")
    @NotNull(message = "user:language:notNull")
    @JsonProperty(USER.ATTR_LANGUAGE)
    public final Locale language;

    /** The users time zone */
    @Schema(description = "User time zone.", type = "string", example = "Europe/Zurich")
    @NotNull(message = "user:timeZone:notNull")
    @JsonProperty(USER.ATTR_TIMEZONE)
    public final DateTimeZone timeZone;

    /** The users roles in a unmodifiable set */
    @ArraySchema(
            arraySchema = @Schema(description = "Roles to assign to the user."),
            schema = @Schema(
                    type = "string",
                    allowableValues = {
                            "SEB_SERVER_ADMIN",
                            "INSTITUTIONAL_ADMIN",
                            "EXAM_ADMIN",
                            "EXAM_SUPPORTER",
                            "TEACHER" }))
    @NotNull(message = "user:userRoles:notNull")
    @NotEmpty(message = "user:userRoles:notNull")
    @JsonProperty(USER_ROLE.REFERENCE_NAME)
    public final Set<String> roles;

    @Schema(description = "New account password.", type = "string", format = "password", minLength = 8, maxLength = 255)
    @NotNull(message = "user:newPassword:notNull")
    @Size(min = 8, max = 255, message = "user:newPassword:size:{min}:{max}:${validatedValue}")
    @JsonProperty(PasswordChange.ATTR_NAME_NEW_PASSWORD)
    private final CharSequence newPassword;

    @Schema(description = "Repeated new account password.", type = "string", format = "password")
    @NotNull(message = "user:confirmNewPassword:notNull")
    @JsonProperty(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD)
    private final CharSequence confirmNewPassword;

    @Schema(description = "Whether this is a local SEB Server account.", example = "true")
    @JsonProperty(USER.ATTR_LOCAL_ACCOUNT)
    private final Boolean isLocalAccount;

    @Schema(description = "Whether direct login is enabled for this account.", example = "true")
    @JsonProperty(USER.ATTR_DIRECT_LOGIN)
    private final Boolean hasDirectLogin;

    @JsonCreator
    public UserMod(
            @JsonProperty(USER.ATTR_UUID) final String uuid,
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_SURNAME) final String surname,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(PasswordChange.ATTR_NAME_NEW_PASSWORD) final CharSequence newPassword,
            @JsonProperty(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD) final CharSequence confirmNewPassword,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_LANGUAGE) final Locale language,
            @JsonProperty(USER.ATTR_TIMEZONE) final DateTimeZone timeZone,
            @JsonProperty(USER.ATTR_LOCAL_ACCOUNT) final Boolean isLocalAccount,
            @JsonProperty(USER.ATTR_DIRECT_LOGIN) final Boolean hasDirectLogin,
            @JsonProperty(USER_ROLE.REFERENCE_NAME) final Set<String> roles) {

        this.uuid = uuid;
        this.institutionId = institutionId;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.email = email;
        this.language = (language != null) ? language : Locale.ENGLISH;
        this.timeZone = (timeZone != null) ? timeZone : DateTimeZone.UTC;
        this.isLocalAccount = BooleanUtils.isNotFalse(isLocalAccount);
        this.hasDirectLogin = BooleanUtils.isNotFalse(hasDirectLogin);
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
        this.surname = postAttrMapper.getString(USER.ATTR_SURNAME);
        this.username = postAttrMapper.getString(USER.ATTR_USERNAME);
        this.email = postAttrMapper.getString(USER.ATTR_EMAIL);
        this.language = postAttrMapper.getLocale(USER.ATTR_LANGUAGE);
        this.timeZone = postAttrMapper.getDateTimeZone(USER.ATTR_TIMEZONE);
        this.roles = postAttrMapper.getStringSet(USER_ROLE.REFERENCE_NAME);
        this.isLocalAccount = BooleanUtils.isNotFalse(postAttrMapper.getBooleanObject(USER.ATTR_LOCAL_ACCOUNT));
        this.hasDirectLogin = BooleanUtils.isNotFalse(postAttrMapper.getBooleanObject(USER.ATTR_DIRECT_LOGIN));
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
    @JsonIgnore
    @Schema(hidden = true)
    public DateTime getCreationDate() {
        return null;
    }

    @Override
    public String getOwnerId() {
        return this.uuid;
    }

    public CharSequence getNewPassword() {
        return this.newPassword;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSurname() {
        return this.surname;
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
        final List<UserRole> roles = getRoles()
                .stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            return EnumSet.noneOf(UserRole.class);
        }
        return EnumSet.copyOf(roles);
    }

    @Override
    @JsonIgnore
    public boolean isOnlyTeacher() {
        return this.roles.contains(UserRole.TEACHER.name()) && this.roles.size() == 1;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public CharSequence getRetypedNewPassword() {
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


    public Boolean isLocalAccount() {
        return isLocalAccount;
    }

    public Boolean hasDirectLogin() {
        return hasDirectLogin;
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
        final StringBuilder builder = new StringBuilder();
        builder.append("UserMod [uuid=");
        builder.append(this.uuid);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", surname=");
        builder.append(this.surname);
        builder.append(", username=");
        builder.append(this.username);
        builder.append(", email=");
        builder.append(this.email);
        builder.append(", language=");
        builder.append(this.language);
        builder.append(", timeZone=");
        builder.append(this.timeZone);
        builder.append(", roles=");
        builder.append(this.roles);
        builder.append(", newPassword=");
        builder.append(this.newPassword);
        builder.append(", confirmNewPassword=");
        builder.append(this.confirmNewPassword);
        builder.append("]");
        return builder.toString();
    }

    public static UserMod createNew(final Long institutionId) {
        return new UserMod(
                UUID.randomUUID().toString(),
                institutionId,
                null, null, null, null, null, null, null, null, true, true, null);
    }

}
