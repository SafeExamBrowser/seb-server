/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.security.Principal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.Privilege.RoleTypeKey;

@Lazy
@Service
@WebServiceProfile
public class AuthorizationGrantServiceImpl implements AuthorizationGrantService {

    /** Map of role based grants for specified entity types. */
    private final Map<Privilege.RoleTypeKey, Privilege> grants = new HashMap<>();
    /** Map of collected AuthorizationGrantRule exceptions */
    private final Map<EntityType, AuthorizationGrantRule> exceptionalRules =
            new EnumMap<>(EntityType.class);

    private final UserService userService;

    public AuthorizationGrantServiceImpl(
            final Collection<AuthorizationGrantRule> exceptionalGrantRules,
            final UserService userService) {

        this.userService = userService;

        if (exceptionalGrantRules != null) {
            exceptionalGrantRules.stream()
                    .forEach(r -> this.exceptionalRules.put(r.entityType(), r));
        }
    }

    @Override
    public UserService getUserService() {
        return this.userService;
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
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .create();

        // grants for lms setup
        addPrivilege(EntityType.LMS_SETUP)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.MODIFY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .create();

        // grants for user account
        addPrivilege(EntityType.USER)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .create();

        // grants for user activity logs
        addPrivilege(EntityType.USER_ACTIVITY_LOG)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ_ONLY)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .create();

        // grants for exam
        addPrivilege(EntityType.EXAM)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(PrivilegeType.READ_ONLY)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(PrivilegeType.READ_ONLY)
                .withOwnerPrivilege(PrivilegeType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(PrivilegeType.MODIFY)
                .create();

        // TODO other entities
    }

    @Override
    public void checkHasAnyPrivilege(final EntityType entityType, final PrivilegeType privilegeType) {
        final SEBServerUser currentUser = this.userService.getCurrentUser();
        if (hasBasePrivilege(entityType, privilegeType, currentUser) ||
                hasInstitutionalPrivilege(entityType, privilegeType, currentUser)) {
            return;
        }

        throw new PermissionDeniedException(entityType, privilegeType, currentUser.getUserInfo().uuid);
    }

    @Override
    public void checkPrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final Long institutionId) {

        final SEBServerUser currentUser = this.userService.getCurrentUser();
        if (hasBasePrivilege(entityType, privilegeType, currentUser)) {
            return;
        }

        if (institutionId == null) {
            throw new PermissionDeniedException(entityType, privilegeType, currentUser.getUserInfo().uuid);
        }

        if (hasInstitutionalPrivilege(entityType, privilegeType, currentUser) &&
                currentUser.institutionId().longValue() == institutionId.longValue()) {
            return;
        }

        throw new PermissionDeniedException(entityType, privilegeType, currentUser.getUserInfo().uuid);
    }

    @Override
    public void checkPrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final Long institutionId,
            final Long ownerId) {

        final SEBServerUser currentUser = this.userService.getCurrentUser();

        // TODO Auto-generated method stub

        throw new PermissionDeniedException(entityType, privilegeType, currentUser.getUserInfo().uuid);
    }

    @Override
    public <E extends GrantEntity> Result<E> checkGrantOnEntity(final E entity, final PrivilegeType privilegeType) {

        final SEBServerUser currentUser = this.userService.getCurrentUser();
        if (hasGrant(entity, privilegeType, currentUser)) {
            return Result.of(entity);
        } else {
            return Result.ofError(new PermissionDeniedException(entity, privilegeType, currentUser.getUserInfo().uuid));
        }
    }

    @Override
    public boolean hasBasePrivilege(final EntityType entityType, final PrivilegeType privilegeType) {
        return hasBasePrivilege(entityType, privilegeType, this.userService.getCurrentUser());
    }

    @Override
    public boolean hasBasePrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final Principal principal) {

        return hasBasePrivilege(entityType, privilegeType, this.userService.extractFromPrincipal(principal));
    }

    private boolean hasBasePrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final SEBServerUser user) {

        for (final UserRole role : user.getUserRoles()) {
            final Privilege roleTypeGrant = this.grants.get(new RoleTypeKey(entityType, role));
            if (roleTypeGrant != null && roleTypeGrant.hasBasePrivilege(privilegeType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasInstitutionalPrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType) {

        return hasInstitutionalPrivilege(
                entityType,
                privilegeType,
                this.userService.getCurrentUser());
    }

    @Override
    public boolean hasInstitutionalPrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final Principal principal) {

        return hasInstitutionalPrivilege(
                entityType,
                privilegeType,
                this.userService.extractFromPrincipal(principal));
    }

    private boolean hasInstitutionalPrivilege(
            final EntityType entityType,
            final PrivilegeType privilegeType,
            final SEBServerUser user) {

        for (final UserRole role : user.getUserRoles()) {
            final Privilege roleTypeGrant = this.grants.get(new RoleTypeKey(entityType, role));
            if (roleTypeGrant != null && roleTypeGrant.hasInstitutionalPrivilege(privilegeType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasGrant(final GrantEntity entity, final PrivilegeType grantType) {
        return hasGrant(entity, grantType, this.userService.getCurrentUser());
    }

    @Override
    public boolean hasGrant(final GrantEntity entity, final PrivilegeType grantType, final Principal principal) {
        return hasGrant(entity, grantType, this.userService.extractFromPrincipal(principal));
    }

    @Override
    public boolean hasGrant(final GrantEntity entity, final PrivilegeType grantType, final SEBServerUser user) {
        final AuthorizationGrantRule authorizationGrantRule = getGrantRule(entity.entityType());
        if (authorizationGrantRule == null) {
            return false;
        }

        return authorizationGrantRule.hasGrant(entity, user, grantType);
    }

    @Override
    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final PrivilegeType grantType) {

        return getGrantFilter(entityType, grantType, this.userService.getCurrentUser());
    }

    @Override
    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final PrivilegeType grantType,
            final Principal principal) {

        return getGrantFilter(entityType, grantType, this.userService.extractFromPrincipal(principal));
    }

    private <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final PrivilegeType grantType,
            final SEBServerUser user) {

        final AuthorizationGrantRule authorizationGrantRule = getGrantRule(entityType);
        if (authorizationGrantRule == null)
            return entity -> false;

        return entity -> authorizationGrantRule.hasGrant(entity, user, grantType);
    }

    private AuthorizationGrantRule getGrantRule(final EntityType type) {
        return this.exceptionalRules.computeIfAbsent(
                type,
                entityType -> new BaseTypeGrantRule(entityType, this));
    }

    private PrivilegeBuilder addPrivilege(final EntityType entityType) {
        return new PrivilegeBuilder(entityType);
    }

    /** This is the default (or base) implementation of a AuthorizationGrantRule.
     *
     * The rule is: go over all user-roles of the given user and for each user-role check
     * if there is base-privilege on the given entity-type for the given grant type.
     * if true return true
     * if false; check if there is a given institutional-privilege on the given
     * entity-instance for the given grant type.
     * if true return true
     * if false; check if there is a given ownership-privilege on the given
     * entity-instance for the given grant type.
     * if true return true
     * if false return false */
    private static class BaseTypeGrantRule implements AuthorizationGrantRule {

        private final EntityType type;
        private final Map<UserRole, Privilege> grants;

        public BaseTypeGrantRule(final EntityType type, final AuthorizationGrantServiceImpl service) {
            this.type = type;
            this.grants = new EnumMap<>(UserRole.class);
            for (final UserRole role : UserRole.values()) {
                this.grants.put(role,
                        service.grants.get(new RoleTypeKey(type, role)));
            }
        }

        @Override
        public EntityType entityType() {
            return this.type;
        }

        @Override
        public boolean hasGrant(final GrantEntity entity, final SEBServerUser user, final PrivilegeType grantType) {
            for (final UserRole role : user.getUserRoles()) {
                final Privilege roleTypeGrant = this.grants.get(role);
                if (roleTypeGrant != null && hasGrant(roleTypeGrant, user, entity, grantType)) {
                    return true;
                }
            }

            return false;
        }

        public boolean hasGrant(
                final Privilege roleTypeGrant,
                final SEBServerUser user,
                final GrantEntity entity,
                final PrivilegeType grantType) {

            return roleTypeGrant.hasBasePrivilege(grantType) ||
                    hasInstitutionalGrant(roleTypeGrant, user, entity, grantType) ||
                    hasOwnershipGrant(roleTypeGrant, user, entity, grantType);
        }

        private boolean hasInstitutionalGrant(
                final Privilege roleTypeGrant,
                final SEBServerUser user,
                final GrantEntity entity,
                final PrivilegeType grantType) {

            if (entity.getInstitutionId() == null) {
                return false;
            }

            return roleTypeGrant.hasInstitutionalPrivilege(grantType) &&
                    user.institutionId().longValue() == entity.getInstitutionId().longValue();
        }

        private boolean hasOwnershipGrant(
                final Privilege roleTypeGrant,
                final SEBServerUser user,
                final GrantEntity entity,
                final PrivilegeType grantType) {

            if (entity.getOwnerId() == null) {
                return false;
            }

            return roleTypeGrant.hasOwnershipPrivilege(grantType) &&
                    user.uuid().equals(entity.getOwnerId());
        }
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
                    this.basePrivilege,
                    this.institutionalPrivilege,
                    this.ownerPrivilege);

            AuthorizationGrantServiceImpl.this.grants.put(roleTypeKey, roleTypeGrant);
        }
    }

}
