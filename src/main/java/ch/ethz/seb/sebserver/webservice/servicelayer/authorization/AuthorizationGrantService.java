/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.security.Principal;
import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** A service to check authorization grants for a given user for entity-types and -instances
 *
 * If there is one or more GrantEntity objects within an authenticated user-request, this service
 * can be used check the authenticated user access grant within the object. Check if a given user
 * has write, modify or even read-only rights on an entity instance or on an entity type. */
public interface AuthorizationGrantService {

    /** Gets the UserService that is bundled within the AuthorizationGrantService
     * 
     * @return the UserService that is bundled within the AuthorizationGrantService */
    UserService getUserService();

    /** Checks if the current user has any privilege (base or institutional or owner) for the given EntityType and
     * PrivilegeType.
     *
     * @param entityType the EntityType
     * @param privilegeType the PrivilegeType to check on EntityType */
    void checkHasAnyPrivilege(EntityType entityType, PrivilegeType privilegeType);

    /** Check if current user has grant on a given GrantEntity instance for specified PrivilegeType
     *
     * @param entity The GrantEntity to check specified PrivilegeType for
     * @param privilegeType The PrivilegeType
     * @return a with a Result of the granted entity instance or with a Result of a PermissionDeniedException */
    <E extends GrantEntity> Result<E> checkGrantOnEntity(E entity, PrivilegeType privilegeType);

    /** Checks if the current user has a implicit base privilege for a given entity-type
     *
     * @param entityType the entity type
     * @param privilegeType the privilege type to check
     * @return true if the current user has a implicit base privilege on specified PrivilegeType for a given
     *         EntityType */
    boolean hasBasePrivilege(EntityType entityType, PrivilegeType privilegeType);

    /** Checks if a given user has a implicit base privilege for a given entity-type
     *
     * @param entityType the entity type
     * @param privilegeType the privilege type to check
     * @return true if a given user has a implicit base privilege on specified PrivilegeType for a given EntityType */
    boolean hasBasePrivilege(EntityType entityType, PrivilegeType privilegeType, Principal principal);

    /** Checks if the current user has an implicit institutional privilege on a given EntityType
     *
     * @param entityType the entity type
     * @param privilegeType the privilege type to check
     * @return true if the current user has an implicit institutional privilege on the given EntityType */
    boolean hasInstitutionalPrivilege(EntityType entityType, PrivilegeType privilegeType);

    /** Checks if a given user has an implicit institutional privilege on a given EntityType
     *
     * @param entityType the entity type
     * @param privilegeType the privilege type to check
     * @return true if a given user has an implicit institutional privilege on the given EntityType */
    boolean hasInstitutionalPrivilege(EntityType entityType, PrivilegeType privilegeType, Principal principal);

    /** Checks if the current user has grant on a given entity for a specified PrivilegeType
     *
     * @param entity the entity-instance
     * @param privilegeType the privilege type to check
     * @return true if the current user has a specified grant for a given entity-instance */
    boolean hasGrant(GrantEntity entity, PrivilegeType privilegeType);

    /** Checks if a given user has grant on a given entity for a specified PrivilegeType
     *
     * @param entity the entity-instance
     * @param privilegeType the grant type to check
     * @param principal an authorization Principal instance to extract the user from
     * @return true if the given user has grant on a given entity for a specified PrivilegeType */
    boolean hasGrant(GrantEntity entity, PrivilegeType privilegeType, Principal principal);

    /** Checks if a given user has specified grant for a given entity-instance
     *
     * @param entity the entity-instance
     * @param privilegeType the grant type to check
     * @param user a SEBServerUser instance to check grant for
     * @return true if a given user has a specified grant for a given entity-instance. False otherwise */
    boolean hasGrant(GrantEntity entity, PrivilegeType privilegeType, SEBServerUser user);

    /** Closure to get a grant check predicate for the current user
     * to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param privilegeType the PrivilegeType for the grant check filter
     * @return A filter predicate working on the given attributes to check user grants */
    <T extends GrantEntity> Predicate<T> getGrantFilter(EntityType entityType, PrivilegeType privilegeType);

    /** Closure to get a grant check predicate to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param privilegeType the PrivilegeType for the grant check filter
     * @param principal an authorization Principal instance to extract the user from
     * @return A filter predicate working on the given attributes to check user grants */
    <T extends GrantEntity> Predicate<T> getGrantFilter(
            EntityType entityType,
            PrivilegeType privilegeType,
            Principal principal);

}
