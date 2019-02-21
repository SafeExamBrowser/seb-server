/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;

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
public final class UserInfo implements UserAccount, Activatable, Serializable {

    private static final long serialVersionUID = 2526446136264377808L;

    public static final String FILTER_ATTR_USER_NAME = "username";
    public static final String FILTER_ATTR_EMAIL = "email";
    public static final String FILTER_ATTR_LOCALE = "locale";

    /** The user's UUID */
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

    /** Indicates whether this user is still active or not */
    @NotNull
    @JsonProperty(USER.ATTR_ACTIVE)
    public final Boolean active;

    /** The users locale */
    @NotNull(message = "user:locale:notNull")
    @JsonProperty(USER.ATTR_LOCALE)
    public final Locale locale;

    /** The users time zone */
    @NotNull(message = "user:timeZone:notNull")
    @JsonProperty(USER.ATTR_TIMEZONE)
    public final DateTimeZone timeZone;

    /** The users roles in a unmodifiable set. Is never null */
    @JsonProperty(USER_ROLE.REFERENCE_NAME)
    public final Set<String> roles;

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public UserInfo(
            @JsonProperty(USER.ATTR_UUID) final String uuid,
            @JsonProperty(USER.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(USER.ATTR_NAME) final String name,
            @JsonProperty(USER.ATTR_USERNAME) final String username,
            @JsonProperty(USER.ATTR_EMAIL) final String email,
            @JsonProperty(USER.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(USER.ATTR_LOCALE) final Locale locale,
            @JsonProperty(USER.ATTR_TIMEZONE) final DateTimeZone timeZone,
            @JsonProperty(USER_ROLE.REFERENCE_NAME) final Set<String> roles) {

        this.uuid = uuid;
        this.institutionId = institutionId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.active = BooleanUtils.isTrue(active);
        this.locale = locale;
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
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    public boolean hasRole(final UserRole userRole) {
        if (userRole == null) {
            return false;
        }
        return this.roles.contains(userRole.name());
    }

    @JsonIgnore
    @Override
    public String getNewPassword() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getRetypedNewPassword() {
        return null;
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
    protected Object clone() throws CloneNotSupportedException {
        return UserInfo.of(this);
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
        return "UserInfo [uuid=" + this.uuid + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", username="
                + this.username + ", email=" + this.email + ", active=" + this.active + ", locale=" + this.locale
                + ", timeZone=" + this.timeZone
                + ", roles=" + this.roles + "]";
    }

    /** Use this to create a copy of a given UserInfo instance.
     *
     * @param userInfo UserInfo instance to copy
     * @return copied UserInfo instance */
    public static final UserInfo of(final UserInfo userInfo) {
        return new UserInfo(
                userInfo.getUuid(),
                userInfo.getInstitutionId(),
                userInfo.getName(),
                userInfo.getUsername(),
                userInfo.getEmail(),
                userInfo.getActive(),
                userInfo.getLocale(),
                userInfo.getTimeZone(),
                userInfo.roles);
    }

}
