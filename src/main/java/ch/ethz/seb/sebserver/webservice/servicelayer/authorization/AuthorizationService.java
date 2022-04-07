/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;

/** A service to check authorization grants for a given user for entity-types and -instances
 *
 * If there is one or more GrantEntity objects within an authenticated user-request, this service
 * can be used check the authenticated user access grant within the object. Check if a given user
 * has write, modify or even read rights on an entity instance or on an entity type. */
public interface AuthorizationService {

    /** Gets the UserService that is bundled within the AuthorizationGrantService
     *
     * @return the UserService that is bundled within the AuthorizationGrantService */
    UserService getUserService();

    /** All Privileges in a collection.
     *
     * @return all registered Privileges */
    Collection<Privilege> getAllPrivileges();

    /** Check grant on privilege type for specified EntityType and for the given user and institution.
     *
     * This makes a privilege grant check for every UserRole given. The first found successful grant
     * will immediately return true
     *
     * The privilege grant check function always checks first the base privilege with no institutional or owner grant.
     * If user has a grant on base privileges this returns true without checking further institutional or owner grant
     * If user has no base privilege grant the function checks further grants, first the institutional grant, where
     * the institution id and the users institution id must match and further more the owner grant, where ownerId
     * and the users id must match.
     *
     * see Privilege.hasGrant for more information how the overall grant function works
     *
     * @param privilegeType The privilege type to check
     * @param entityType The type of entity to check privilege on
     * @param institutionId the institution id (may be null in case the institutional grant check should be skipped)
     * @param ownerId the owner identifier (may be null in case the owner grant check should be skipped)
     * @param userId the user identifier (UUID)
     * @param userInstitutionId the user institution identifier
     * @param userRoles the user roles
     * @return true if there is any grant within the given context or false on deny */
    boolean hasGrant(
            PrivilegeType privilegeType,
            EntityType entityType,
            Long institutionId,
            String ownerId,
            String userId,
            Long userInstitutionId,
            Set<UserRole> userRoles);

    /** Check grant for a given privilege type and entity type for the current user.
     *
     * NOTE: This only checks the base privilege grant because there is no Entity specific information
     *
     * @param privilegeType the privilege type to check
     * @param entityType the type of the entity to check the given privilege type on
     * @return true if there is any grant within the given context or false on deny */
    default boolean hasGrant(final PrivilegeType privilegeType, final EntityType entityType) {
        final SEBServerUser currentUser = getUserService().getCurrentUser();
        final UserInfo userInfo = currentUser.getUserInfo();
        return hasGrant(
                privilegeType,
                entityType,
                null, null,
                userInfo.uuid,
                userInfo.institutionId,
                currentUser.getUserRoles());
    }

    /** Check grant for a given privilege type and Entity for the current user.
     *
     * @param privilegeType the privilege type to check
     * @param grantEntity the Entity to check the privilege grant on
     * @return true if there is any grant within the given context or false on deny */
    default boolean hasGrant(final PrivilegeType privilegeType, final GrantEntity grantEntity) {
        final SEBServerUser currentUser = getUserService().getCurrentUser();
        final UserInfo userInfo = currentUser.getUserInfo();
        return hasGrant(
                privilegeType,
                grantEntity.entityType(),
                grantEntity.getInstitutionId(),
                grantEntity.getOwnerId(),
                userInfo.uuid,
                userInfo.institutionId,
                currentUser.getUserRoles());
    }

    /** Check base privilege grant and institutional privilege grant for a given privilege type
     * on a given entity type.
     *
     * If the question is similar like this:
     * "Has the current user that belongs to institution A the right to create an entity of
     * type X on institution B", then this is the answer, use:
     *
     * hasPrivilege(PrivilegeType.WRITE, EntityType.X, B)
     *
     * @param privilegeType the privilege type to check
     * @param entityType the type of the entity to check the given privilege type on
     * @param institutionId the institution identifier for institutional privilege grant check
     * @return true if there is any grant within the given context or false on deny */
    default boolean hasGrant(
            final PrivilegeType privilegeType,
            final EntityType entityType,
            final Long institutionId) {

        final SEBServerUser currentUser = getUserService().getCurrentUser();
        final UserInfo userInfo = currentUser.getUserInfo();
        return hasGrant(
                privilegeType,
                entityType,
                institutionId,
                null,
                userInfo.uuid,
                userInfo.institutionId,
                currentUser.getUserRoles());
    }

    /** Check read grant for a given Entity instance and current user.
     *
     * @param grantEntity Entity instance
     * @return true if the current user has read grant on given Entity instance or false on deny */
    default boolean hasReadGrant(final GrantEntity grantEntity) {
        return hasGrant(PrivilegeType.READ, grantEntity);
    }

    /** Check modify grant for a given Entity instance and current user.
     *
     * @param grantEntity Entity instance
     * @return true if the current user has modify grant on given Entity instance or false on deny */
    default boolean hasModifyGrant(final GrantEntity grantEntity) {
        return hasGrant(PrivilegeType.MODIFY, grantEntity);
    }

    /** Check write grant for a given Entity instance and current user.
     *
     * @param grantEntity Entity instance
     * @return true if the current user has write grant on given Entity instance or false on deny */
    default boolean hasWriteGrant(final GrantEntity grantEntity) {
        return hasGrant(PrivilegeType.WRITE, grantEntity);
    }

    /** Check grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny.
     *
     * @param privilegeType the privilege type to check
     * @param entityType the type of the entity to check the given privilege type on */
    default void check(final PrivilegeType privilegeType, final EntityType entityType) {
        check(privilegeType, entityType, null);
    }

    /** Check grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny.
     *
     * @param privilegeType the privilege type to check
     * @param entityType the type of the entity to check the given privilege type on
     * @param institutionId the institution identifier for institutional privilege grant check */
    default void check(final PrivilegeType privilegeType, final EntityType entityType, final Long institutionId) {
        // check institutional grant
        if (hasGrant(privilegeType, entityType, institutionId)) {
            return;
        }

        // if there is no institutional grant the user may have owner based grant on the specified realm
        if (hasOwnerPrivilege(privilegeType, entityType, institutionId)) {
            return;
        }

        throw new PermissionDeniedException(
                entityType,
                privilegeType,
                getUserService().getCurrentUser().getUserInfo());

    }

    /** Check grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny.
     *
     * @param privilegeType the privilege type to check
     * @param userInfo the the user
     * @param grantEntity the entity */
    default <T extends GrantEntity> T check(
            final PrivilegeType privilegeType,
            final UserInfo userInfo,
            final T grantEntity) {

        // check institutional grant
        if (hasGrant(
                PrivilegeType.MODIFY,
                EntityType.CONFIGURATION_NODE,
                grantEntity.getInstitutionId(),
                userInfo.uuid,
                userInfo.uuid,
                userInfo.institutionId,
                userInfo.getUserRoles())) {
            return grantEntity;
        }

        // if there is no institutional grant the user may have owner based grant on the specified realm
        // TODO
//        return userInfo.getUserRoles()
//                .stream()
//                .map(role -> new RoleTypeKey(entityType, role))
//                .map(this.privileges::get)
//                .anyMatch(privilege -> (privilege != null) && privilege.hasOwnershipPrivilege(privilegeType));
//        if (hasOwnerPrivilege(privilegeType, entityType, institutionId)) {
//            return;
//        }

        throw new PermissionDeniedException(
                grantEntity.entityType(),
                privilegeType,
                getUserService().getCurrentUser().getUserInfo());

    }

    /** Indicates if the current user has an owner privilege for this give entity type and institution
     *
     * @param privilegeType the privilege type to check
     * @param entityType entityType the type of the entity to check ownership privilege
     * @param institutionId the institution identifier for institutional privilege grant check
     * @return true if the current user has owner privilege */
    boolean hasOwnerPrivilege(final PrivilegeType privilegeType, final EntityType entityType, Long institutionId);

    /** Check grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny or return the given entity within a Result on successful grant.
     * This is useful to use with a Result based functional chain.
     *
     * @param privilegeType the privilege type to check
     * @param entity The entity instance to check for privileges */
    default <E extends GrantEntity> Result<E> check(final PrivilegeType privilegeType, final E entity) {
        if (!hasGrant(privilegeType, entity)) {
            throw new PermissionDeniedException(
                    entity,
                    privilegeType,
                    getUserService().getCurrentUser().getUserInfo().uuid);
        }

        return Result.of(entity);
    }

    /** Check read grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny or returns the given grantEntity within a Result on successful grant.
     * This is useful to use with a Result based functional chain.
     *
     * @param entity The entity instance to check overall read access */
    default <E extends GrantEntity> Result<E> checkRead(final E entity) {
        return check(PrivilegeType.READ, entity);
    }

    /** Check modify grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny or returns the given grantEntity within a Result on successful grant.
     * This is useful to use with a Result based functional chain.
     *
     * @param entity The entity instance to check overall modify access */
    default <E extends GrantEntity> Result<E> checkModify(final E entity) {
        return check(PrivilegeType.MODIFY, entity);
    }

    /** Check write grant by using corresponding hasGrant(XY) method and throws PermissionDeniedException
     * on deny or returns the given grantEntity within a Result on successful grant.
     * This is useful to use with a Result based functional chain.
     *
     * @param entity The entity instance to check overall write access */
    default <E extends GrantEntity> Result<E> checkWrite(final E entity) {
        return check(PrivilegeType.WRITE, entity);
    }

    /** Checks if the current user has a specified role.
     * If not a PermissionDeniedException is thrown for the given EntityType
     *
     * @param roles The UserRoles to check if any of them matches to current users roles
     * @param institution the institution identifier
     * @param type EntityType for PermissionDeniedException
     * @throws PermissionDeniedException if current user don't have the specified UserRole */
    default void checkRole(final Long institution, final EntityType type, final UserRole... roles) {
        final SEBServerUser currentUser = this
                .getUserService()
                .getCurrentUser();

        final List<UserRole> rolesList = (roles == null)
                ? Collections.emptyList()
                : Arrays.asList(roles);

        if (!currentUser.institutionId().equals(institution) ||
                Collections.disjoint(currentUser.getUserRoles(), rolesList)) {

            throw new PermissionDeniedException(
                    type,
                    PrivilegeType.READ,
                    currentUser.getUserInfo());
        }
    }

}
