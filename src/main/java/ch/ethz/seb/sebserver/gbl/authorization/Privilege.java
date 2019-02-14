/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.authorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

/** Defines a Privilege by combining a PrivilegeType for base (overall) rights,
 * institutional rights and ownershipRights. */
public final class Privilege {

    /** The RoleTypeKey defining the UserRole and EntityType for this Privilege */
    @JsonProperty("roleTypeKey")
    public final RoleTypeKey roleTypeKey;

    /** Defines a base-privilege type that defines the overall access for an entity-type */
    @JsonProperty("basePrivilege")
    public final PrivilegeType basePrivilege;

    /** Defines an institutional privilege type that defines the institutional restricted access for a
     * entity-type */
    @JsonProperty("institutionalPrivilege")
    public final PrivilegeType institutionalPrivilege;

    /** Defines an ownership privilege type that defines the ownership restricted access for a entity-type */
    @JsonProperty("ownershipPrivilege")
    public final PrivilegeType ownershipPrivilege;

    @JsonCreator
    public Privilege(
            @JsonProperty("roleTypeKey") final RoleTypeKey roleTypeKey,
            @JsonProperty("basePrivilege") final PrivilegeType basePrivilege,
            @JsonProperty("institutionalPrivilege") final PrivilegeType institutionalPrivilege,
            @JsonProperty("ownershipPrivilege") final PrivilegeType ownershipPrivilege) {

        this.roleTypeKey = roleTypeKey;
        this.basePrivilege = basePrivilege;
        this.institutionalPrivilege = institutionalPrivilege;
        this.ownershipPrivilege = ownershipPrivilege;
    }

    /** Checks the base privilege on given privilegeType by using the hasImplicit
     * function of this privilegeType.
     *
     * @param privilegeType to check
     * @return true if the privilegeType includes the given privilegeType */
    public boolean hasBasePrivilege(final PrivilegeType privilegeType) {
        return this.basePrivilege.hasImplicit(privilegeType);
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

    public final boolean hasGrant(
            final String userId,
            final Long userInstitutionId,
            final PrivilegeType privilegeType,
            final Long institutionId,
            final String ownerId) {

        return this.hasBasePrivilege(privilegeType)
                || ((institutionId != null) &&
                        (this.hasInstitutionalPrivilege(privilegeType)
                                && userInstitutionId.longValue() == institutionId
                                        .longValue())
                        || (this.hasOwnershipPrivilege(privilegeType)
                                && userId.equals(ownerId)));
    }

    @Override
    public String toString() {
        return "Privilege [privilegeType=" + this.basePrivilege + ", institutionalPrivilege="
                + this.institutionalPrivilege
                + ", ownershipPrivilege=" + this.ownershipPrivilege + "]";
    }

    /** A key that combines UserRole EntityType identity */
    public static final class RoleTypeKey {

        @JsonProperty("entityType")
        public final EntityType entityType;
        @JsonProperty("userRole")
        public final UserRole userRole;

        @JsonCreator
        public RoleTypeKey(
                @JsonProperty("entityType") final EntityType type,
                @JsonProperty("userRole") final UserRole role) {

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
