/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public class AuthorizationGrantServiceTest {

    @Mock
    private Principal principal;

    @Test
    public void testInstitutionGrantForSEB_SERVER_ADMIN() {
        final AuthorizationGrantServiceImpl service = getTestServiceWithUserWithRoles(UserRole.SEB_SERVER_ADMIN);

        assertTrue(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.READ_ONLY, this.principal));
        assertTrue(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.MODIFY, this.principal));
        assertTrue(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.WRITE, this.principal));

        final GrantEntity institution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertTrue(service.hasGrant(institution, PrivilegeType.READ_ONLY, this.principal));
        assertTrue(service.hasGrant(institution, PrivilegeType.MODIFY, this.principal));
        assertTrue(service.hasGrant(institution, PrivilegeType.WRITE, this.principal));
    }

    @Test
    public void testInstitutionGrantsForINSTITUTIONAL_ADMIN() {
        final AuthorizationGrantServiceImpl service = getTestServiceWithUserWithRoles(UserRole.INSTITUTIONAL_ADMIN);

        assertFalse(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.READ_ONLY, this.principal));
        assertFalse(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.MODIFY, this.principal));
        assertFalse(service.hasBasePrivilege(EntityType.INSTITUTION, PrivilegeType.WRITE, this.principal));

        final GrantEntity ownInstitution = entityOf(EntityType.INSTITUTION, 1L, "");

        assertTrue(service.hasGrant(ownInstitution, PrivilegeType.READ_ONLY, this.principal));
        assertTrue(service.hasGrant(ownInstitution, PrivilegeType.MODIFY, this.principal));
        assertFalse(service.hasGrant(ownInstitution, PrivilegeType.WRITE, this.principal));

        final GrantEntity otherInstitution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertFalse(service.hasGrant(otherInstitution, PrivilegeType.READ_ONLY, this.principal));
        assertFalse(service.hasGrant(otherInstitution, PrivilegeType.MODIFY, this.principal));
        assertFalse(service.hasGrant(otherInstitution, PrivilegeType.WRITE, this.principal));
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
            public Long getInstitutionId() {
                return instId;
            }

            @Override
            public String getOwnerId() {
                return owner;
            }

            @Override
            public String getModelId() {
                return "1";
            }

            @Override
            public String getName() {
                return getModelId();
            }

        };
    }

    private AuthorizationGrantServiceImpl getTestServiceWithUserWithRoles(final UserRole... roles) {
        final SEBServerUser user = getUser(roles);
        final UserServiceImpl currentUserServiceMock = Mockito.mock(UserServiceImpl.class);
        Mockito.when(currentUserServiceMock.extractFromPrincipal(this.principal)).thenReturn(user);

        final AuthorizationGrantServiceImpl authorizationGrantService = new AuthorizationGrantServiceImpl(
                Collections.emptyList(),
                currentUserServiceMock);
        authorizationGrantService.init();
        return authorizationGrantService;
    }

}
