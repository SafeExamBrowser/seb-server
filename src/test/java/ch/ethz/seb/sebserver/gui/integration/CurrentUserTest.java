/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.OAuth2AuthorizationContextHolder;

public class CurrentUserTest extends GuiIntegrationTest {

    @Test
    public void testCurrentUserLoginAndGet() {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = getAuthorizationContextHolder();

        final CurrentUser currentUser = new CurrentUser(authorizationContextHolder);

        // no user is logged in for now
        assertNull(currentUser.get());

        // login as SEB Administrator
        authorizationContextHolder.getAuthorizationContext().login("admin", "admin");
        final UserInfo userInfo = currentUser.getOrHandleError(error -> {
            fail("expecting no error here");
            return null;
        });
        assertNotNull(userInfo);
        assertEquals("user1", userInfo.uuid);
    }

    @Test
    public void testCurrentUserPrivileges() {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = getAuthorizationContextHolder();

        final CurrentUser currentUser = new CurrentUser(authorizationContextHolder);
        // login as SEB Administrator
        authorizationContextHolder.getAuthorizationContext().login("admin", "admin");

        assertTrue(currentUser.hasBasePrivilege(PrivilegeType.READ, EntityType.INSTITUTION));
        assertTrue(currentUser.hasPrivilege(PrivilegeType.WRITE, currentUser.get()));
        assertTrue(currentUser.hasBasePrivilege(PrivilegeType.WRITE, EntityType.INSTITUTION));
        assertTrue(currentUser.hasInstitutionalPrivilege(PrivilegeType.MODIFY, EntityType.INSTITUTION));
    }

    @Test
    public void testCurrentUserLogin() {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = login("admin", "admin");
        final CurrentUser currentUser = new CurrentUser(authorizationContextHolder);
        final UserInfo userInfo = currentUser.getOrHandleError(error -> {
            fail("expecting no error here");
            return null;
        });
        assertNotNull(userInfo);
        assertEquals("user1", userInfo.uuid);
    }

}
