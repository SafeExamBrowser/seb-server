/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.util.Collection;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** A service to check authorization grants for a given user for entity-types and -instances
 *
 * If there is one or more GrantEntity objects within an authenticated user-request, this service
 * can be used check the authenticated user access grant within the object. Check if a given user
 * has write, modify or even read-only rights on an entity instance or on an entity type. */
public interface AuthorizationService {

    /** Gets the UserService that is bundled within the AuthorizationGrantService
     *
     * @return the UserService that is bundled within the AuthorizationGrantService */
    UserService getUserService();

    /** All Privileges in a collection.
     *
     * @return all registered Privileges */
    Collection<Privilege> getAllPrivileges();

    boolean hasPrivilege(
            PrivilegeType privilegeType,
            EntityType entityType,
            Long institutionId,
            String userId,
            Long userInstitutionId,
            Set<UserRole> userRoles);

    boolean hasPrivilege(PrivilegeType privilegeType, GrantEntity grantEntity);

    default boolean hasPrivilege(final PrivilegeType privilegeType, final EntityType entityType) {
        final SEBServerUser currentUser = getUserService().getCurrentUser();
        final UserInfo userInfo = currentUser.getUserInfo();
        return hasPrivilege(
                privilegeType,
                entityType,
                null,
                userInfo.uuid,
                userInfo.institutionId,
                currentUser.getUserRoles());
    }

    default boolean hasPrivilege(
            final PrivilegeType privilegeType,
            final EntityType entityType,
            final Long institutionId) {

        final SEBServerUser currentUser = getUserService().getCurrentUser();
        final UserInfo userInfo = currentUser.getUserInfo();
        return hasPrivilege(
                privilegeType,
                entityType,
                institutionId,
                userInfo.uuid,
                userInfo.institutionId,
                currentUser.getUserRoles());
    }

    default boolean hasReadonlyPrivilege(final GrantEntity grantEntity) {
        return hasPrivilege(PrivilegeType.READ_ONLY, grantEntity);
    }

    default boolean hasModifyPrivilege(final GrantEntity grantEntity) {
        return hasPrivilege(PrivilegeType.MODIFY, grantEntity);
    }

    default boolean hasWritePrivilege(final GrantEntity grantEntity) {
        return hasPrivilege(PrivilegeType.WRITE, grantEntity);
    }

    default void check(final PrivilegeType privilegeType, final EntityType entityType) {
        check(privilegeType, entityType, null);
    }

    default void check(final PrivilegeType privilegeType, final EntityType entityType, final Long institutionId) {
        if (!hasPrivilege(privilegeType, entityType, institutionId)) {
            throw new PermissionDeniedException(
                    entityType,
                    privilegeType,
                    getUserService().getCurrentUser().getUserInfo().uuid);
        }
    }

    default <E extends GrantEntity> Result<E> check(final PrivilegeType privilegeType, final E grantEntity) {
        if (!hasPrivilege(privilegeType, grantEntity)) {
            throw new PermissionDeniedException(
                    grantEntity,
                    privilegeType,
                    getUserService().getCurrentUser().getUserInfo().uuid);
        }

        return Result.of(grantEntity);
    }

    default <E extends GrantEntity> Result<E> checkReadonly(final E grantEntity) {
        return check(PrivilegeType.READ_ONLY, grantEntity);
    }

    default <E extends GrantEntity> Result<E> checkModify(final E grantEntity) {
        return check(PrivilegeType.MODIFY, grantEntity);
    }

    default <E extends GrantEntity> Result<E> checkWrite(final E grantEntity) {
        return check(PrivilegeType.WRITE, grantEntity);
    }

}
