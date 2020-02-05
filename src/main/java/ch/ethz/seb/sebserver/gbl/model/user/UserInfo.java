/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ROLE;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** The user info domain model contains primary user information
 *
 * This domain model is annotated and fully serializable and deserializable
 * to and from JSON within the Jackson library.
 *
 * This domain model is immutable and thread-save */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserInfo implements UserAccount, Activatable, Serializable {

    private static final long serialVersionUID = 2526446136264377808L;

    public static final String FILTER_ATTR_USER_NAME = "username";
    public static final String FILTER_ATTR_EMAIL = "email";
    public static final String FILTER_ATTR_LANGUAGE = "language";
    public static final String FILTER_ATTR_ROLE = "role";

    /** The user's UUID */
    @JsonProperty(USER.ATTR_UUID)
    public final String uuid;

    /** The foreign key identifier to the institution where the User belongs to */
    @NotNull
    @JsonProperty(USER.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(USER.ATTR_CREATION_DATE)
    public final DateTime creationDate;

    /** First name of the user */
    @NotNull(message = "user:name:notNull")
    @Size(min = 3, max = 255, message = "user:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_NAME)
    public final String name;

    /** Surname of the user */
    @Size(max = 255, message = "user:surname:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_SURNAME)
    public final String surname;

    /** The internal user name */
    @NotNull(message = "user:username:notNull")
    @Size(min = 3, max = 255, message = "user:username:size:{min}:{max}:${validatedValue}")
    @JsonProperty(USER.ATTR_USERNAME)
    public final String username;

    /** E-mail address of the user */
    @Email(message = "user:email:email:_:_:${validatedValue}")
    @JsonProperty(USER.ATTR_EMAIL)
    public final String email;

    /** Indicates whether this user is still active or not */
    @NotNull
    @JsonProperty(USER.ATTR_ACTIVE)
    public final Boolean active;

    /** The users locale */
    @NotNull(message = "user:language:notNull")
    @JsonProperty(USER.ATTR_LANGUAGE)
    public final Locale language;

    /** The users time zone */
    @NotNull(message = "user:timeZone:notNull")
    @JsonProperty(USER.ATTR_TIMEZONE)
    public final DateTimeZone timeZone;

    /** The users roles in a unmodifiable set. Is never null */
    @NotNull(message = "user:userRoles:notNull")
    @NotEmpty(message = "user:userRoles:notNull")
    @JsonProperty(USER_ROLE.REFERENCE_NAME)
    public final Set<String> roles;

    @JsonCreator
    public UserInfo(
            @JsonProperty(USER.ATTR_UUID) final String uuid,
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_CREATION_DATE) final DateTime creationDate,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_SURNAME) final String surname,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(USER.ATTR_LANGUAGE) final Locale language,
            @JsonProperty(USER.ATTR_TIMEZONE) final DateTimeZone timeZone,
            @JsonProperty(USER_ROLE.REFERENCE_NAME) final Set<String> roles) {

        this.uuid = uuid;
        this.institutionId = institutionId;
        this.creationDate = creationDate;
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.email = email;
        this.active = BooleanUtils.isTrue(active);
        this.language = language;
        this.timeZone = timeZone;
        this.roles = Utils.immutableSetOf(roles);
    }

    @Override
    public EntityType entityType() {
        return EntityType.USER;
    }

    @Override
    public String getModelId() {
        return this.uuid;
    }

    public String getUuid() {
        return this.uuid;
    }

    @Override
    public DateTime getCreationDate() {
        return this.creationDate;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return this.uuid;
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
    public Boolean getActive() {
        return this.active;
    }

    @Override
    public boolean isActive() {
        return this.active;
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

    public boolean hasRole(final UserRole userRole) {
        if (userRole == null) {
            return false;
        }
        return this.roles.contains(userRole.name());
    }

    public boolean hasAnyRole(final UserRole... userRole) {
        if (userRole == null) {
            return false;
        }
        return CollectionUtils.containsAny(getUserRoles(), Arrays.asList(userRole));
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.uuid == null) ? 0 : this.uuid.hashCode());
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
        final UserInfo other = (UserInfo) obj;
        if (this.uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!this.uuid.equals(other.uuid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UserInfo [uuid=");
        builder.append(this.uuid);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", creationDate=");
        builder.append(this.creationDate);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", surname=");
        builder.append(this.surname);
        builder.append(", username=");
        builder.append(this.username);
        builder.append(", email=");
        builder.append(this.email);
        builder.append(", active=");
        builder.append(this.active);
        builder.append(", language=");
        builder.append(this.language);
        builder.append(", timeZone=");
        builder.append(this.timeZone);
        builder.append(", roles=");
        builder.append(this.roles);
        builder.append("]");
        return builder.toString();
    }

    /** Use this to create a copy of a given UserInfo instance.
     *
     * @param userInfo UserInfo instance to copy
     * @return copied UserInfo instance */
    public static final UserInfo of(final UserInfo userInfo) {
        return new UserInfo(
                userInfo.getUuid(),
                userInfo.getInstitutionId(),
                userInfo.creationDate,
                userInfo.getName(),
                userInfo.getUsername(),
                userInfo.getSurname(),
                userInfo.getEmail(),
                userInfo.getActive(),
                userInfo.getLanguage(),
                userInfo.getTimeZone(),
                userInfo.roles);
    }

    /** Use this to create a copy of a given UserInfo by overriding available arguments.
     *
     * @param userInfo UserInfo instance to copy
     * @param name new name or null if the name of given userInfo should be taken
     * @param surname new surname or null if the name of given userInfo should be taken
     * @param username new username or null if the username of given userInfo should be taken
     * @param email new email or null if the email of given userInfo should be taken
     * @param language new language or null if the language of given userInfo should be taken
     * @param timeZone new timeZone or null if the timeZone of given userInfo should be taken
     * @param roles new timeZone or null if the roles of given userInfo should be taken
     * @return copied UserInfo instance with the given attributes */
    public static final UserInfo of(
            final UserInfo userInfo,
            final String name,
            final String username,
            final String surname,
            final String email,
            final Locale language,
            final DateTimeZone timeZone,
            final String... roles) {

        return new UserInfo(
                userInfo.getUuid(),
                userInfo.getInstitutionId(),
                userInfo.creationDate,
                (name != null) ? name : userInfo.getName(),
                (surname != null) ? surname : userInfo.getSurname(),
                (username != null) ? username : userInfo.getUsername(),
                (email != null) ? email : userInfo.getEmail(),
                userInfo.getActive(),
                (language != null) ? language : userInfo.getLanguage(),
                (timeZone != null) ? timeZone : userInfo.getTimeZone(),
                (roles != null) ? new HashSet<>(Arrays.asList(roles)) : userInfo.roles);
    }

    public static final UserInfo withEMail(final UserInfo userInfo, final String email) {
        return of(userInfo, null, null, null, email, null, null, (String[]) null);
    }

    public static final UserInfo withRoles(final UserInfo userInfo, final String... roles) {
        return of(userInfo, null, null, null, null, null, null, roles);
    }
}
