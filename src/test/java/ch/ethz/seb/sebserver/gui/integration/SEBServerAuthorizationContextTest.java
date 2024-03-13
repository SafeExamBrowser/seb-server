/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;

public class SEBServerAuthorizationContextTest extends GuiIntegrationTest {

    @Test
    public void testLoginLogoutAsSEBAdmin() {

        final SEBServerAuthorizationContext authorizationContext = getAuthorizationContext();

        assertTrue(authorizationContext.login("admin", "admin"));
        assertTrue(authorizationContext.isLoggedIn());
        final Result<UserInfo> loggedInUser = authorizationContext.getLoggedInUser();
        assertFalse(loggedInUser.hasError());
        final UserInfo userInfo = loggedInUser.get();
        assertEquals("user1", userInfo.uuid);

        assertTrue(authorizationContext.logout());
        assertFalse(authorizationContext.isLoggedIn());
    }

}
