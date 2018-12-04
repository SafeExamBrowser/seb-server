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
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.RoleTypeGrant.RoleTypeKey;

/** A service to check authorization grants for a given user for entity-types and -instances
 *
 * If there is one or more GrantEntity objects within an authenticated user-request, this service
 * can be used check the authenticated user access grant within the object. Check if a given user
 * has write, modify or even read-only rights on an entity instance or on an entity type. */
@Lazy
@Service
@WebServiceProfile
public class AuthorizationGrantService {

    /** Map of role based grants for specified entity types. */
    private final Map<RoleTypeGrant.RoleTypeKey, RoleTypeGrant> grants = new HashMap<>();
    /** Map of collected AuthorizationGrantRule exceptions */
    private final Map<EntityType, AuthorizationGrantRule> exceptionalRules =
            new EnumMap<>(EntityType.class);

    private final CurrentUserService currentUserService;

    public AuthorizationGrantService(
            final Collection<AuthorizationGrantRule> exceptionalGrantRules,
            final CurrentUserService currentUserService) {

        this.currentUserService = currentUserService;

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

    /** Checks if a given user has a specified grant for a given entity-type
     *
     * NOTE: within this method only base-privileges for a given entity-type are checked
     * there is no institutional or ownership grant check because this information lays on an entity-instance
     * rather then the entity-type.
     *
     * @param entityType the entity type
     * @param grantType the grant type to check
     * @param principal an authorization Principal instance to extract the user from
     * @return true if a given user has a specified grant for a given entity-type. False otherwise */
    public boolean hasTypeGrant(final EntityType entityType, final GrantType grantType, final Principal principal) {
        final SEBServerUser user = this.currentUserService.extractFromPrincipal(principal);
        for (final UserRole role : user.getUserRoles()) {
            final RoleTypeGrant roleTypeGrant = this.grants.get(new RoleTypeKey(entityType, role));
            if (roleTypeGrant != null && roleTypeGrant.hasBasePrivilege(grantType)) {
                return true;
            }
        }

        return false;
    }

    /** Checks if a given user has specified grant for a given entity-instance
     *
     * @param entity the entity-instance
     * @param grantType the grant type to check
     * @param principal an authorization Principal instance to extract the user from
     * @return true if a given user has a specified grant for a given entity-instance. False otherwise */
    public boolean hasGrant(final GrantEntity entity, final GrantType grantType, final Principal principal) {
        return hasGrant(entity, grantType, this.currentUserService.extractFromPrincipal(principal));
    }

    /** Checks if a given user has specified grant for a given entity-instance
     *
     * @param entity the entity-instance
     * @param grantType the grant type to check
     * @param user a SEBServerUser instance to check grant for
     * @return true if a given user has a specified grant for a given entity-instance. False otherwise */
    public boolean hasGrant(final GrantEntity entity, final GrantType grantType, final SEBServerUser user) {
        final AuthorizationGrantRule authorizationGrantRule = getGrantRule(entity.entityType());
        if (authorizationGrantRule == null) {
            return false;
        }

        return authorizationGrantRule.hasGrant(entity, user, grantType);
    }

    /** Closure to get a grant check predicate to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param grantType the GrantType for the grant check filter
     * @param principal an authorization Principal instance to extract the user from
     * @return A filter predicate working on the given attributes to check user grants */
    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType grantType,
            final Principal principal) {

        return getGrantFilter(entityType, grantType, this.currentUserService.extractFromPrincipal(principal));
    }

    /** Closure to get a grant check predicate to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param grantType the GrantType for the grant check filter
     * @param user a SEBServerUser instance to check grant for
     * @return A filter predicate working on the given attributes to check user grants */
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

        public BaseTypeGrantRule(final EntityType type, final AuthorizationGrantService service) {
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

            AuthorizationGrantService.this.grants.put(roleTypeGrant.roleTypeKey, roleTypeGrant);
        }
    }

}
