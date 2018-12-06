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

    /** Check a specified GrantType for a given GrantEntity for given user-principal and
     * returns a with a Result of the granted entity instance or with a Result of a
     * NoGrantException.
     *
     * @param entity The GrantEntity to check specified GrantType for
     * @param grantType The GrantType
     * @param principal the user principal
     * @return a with a Result of the granted entity instance or with a Result of a NoGrantException */
    <E extends GrantEntity> Result<E> checkGrantForEntity(
            final E entity,
            final GrantType grantType,
            final Principal principal);

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
    boolean hasBaseGrant(final EntityType entityType, final GrantType grantType, final Principal principal);

    /** Checks if a given user has specified grant for a given entity-instance
     *
     * @param entity the entity-instance
     * @param grantType the grant type to check
     * @param principal an authorization Principal instance to extract the user from
     * @return true if a given user has a specified grant for a given entity-instance. False otherwise */
    boolean hasGrant(final GrantEntity entity, final GrantType grantType, final Principal principal);

    /** Checks if a given user has specified grant for a given entity-instance
     *
     * @param entity the entity-instance
     * @param grantType the grant type to check
     * @param user a SEBServerUser instance to check grant for
     * @return true if a given user has a specified grant for a given entity-instance. False otherwise */
    boolean hasGrant(final GrantEntity entity, final GrantType grantType, final SEBServerUser user);

    /** Closure to get a grant check predicate to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param grantType the GrantType for the grant check filter
     * @param principal an authorization Principal instance to extract the user from
     * @return A filter predicate working on the given attributes to check user grants */
    <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType grantType,
            final Principal principal);

    /** Closure to get a grant check predicate to filter a several entity-instances within the same grant
     *
     * @param entityType the EntityType for the grant check filter
     * @param grantType the GrantType for the grant check filter
     * @param user a SEBServerUser instance to check grant for
     * @return A filter predicate working on the given attributes to check user grants */
    <T extends GrantEntity> Predicate<T> getGrantFilter(
            final EntityType entityType,
            final GrantType grantType,
            final SEBServerUser user);

}
