/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@GuiProfile
public class CurrentUser {

    private static final Logger log = LoggerFactory.getLogger(CurrentUser.class);

    private final AuthorizationContextHolder authorizationContextHolder;
    private SEBServerAuthorizationContext authContext = null;

    public CurrentUser(final AuthorizationContextHolder authorizationContextHolder) {
        this.authorizationContextHolder = authorizationContextHolder;
    }

    public UserInfo get() {
        if (isAvailable()) {
            return this.authContext.getLoggedInUser();
        }

        log.warn("Current user requested but no user is currently logged in");

        return null;
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

}
