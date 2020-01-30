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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.AuthorizationServiceImpl;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.UserServiceImpl;

public class AuthorizationServiceTest {

    @Test
    public void testInstitutionGrantForSEB_SERVER_ADMIN() {
        final AuthorizationServiceImpl service = getTestServiceWithUserWithRoles(UserRole.SEB_SERVER_ADMIN);

        assertTrue(service.hasGrant(PrivilegeType.READ, EntityType.INSTITUTION));
        assertTrue(service.hasGrant(PrivilegeType.MODIFY, EntityType.INSTITUTION));
        assertTrue(service.hasGrant(PrivilegeType.WRITE, EntityType.INSTITUTION));

        final GrantEntity institution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertTrue(service.hasReadGrant(institution));
        assertTrue(service.hasModifyGrant(institution));
        assertTrue(service.hasWriteGrant(institution));
    }

    @Test
    public void testInstitutionGrantsForINSTITUTIONAL_ADMIN() {
        final AuthorizationServiceImpl service = getTestServiceWithUserWithRoles(UserRole.INSTITUTIONAL_ADMIN);

        assertFalse(service.hasGrant(PrivilegeType.READ, EntityType.INSTITUTION));
        assertFalse(service.hasGrant(PrivilegeType.MODIFY, EntityType.INSTITUTION));
        assertFalse(service.hasGrant(PrivilegeType.WRITE, EntityType.INSTITUTION));

        final GrantEntity ownInstitution = entityOf(EntityType.INSTITUTION, 1L, "");

        assertTrue(service.hasReadGrant(ownInstitution));
        assertTrue(service.hasModifyGrant(ownInstitution));
        assertFalse(service.hasWriteGrant(ownInstitution));

        final GrantEntity otherInstitution = entityOf(EntityType.INSTITUTION, 2L, "");

        assertFalse(service.hasReadGrant(otherInstitution));
        assertFalse(service.hasModifyGrant(otherInstitution));
        assertFalse(service.hasWriteGrant(otherInstitution));
    }

    private SEBServerUser getUser(final UserRole... roles) {
        final UserInfo userInfo = new UserInfo("test", 1L, "test", "", "test", "mail", true, Locale.ENGLISH,
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

    private AuthorizationServiceImpl getTestServiceWithUserWithRoles(final UserRole... roles) {
        final SEBServerUser user = getUser(roles);
        final UserServiceImpl currentUserServiceMock = Mockito.mock(UserServiceImpl.class);
        Mockito.when(currentUserServiceMock.getCurrentUser()).thenReturn(user);

        final AuthorizationServiceImpl authorizationGrantService = new AuthorizationServiceImpl(
                currentUserServiceMock);
        authorizationGrantService.init();
        return authorizationGrantService;
    }

}
