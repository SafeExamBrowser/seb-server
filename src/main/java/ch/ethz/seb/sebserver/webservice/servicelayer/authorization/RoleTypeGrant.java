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

/** Defines a grant for a specified role and entity-type. */
public final class RoleTypeGrant {

    /** Defines a RoleTypeKey that is the combined identity of
     * a UserRole and a EntityType the RoleTypeGrant is applying for */
    public final RoleTypeKey roleTypeKey;
    /** Defines a base-privilege grant type that defines the overall access for entity-type */
    public final GrantType basePrivilege;
    /** Defines an institutional grant type that defines the institutional restricted access for a
     * entity-instance */
    public final GrantType institutionalPrivilege;
    /** Defines an ownership grant type that defines the ownership restricted access for a entity-instance */
    public final GrantType ownershipPrivilege;

    public RoleTypeGrant(
            final GrantType basePrivilege,
            final GrantType institutionalPrivilege,
            final GrantType ownershipPrivilege,
            final EntityType type,
            final UserRole role) {

        this.roleTypeKey = new RoleTypeKey(type, role);
        this.basePrivilege = basePrivilege;
        this.institutionalPrivilege = institutionalPrivilege;
        this.ownershipPrivilege = ownershipPrivilege;
    }

    /** Checks if a given user has specified grant type for a given entity-instance.
     * Checks all privileges in the order of: basePrivilege, institutionalPrivilege and ownershipPrivilege
     *
     *
     * @param user SEBServerUser instance to check institutional grant
     * @param entity entity-instance to check institutional grant
     * @param grantType the GrantType to check on all privileges if one matches
     * @return true if one privilege of this RoleTypeGrant matches the implicit grant type check for a given user and
     *         entity instance */
    public boolean hasPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        return hasBasePrivilege(grantType) ||
                hasInstitutionalPrivilege(user, entity, grantType) ||
                hasOwnershipPrivilege(user, entity, grantType);
    }

    /** Checks the base privilege on given grantType by using the hasImplicit
     * function of this basePrivilege.
     *
     * Implicit in this case means: if the basePrivilege is of type GrantType.WRITE,
     * GrantType.MODIFY and GrantType.READ_ONLY are implicitly included.
     * If the basePrivilege is of type GrantType.MODIFY, the GrantType.READ_ONLY are implicitly included
     * and so on.
     *
     * @param grantType the GrantType to check on basePrivilege
     * @return true if the basePrivilege includes the given grantType */
    public boolean hasBasePrivilege(final GrantType grantType) {
        return this.basePrivilege.hasImplicit(grantType);
    }

    /** Checks the institutional privilege on given grantType by using the hasImplicit
     * function of this institutionalPrivilege.
     *
     * Implicit in this case means: if the institutionalPrivilege is of type GrantType.WRITE,
     * GrantType.MODIFY and GrantType.READ_ONLY are implicitly included.
     * If the institutionalPrivilege is of type GrantType.MODIFY, the GrantType.READ_ONLY are implicitly included
     * and so on.
     *
     * If the given GrantEntity instance has no institution id (null) this returns false
     *
     * @param grantType the GrantType to check on institutionalPrivilege
     * @param user SEBServerUser instance to check institutional grant
     * @param entity entity-instance to check institutional grant
     * @return true if the institutionalPrivilege includes the given grantType */
    public boolean hasInstitutionalPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        if (entity.getInstitutionId() == null) {
            return false;
        }

        return this.institutionalPrivilege.hasImplicit(grantType) &&
                user.institutionId().longValue() == entity.getInstitutionId().longValue();
    }

    /** Checks the ownership privilege on given grantType by using the hasImplicit
     * function of this ownershipPrivilege.
     *
     * Implicit in this case means: if the ownershipPrivilege is of type GrantType.WRITE,
     * GrantType.MODIFY and GrantType.READ_ONLY are implicitly included.
     * If the ownershipPrivilege is of type GrantType.MODIFY, the GrantType.READ_ONLY are implicitly included
     * and so on.
     *
     * If the given GrantEntity instance has no owner UUID (null) this returns false
     *
     * @param grantType the GrantType to check on ownershipPrivilege
     * @param user SEBServerUser instance to check ownership grant
     * @param entity entity-instance to check ownership grant
     * @return true if the ownershipPrivilege includes the given grantType */
    public boolean hasOwnershipPrivilege(
            final SEBServerUser user,
            final GrantEntity entity,
            final GrantType grantType) {

        if (entity.getOwnerUUID() == null) {
            return false;
        }

        return this.ownershipPrivilege.hasImplicit(grantType) &&
                user.uuid().equals(entity.getOwnerUUID());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.roleTypeKey == null) ? 0 : this.roleTypeKey.hashCode());
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
        final RoleTypeGrant other = (RoleTypeGrant) obj;
        if (this.roleTypeKey == null) {
            if (other.roleTypeKey != null)
                return false;
        } else if (!this.roleTypeKey.equals(other.roleTypeKey))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RoleTypeGrant [roleTypeKey=" + this.roleTypeKey + ", basePrivilege=" + this.basePrivilege
                + ", institutionalPrivilege=" + this.institutionalPrivilege + ", ownershipPrivilege="
                + this.ownershipPrivilege
                + "]";
    }

    /** A key that combines UserRole EntityType identity */
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

        @Override
        public String toString() {
            return "RoleTypeKey [entityType=" + this.entityType + ", userRole=" + this.userRole + "]";
        }
    }

}
