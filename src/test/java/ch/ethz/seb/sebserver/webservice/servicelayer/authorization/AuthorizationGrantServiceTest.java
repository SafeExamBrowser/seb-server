/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import static org.junit.Assert.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public class AuthorizationGrantServiceTest {

    @Mock
    private Principal principal;

    @Test
    public void testInit() {
        try {
            final AuthorizationGrantService service = getTestServiceWithUserWithRoles();
            fail("Error expected here, user with no roles makes no sense");
        } catch (final Exception e) {

        }

        final AuthorizationGrantService service = getTestServiceWithUserWithRoles(UserRole.SEB_SERVER_ADMIN);
    }

    @Test
    public void testInstitutionGrantsForSEB_SERVER_ADMIN() {
        final AuthorizationGrantService service = getTestServiceWithUserWithRoles(UserRole.SEB_SERVER_ADMIN);

        assertTrue(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.READ_ONLY, this.principal));
        assertTrue(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.MODIFY, this.principal));
        assertTrue(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.WRITE, this.principal));

        final GrantEntity institution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertTrue(service.hasGrant(institution, GrantType.READ_ONLY, this.principal));
        assertTrue(service.hasGrant(institution, GrantType.MODIFY, this.principal));
        assertTrue(service.hasGrant(institution, GrantType.WRITE, this.principal));
    }

    @Test
    public void testInstitutionGrantsForINSTITUTIONAL_ADMIN() {
        final AuthorizationGrantService service = getTestServiceWithUserWithRoles(UserRole.INSTITUTIONAL_ADMIN);

        assertFalse(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.READ_ONLY, this.principal));
        assertFalse(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.MODIFY, this.principal));
        assertFalse(service.hasTypeGrant(EntityType.INSTITUTION, GrantType.WRITE, this.principal));

        final GrantEntity ownInstitution = entityOf(EntityType.INSTITUTION, 1L, "");

        assertTrue(service.hasGrant(ownInstitution, GrantType.READ_ONLY, this.principal));
        assertTrue(service.hasGrant(ownInstitution, GrantType.MODIFY, this.principal));
        assertFalse(service.hasGrant(ownInstitution, GrantType.WRITE, this.principal));

        final GrantEntity otherInstitution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertFalse(service.hasGrant(otherInstitution, GrantType.READ_ONLY, this.principal));
        assertFalse(service.hasGrant(otherInstitution, GrantType.MODIFY, this.principal));
        assertFalse(service.hasGrant(otherInstitution, GrantType.WRITE, this.principal));
    }

    private SEBServerUser getUser(final UserRole... roles) {
        final UserInfo userInfo = new UserInfo("test", 1L, "test", "test", "mail", true, Locale.ENGLISH,
                DateTimeZone.UTC,
                roles != null
                        ? new HashSet<>(Arrays.asList(roles).stream().map(r -> r.name()).collect(Collectors.toList()))
                        : Collections.emptySet());
        return new SEBServerUser(0L, userInfo, "");
    }

    private GrantEntity entityOf(final EntityType type, final Long instId, final String owner) {
        return new GrantEntity() {

            @Override
            public EntityType entityType() {
                return type;
            }

            @Override
            public Long institutionId() {
                return instId;
            }

            @Override
            public String ownerUUID() {
                return owner;
            }

        };
    }

    private AuthorizationGrantService getTestServiceWithUserWithRoles(final UserRole... roles) {
        final SEBServerUser user = getUser(roles);
        final CurrentUserService currentUserServiceMock = Mockito.mock(CurrentUserService.class);
        Mockito.when(currentUserServiceMock.extractFromPrincipal(this.principal)).thenReturn(user);

        final AuthorizationGrantService authorizationGrantService = new AuthorizationGrantService(
                Collections.emptyList(),
                currentUserServiceMock);
        authorizationGrantService.init();
        return authorizationGrantService;
    }

}
