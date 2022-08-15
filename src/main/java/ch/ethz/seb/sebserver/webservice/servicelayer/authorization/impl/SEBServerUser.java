/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

/** SEBServerUser defines web-service internal user-account based authentication principal
 *
 * This implements Spring's UserDetails and CredentialsContainer to act as a principal
 * within internal authentication and authorization processes. */
public final class SEBServerUser implements UserDetails, CredentialsContainer, Authentication {

    private static final long serialVersionUID = 5726250141482925769L;

    private final Long id;
    private final UserInfo userInfo;
    private String password;
    private final Collection<GrantedAuthority> authorities;
    private final EnumSet<UserRole> userRoles;

    public SEBServerUser(final Long id, final UserInfo userInfo, final String password) {
        this.id = id;
        this.userInfo = userInfo;
        this.password = password;
        this.authorities = Collections.unmodifiableList(
                userInfo.roles
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(role))
                        .collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(userInfo.roles)) {
            this.userRoles = EnumSet.noneOf(UserRole.class);
        } else {
            this.userRoles = EnumSet.copyOf(userInfo.roles
                    .stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toSet()));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.userInfo.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.userInfo.active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.userInfo.active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.userInfo.active;
    }

    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    public EnumSet<UserRole> getUserRoles() {
        return this.userRoles;
    }

    public Long institutionId() {
        return this.userInfo.institutionId;
    }

    public String uuid() {
        return this.userInfo.uuid;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.authorities == null) ? 0 : this.authorities.hashCode());
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
        final SEBServerUser other = (SEBServerUser) obj;
        if (this.authorities == null) {
            if (other.authorities != null)
                return false;
        } else if (!this.authorities.equals(other.authorities))
            return false;
        if (this.userInfo == null) {
            if (other.userInfo != null)
                return false;
        } else if (!this.userInfo.equals(other.userInfo))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SEBServerUser [user=" + this.userInfo + "]";
    }

    /** Use this to make a copy of a given SEBServerUser instance.
     *
     * @param user given SEBServerUser instance to make a copy of
     * @return return copied SEBServerUser instance */
    public static SEBServerUser of(final SEBServerUser user) {
        return new SEBServerUser(user.id, UserInfo.of(user.userInfo), user.password);
    }

    @Override
    public String getName() {
        return this.userInfo.username;
    }

    @Override
    public Object getCredentials() {
        return this;
    }

    @Override
    public Object getDetails() {
        return this;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return isEnabled();
    }

    @Override
    public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {

    }

}
