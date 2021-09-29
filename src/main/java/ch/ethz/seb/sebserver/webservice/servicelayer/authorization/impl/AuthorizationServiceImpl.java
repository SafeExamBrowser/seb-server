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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege.RoleTypeKey;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;

@Lazy
@Service
@WebServiceProfile
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserService userService;

    /** Map of role based grants for specified entity types. */
    private final Map<Privilege.RoleTypeKey, Privilege> privileges = new HashMap<>();

    public AuthorizationServiceImpl(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserService getUserService() {
        return this.userService;
    }

    @Override
    public Collection<Privilege> getAllPrivileges() {
        return Collections.unmodifiableCollection(this.privileges.values());
    }

    /** Initialize the (hard-coded) grants */
    @PostConstruct
    public void init() {
        // grants for institution
        addPrivilege(EntityType.INSTITUTION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.MODIFY)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();

        // grants for user account
        addPrivilege(EntityType.USER)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .create();

        // grants for certificates
        addPrivilege(EntityType.CERTIFICATE)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .create();

        // grants for seb client config
        addPrivilege(EntityType.SEB_CLIENT_CONFIGURATION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .create();

        // grants for lms setup
        addPrivilege(EntityType.LMS_SETUP)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.MODIFY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();

        // grants for exam
        addPrivilege(EntityType.EXAM)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .create();

        // grants for exam templates
        addPrivilege(EntityType.EXAM_TEMPLATE)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .create();

        // grants for configuration node
        addPrivilege(EntityType.CONFIGURATION_NODE)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();
        // grants for configuration
        addPrivilege(EntityType.CONFIGURATION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();
        // grants for configuration value
        addPrivilege(EntityType.CONFIGURATION_VALUE)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();

        // grants for configuration attributes
        addPrivilege(EntityType.CONFIGURATION_ATTRIBUTE)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();

        // grants for configuration orientations
        addPrivilege(EntityType.ORIENTATION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();

        // grants for SEB client connections
        addPrivilege(EntityType.CLIENT_CONNECTION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.READ)
                .create();

        // grants for SEB client events
        addPrivilege(EntityType.CLIENT_EVENT)
                .forRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.READ)
                .create();

        // grants for user activity logs
        addPrivilege(EntityType.USER_ACTIVITY_LOG)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ)
                .create();
    }

    @Override
    public boolean hasGrant(
            final PrivilegeType privilegeType,
            final EntityType entityType,
            final Long institutionId,
            final String ownerId,
            final String userId,
            final Long userInstitutionId,
            final Set<UserRole> userRoles) {

        return userRoles
                .stream()
                .map(role -> new RoleTypeKey(entityType, role))
                .map(this.privileges::get)
                .anyMatch(privilege -> (privilege != null) && privilege.hasGrant(
                        userId,
                        userInstitutionId,
                        privilegeType,
                        institutionId,
                        ownerId));
    }

    @Override
    public boolean hasOwnerPrivilege(
            final PrivilegeType privilegeType,
            final EntityType entityType,
            final Long institutionId) {

        final SEBServerUser currentUser = this.getUserService().getCurrentUser();
        if (!currentUser.institutionId().equals(institutionId)) {
            return false;
        }

        return currentUser.getUserRoles()
                .stream()
                .map(role -> new RoleTypeKey(entityType, role))
                .map(this.privileges::get)
                .anyMatch(privilege -> (privilege != null) && privilege.hasOwnershipPrivilege(privilegeType));

    }

    private PrivilegeBuilder addPrivilege(final EntityType entityType) {
        return new PrivilegeBuilder(entityType);
    }

    /** Implements a GrantRuleBuilder for internal use and to make the code more readable.
     * See init (PostConstruct) */
    private final class PrivilegeBuilder {
        private final EntityType entityType;
        private UserRole userRole;
        private PrivilegeType basePrivilege = PrivilegeType.NONE;
        private PrivilegeType institutionalPrivilege = PrivilegeType.NONE;
        private PrivilegeType ownerPrivilege = PrivilegeType.NONE;

        public PrivilegeBuilder(final EntityType entityType) {
            super();
            this.entityType = entityType;
        }

        public PrivilegeBuilder forRole(final UserRole userRole) {
            this.userRole = userRole;
            return this;
        }

        public PrivilegeBuilder withBasePrivilege(final PrivilegeType basePrivilege) {
            this.basePrivilege = basePrivilege;
            return this;
        }

        public PrivilegeBuilder withInstitutionalPrivilege(final PrivilegeType institutionalPrivilege) {
            this.institutionalPrivilege = institutionalPrivilege;
            return this;
        }

        public PrivilegeBuilder withOwnerPrivilege(final PrivilegeType ownerPrivilege) {
            this.ownerPrivilege = ownerPrivilege;
            return this;
        }

        public PrivilegeBuilder andForRole(final UserRole userRole) {
            create();
            return new PrivilegeBuilder(this.entityType)
                    .forRole(userRole);
        }

        public void create() {
            final RoleTypeKey roleTypeKey = new RoleTypeKey(this.entityType, this.userRole);
            final Privilege roleTypeGrant = new Privilege(
                    roleTypeKey,
                    this.basePrivilege,
                    this.institutionalPrivilege,
                    this.ownerPrivilege);

            AuthorizationServiceImpl.this.privileges.put(roleTypeKey, roleTypeGrant);
        }
    }

}
