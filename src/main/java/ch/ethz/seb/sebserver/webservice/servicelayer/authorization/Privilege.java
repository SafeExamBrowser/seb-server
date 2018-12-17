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

/** Defines a Privilege by combining a PrivilegeType for base (overall) rights,
 * institutional rights and ownershipRights. */
public final class Privilege {

    /** Defines a base-privilege type that defines the overall access for an entity-type */
    public final PrivilegeType privilegeType;
    /** Defines an institutional privilege type that defines the institutional restricted access for a
     * entity-type */
    public final PrivilegeType institutionalPrivilege;
    /** Defines an ownership privilege type that defines the ownership restricted access for a entity-type */
    public final PrivilegeType ownershipPrivilege;

    public Privilege(
            final PrivilegeType privilegeType,
            final PrivilegeType institutionalPrivilege,
            final PrivilegeType ownershipPrivilege) {

        this.privilegeType = privilegeType;
        this.institutionalPrivilege = institutionalPrivilege;
        this.ownershipPrivilege = ownershipPrivilege;
    }

    /** Checks the base privilege on given privilegeType by using the hasImplicit
     * function of this privilegeType.
     *
     * @param privilegeType to check
     * @return true if the privilegeType includes the given privilegeType */
    public boolean hasBasePrivilege(final PrivilegeType privilegeType) {
        return this.privilegeType.hasImplicit(privilegeType);
    }

    /** Checks the institutional privilege on given privilegeType by using the hasImplicit
     * function of this institutionalPrivilege.
     *
     * @param privilegeType to check
     * @return true if the institutionalPrivilege includes the given privilegeType */
    public boolean hasInstitutionalPrivilege(final PrivilegeType privilegeType) {
        return this.institutionalPrivilege.hasImplicit(privilegeType);
    }

    /** Checks the owner-ship privilege on given privilegeType by using the hasImplicit
     * function of this ownershipPrivilege.
     *
     * @param privilegeType to check
     * @return true if the ownershipPrivilege includes the given privilegeType */
    public boolean hasOwnershipPrivilege(final PrivilegeType privilegeType) {
        return this.ownershipPrivilege.hasImplicit(privilegeType);
    }

    @Override
    public String toString() {
        return "Privilege [privilegeType=" + this.privilegeType + ", institutionalPrivilege="
                + this.institutionalPrivilege
                + ", ownershipPrivilege=" + this.ownershipPrivilege + "]";
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
