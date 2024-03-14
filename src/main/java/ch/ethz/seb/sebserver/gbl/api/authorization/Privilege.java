/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api.authorization;

import java.util.Arrays;
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

/** Defines a Privilege by combining a PrivilegeType for base (overall) rights,
 * institutional rights and ownership rights.
 * <p>
 * A base-, institutional- and ownership- grant is checked in this exact order and the
 * first match fund makes a grant or a denied if none of the three privilege levels has a match */
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

    /** Checks if this Privilege has a grant for a given context.
     * <p>
     * The privilege grant check function always checks first the base privilege with no institutional or owner grant.
     * If user has a grant on base privileges this returns true without checking further institutional or owner grant
     * If user has no base privilege grant the function checks further grants, first the institutional grant, where
     * the institution id and the users institution id must match and furthermore the owner grant, where ownerId
     * and the users id must match.
     *
     * @param userId The user identifier of the user to check the grant on
     * @param userInstitutionId the users institution identifier. The institution where the user belong to
     * @param privilegeType the type of privilege to check (READ_ONLY, MODIFY, WRITE...)
     * @param institutionId the institution identifier of an Entity for the institutional grant check,
     *            may be null in case the institutional grant check should be skipped
     * @param ownerId the owner identifier of an Entity for ownership grant check.
     *            This can be a single id or a comma-separated list of user ids and may be null in case
     *            the ownership grant check should be skipped
     * @return true if there is any grant within the given context or false on deny */
    public final boolean hasGrant(
            final String userId,
            final Long userInstitutionId,
            final PrivilegeType privilegeType,
            final Long institutionId,
            final String ownerId) {

        // Has base privilege?
        if (this.hasBasePrivilege(privilegeType)) {
            return true;
        }

        // has institutional privilege?
        if ((institutionId != null)
                && (this.hasInstitutionalPrivilege(privilegeType)
                && userInstitutionId.longValue() == institutionId.longValue())) {
            return true;
        }

        // has owner privilege?
        return this.hasOwnershipPrivilege(privilegeType) && isOwner(ownerId, userId);

    }

    private boolean isOwner(final String ownerId, final String userId) {
        if (StringUtils.isBlank(ownerId)) {
            return false;
        }

        return Arrays.asList(StringUtils.split(ownerId, Constants.LIST_SEPARATOR))
                .contains(userId);
    }

    @Override
    public String toString() {
        return "Privilege [privilegeType=" + this.basePrivilege + ", institutionalPrivilege="
                + this.institutionalPrivilege
                + ", ownershipPrivilege=" + this.ownershipPrivilege + "]";
    }

    /** Checks if the current user has role based edit access to a specified user account.
     * <p>
     * If user account has UserRole.SEB_SERVER_ADMIN this always gives true
     * If user account has UserRole.INSTITUTIONAL_ADMIN this is true if the given user account has
     * not the UserRole.SEB_SERVER_ADMIN (institutional administrators should not be able to edit SEB Server
     * administrators)
     * If the current user is the same as the given user account this is always true no matter if there are any
     * user-account based privileges (every user shall see its own account)
     *
     * @param userAccount the user account the check role based edit access
     * @return true if the current user has role based edit access to a specified user account */
    public static boolean hasRoleBasedUserAccountEditGrant(final UserAccount userAccount, final UserInfo currentUser) {
        final EnumSet<UserRole> userRolesOfUserAccount = userAccount.getUserRoles();
        final EnumSet<UserRole> userRolesOfCurrentUser = currentUser.getUserRoles();

        if (userRolesOfCurrentUser.contains(UserRole.SEB_SERVER_ADMIN)) {
            return true;
        }

        if (userRolesOfCurrentUser.contains(UserRole.INSTITUTIONAL_ADMIN)) {
            return !userRolesOfUserAccount.contains(UserRole.SEB_SERVER_ADMIN);
        }

        return currentUser.equals(userAccount);
    }
}
