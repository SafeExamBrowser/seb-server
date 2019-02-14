/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.authorization.Privilege.RoleTypeKey;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@GuiProfile
public class CurrentUser {

    private static final Logger log = LoggerFactory.getLogger(CurrentUser.class);

    private final AuthorizationContextHolder authorizationContextHolder;
    private SEBServerAuthorizationContext authContext = null;
    private Map<RoleTypeKey, Privilege> privileges = null;

    public CurrentUser(final AuthorizationContextHolder authorizationContextHolder) {
        this.authorizationContextHolder = authorizationContextHolder;
    }

    public UserInfo get() {
        if (isAvailable()) {
            return this.authContext
                    .getLoggedInUser()
                    .getOrThrow();
        }

        log.warn("Current user requested but no user is currently logged in");

        return null;
    }

    public UserInfo getOrHandleError(final Function<Throwable, UserInfo> errorHandler) {
        if (isAvailable()) {
            return this.authContext
                    .getLoggedInUser()
                    .get(errorHandler);
        }

        log.warn("Current user requested but no user is currently logged in");

        return null;
    }

    public boolean hasPrivilege(final PrivilegeType privilegeType, final EntityType entityType) {
        if (loadPrivileges()) {
            try {
                final UserInfo userInfo = get();
                return userInfo.getRoles()
                        .stream()
                        .map(roleName -> UserRole.valueOf(roleName))
                        .map(role -> new RoleTypeKey(entityType, role))
                        .map(key -> this.privileges.get(key))
                        .filter(priv -> (priv != null) && priv.hasGrant(
                                userInfo.uuid,
                                userInfo.institutionId,
                                privilegeType,
                                null, null))
                        .findFirst()
                        .isPresent();
            } catch (final Exception e) {
                log.error("Failed to verify privilege: PrivilegeType {} EntityType {}",
                        privilegeType, entityType, e);
            }
        }

        return false;
    }

    public boolean hasPrivilege(
            final PrivilegeType privilegeType,
            final GrantEntity grantEntity) {

        if (loadPrivileges()) {
            final EntityType entityType = grantEntity.entityType();
            try {
                final UserInfo userInfo = get();
                return userInfo.getRoles()
                        .stream()
                        .map(roleName -> UserRole.valueOf(roleName))
                        .map(role -> new RoleTypeKey(entityType, role))
                        .map(key -> this.privileges.get(key))
                        .filter(priv -> (priv != null) && priv.hasGrant(
                                userInfo.uuid,
                                userInfo.institutionId,
                                privilegeType,
                                grantEntity.getInstitutionId(),
                                grantEntity.getOwnerId()))
                        .findFirst()
                        .isPresent();
            } catch (final Exception e) {
                log.error("Failed to verify privilege: PrivilegeType {} EntityType {}",
                        privilegeType, entityType, e);
            }
        }

        return false;
    }

    public boolean isAvailable() {
        updateContext();
        return this.authContext != null && this.authContext.isLoggedIn();
    }

    private void updateContext() {
        if (this.authContext == null || !this.authContext.isValid()) {
            this.authContext = this.authorizationContextHolder.getAuthorizationContext();
        }
    }

    private boolean loadPrivileges() {
        if (this.privileges != null) {
            return true;
        }

        updateContext();
        if (this.authContext != null) {
            try {
                final WebserviceURIService webserviceURIService =
                        this.authorizationContextHolder.getWebserviceURIService();
                final ResponseEntity<Collection<Privilege>> exchange = this.authContext.getRestTemplate()
                        .exchange(
                                webserviceURIService.getURIBuilder()
                                        .path(API.PRIVILEGES_ENDPOINT)
                                        .toUriString(),
                                HttpMethod.GET,
                                HttpEntity.EMPTY,
                                new ParameterizedTypeReference<Collection<Privilege>>() {
                                });

                if (exchange.getStatusCodeValue() == HttpStatus.OK.value()) {
                    this.privileges = exchange.getBody().stream()
                            .reduce(new HashMap<RoleTypeKey, Privilege>(),
                                    (map, priv) -> {
                                        map.put(priv.roleTypeKey, priv);
                                        return map;
                                    },
                                    (map1, map2) -> {
                                        map1.putAll(map2);
                                        return map1;
                                    });

                    return true;
                } else {
                    log.error("Failed to get Privileges from webservice API: {}", exchange);
                    return false;
                }

            } catch (final Exception e) {
                log.error("Failed to get Privileges from webservice API: ", e);
                return false;
            }
        } else {
            log.error("Failed to get Privileges from webservice API. No AuthorizationContext available");
            return false;
        }
    }

}
