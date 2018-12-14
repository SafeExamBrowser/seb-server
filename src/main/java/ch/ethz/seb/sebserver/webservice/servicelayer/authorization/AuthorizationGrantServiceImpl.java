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
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.RoleTypeGrant.RoleTypeKey;

@Lazy
@Service
@WebServiceProfile
public class AuthorizationGrantServiceImpl implements AuthorizationGrantService {

    /** Map of role based grants for specified entity types. */
    private final Map<RoleTypeGrant.RoleTypeKey, RoleTypeGrant> grants = new HashMap<>();
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

    /** Initialize the (hard-coded) grants */
    @PostConstruct
    public void init() {
        // grants for institution
        addGrant(EntityType.INSTITUTION)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(GrantType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(GrantType.MODIFY)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(GrantType.READ_ONLY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(GrantType.READ_ONLY)
                .create();

        // grants for lms setup
        addGrant(EntityType.LMS_SETUP)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(GrantType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(GrantType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(GrantType.MODIFY)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withInstitutionalPrivilege(GrantType.READ_ONLY)
                .create();

        // grants for user account
        addGrant(EntityType.USER)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(GrantType.WRITE)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(GrantType.WRITE)
                .andForRole(UserRole.EXAM_ADMIN)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .create();

        // grants for exam
        addGrant(EntityType.EXAM)
                .forRole(UserRole.SEB_SERVER_ADMIN)
                .withBasePrivilege(GrantType.READ_ONLY)
                .andForRole(UserRole.INSTITUTIONAL_ADMIN)
                .withInstitutionalPrivilege(GrantType.READ_ONLY)
                .andForRole(UserRole.EXAM_ADMIN)
                .withInstitutionalPrivilege(GrantType.READ_ONLY)
                .withOwnerPrivilege(GrantType.WRITE)
                .andForRole(UserRole.EXAM_SUPPORTER)
                .withOwnerPrivilege(GrantType.MODIFY)
                .create();

        // TODO other entities
    }

    @Override
    public <E extends GrantEntity> Result<E> checkGrantForEntity(
            final E entity,
            final GrantType grantType,
            final Principal principal) {

        if (hasGrant(entity, grantType, principal)) {
            return Result.of(entity);
        } else {
            return Result.ofError(new PermissionDeniedException(entity, grantType, principal.getName()));
        }
    }

    @Override
    public Result<EntityType> checkGrantForType(
            final EntityType entityType,
            final GrantType grantType,
            final Principal principal) {

        if (hasBaseGrant(entityType, grantType, principal)) {
            return Result.of(entityType);
        } else {
            return Result.ofError(new PermissionDeniedException(entityType, grantType, principal.getName()));
        }
    }

    @Override
    public boolean hasBaseGrant(
            final EntityType entityType,
            final GrantType grantType,
            final Principal principal) {

        final SEBServerUser user = this.userService.extractFromPrincipal(principal);
        for (final UserRole role : user.getUserRoles()) {
            final RoleTypeGrant roleTypeGrant = this.grants.get(new RoleTypeKey(entityType, role));
            if (roleTypeGrant != null && roleTypeGrant.hasBasePrivilege(grantType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasGrant(final GrantEntity entity, final GrantType grantType, final Principal principal) {
        return hasGrant(entity, grantType, this.userService.extractFromPrincipal(principal));
    }

    @Override
    public boolean hasGrant(final GrantEntity entity, final GrantType grantType, final SEBServerUser user) {
        final AuthorizationGrantRule authorizationGrantRule = getGrantRule(entity.entityType());
        if (authorizationGrantRule == null) {
            return false;
        }

        return authorizationGrantRule.hasGrant(entity, user, grantType);
    }

    @Override
    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType grantType,
            final Principal principal) {

        return getGrantFilter(entityType, grantType, this.userService.extractFromPrincipal(principal));
    }

    @Override
    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType grantType,
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

    private GrantRuleBuilder addGrant(final EntityType entityType) {
        return new GrantRuleBuilder(entityType);
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
        private final Map<UserRole, RoleTypeGrant> grants;

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
        public boolean hasGrant(final GrantEntity entity, final SEBServerUser user, final GrantType grantType) {
            for (final UserRole role : user.getUserRoles()) {
                final RoleTypeGrant roleTypeGrant = this.grants.get(role);
                if (roleTypeGrant != null && roleTypeGrant.hasPrivilege(user, entity, grantType)) {
                    return true;
                }
            }

            return false;
        }
    }

    /** Implements a GrantRuleBuilder for internal use and to make the code more readable.
     * See init (PostConstruct) */
    private final class GrantRuleBuilder {
        private final EntityType entityType;
        private UserRole userRole;
        private GrantType basePrivilege = GrantType.NONE;
        private GrantType institutionalPrivilege = GrantType.NONE;
        private GrantType ownerPrivilege = GrantType.NONE;

        public GrantRuleBuilder(final EntityType entityType) {
            super();
            this.entityType = entityType;
        }

        public GrantRuleBuilder forRole(final UserRole userRole) {
            this.userRole = userRole;
            return this;
        }

        public GrantRuleBuilder withBasePrivilege(final GrantType basePrivilege) {
            this.basePrivilege = basePrivilege;
            return this;
        }

        public GrantRuleBuilder withInstitutionalPrivilege(final GrantType institutionalPrivilege) {
            this.institutionalPrivilege = institutionalPrivilege;
            return this;
        }

        public GrantRuleBuilder withOwnerPrivilege(final GrantType ownerPrivilege) {
            this.ownerPrivilege = ownerPrivilege;
            return this;
        }

        public GrantRuleBuilder andForRole(final UserRole userRole) {
            create();
            return new GrantRuleBuilder(this.entityType)
                    .forRole(userRole);
        }

        public void create() {
            final RoleTypeGrant roleTypeGrant = new RoleTypeGrant(
                    this.basePrivilege,
                    this.institutionalPrivilege,
                    this.ownerPrivilege,
                    this.entityType,
                    this.userRole);

            AuthorizationGrantServiceImpl.this.grants.put(roleTypeGrant.roleTypeKey, roleTypeGrant);
        }
    }

}
