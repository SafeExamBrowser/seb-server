/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public final class RoleTypeGrant {

    public final RoleTypeKey roleTypeKey;
    public final GrantType basePrivilege;
    public final GrantType institutionalPrivilege;
    public final GrantType ownerPrivilege;

    public RoleTypeGrant(
            final GrantType basePrivilege,
            final GrantType institutionalPrivilege,
            final GrantType ownerPrivilege,
            final EntityType type,
            final UserRole role) {

        this.roleTypeKey = new RoleTypeKey(type, role);
        this.basePrivilege = basePrivilege;
        this.institutionalPrivilege = institutionalPrivilege;
        this.ownerPrivilege = ownerPrivilege;
    }

    public boolean hasPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        return hasBasePrivilege(grantType) ||
                hasInstitutionalPrivilege(user, entity, grantType) ||
                hasOwnerPrivilege(user, entity, grantType);
    }

    public boolean hasBasePrivilege(final GrantType grantType) {
        return this.basePrivilege.hasImplicit(grantType);
    }

    public boolean hasInstitutionalPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        return this.institutionalPrivilege.hasImplicit(grantType) &&
                user.institutionId().longValue() == entity.institutionId().longValue();
    }

    public boolean hasOwnerPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        return this.ownerPrivilege.hasImplicit(grantType) &&
                user.uuid().equals(entity.ownerUUID());
    }

    static final class RoleTypeKey {

        public final EntityType entityType;
        public final UserRole userRole;

        public RoleTypeKey(final EntityType type, final UserRole role) {
            this.entityType = type;
            this.userRole = role;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.userRole == null) ? 0 : this.userRole.hashCode());
            result = prime * result + ((this.entityType == null) ? 0 : this.entityType.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final RoleTypeKey other = (RoleTypeKey) obj;
            if (this.userRole != other.userRole)
                return false;
            if (this.entityType != other.entityType)
                return false;
            return true;
        }
    }

}
