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

@Lazy
@Service
@WebServiceProfile
public class AuthorizationGrantService {

    private final Map<RoleTypeGrant.RoleTypeKey, RoleTypeGrant> grants = new HashMap<>();
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

    public boolean hasGrant(final GrantEntity entity, final GrantType type, final Principal principal) {
        return hasGrant(entity, type, this.currentUserService.extractFromPrincipal(principal));
    }

    public boolean hasGrant(final GrantEntity entity, final GrantType grantType, final SEBServerUser user) {
        final AuthorizationGrantRule authorizationGrantRule = getGrantRule(entity.entityType());
        if (authorizationGrantRule == null) {
            return false;
        }

        return authorizationGrantRule.hasGrant(entity, user, grantType);
    }

    public <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType type,
            final Principal principal) {

        return getGrantFilter(entityType, type, this.currentUserService.extractFromPrincipal(principal));
    }

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
        return this.exceptionalRules.computeIfAbsent(type, entityType -> new BaseTypeGrantRule(entityType));
    }

    private GrantRuleBuilder addGrant(final EntityType entityType) {
        return new GrantRuleBuilder(entityType);
    }

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

    private final class BaseTypeGrantRule implements AuthorizationGrantRule {

        private final EntityType type;
        private final Map<UserRole, RoleTypeGrant> grants;

        public BaseTypeGrantRule(final EntityType type) {
            this.type = type;
            this.grants = new EnumMap<>(UserRole.class);
            for (final UserRole role : UserRole.values()) {
                this.grants.put(role,
                        AuthorizationGrantService.this.grants.get(new RoleTypeKey(type, role)));
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
                if (roleTypeGrant != null) {
                    if (roleTypeGrant.hasPrivilege(user, entity, grantType)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

}
