/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface EntityDAO<T extends Entity> {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    /** Use this to get an Entity instance of concrete type by database identifier
     *
     * @param id the data base identifier of the entity
     * @return Result refer the Entity instance with the specified database identifier or refer to an error if
     *         happened */
    Result<T> byId(Long id);

    /** Use this to get a Collection of all entities of concrete type that matches a given predicate.
     *
     * NOTE: This first gets all records from database, for each creates new Entity instance and then
     * tests then matching predicate. So predicate filtering is not really fast
     * If you need a fast filtering implement a specific filtering in SQL level
     *
     * @param predicate Predicate expecting instance of type specific entity type
     * @param active indicates if only active entities should be included (on SQL level). Can be null.
     * @return Result of Collection of Entity that matches a given predicate. Or an exception result on error
     *         case */
    Result<Collection<T>> all(Predicate<T> predicate, Boolean active);

    /** Use this to get a Collection of all entities of concrete type that matches a given predicate.
     *
     * NOTE: This first gets all records from database, for each creates new Entity instance and then
     * tests then matching predicate. So predicate filtering is not really fast
     * If you need a fast filtering implement a specific filtering in SQL level
     *
     * @param predicate Predicate expecting instance of type specific entity type
     * @return Result of Collection of Entity that matches a given predicate. Or an exception result on error
     *         case */
    default Result<Collection<T>> all(final Predicate<T> predicate) {
        return all(predicate, null);
    }

    /** Use this to get a Collection of all active entities of concrete type
     *
     * @return Result of Collection of all active entities or an exception result on error case */
    default Result<Collection<T>> all() {
        return all(entity -> true);
    }

    /** Use this to delete a set Entity by a Collection of EntityKey
     *
     * @param all The Collection of EntityKey to delete
     * @return Result of a collection of all entities that has been deleted or refer to an error if
     *         happened */
    Collection<Result<EntityKey>> delete(Set<EntityKey> all);

    /** Utility method to extract an expected single resource entry form a Collection of specified type.
     * Gets a Result refer to an expected single resource entry form a Collection of specified type or refer
     * to a ResourceNotFoundException if specified collection is null or empty or refer to a
     * unexpected RuntimeException if there are more then the expected single element in the given collection
     *
     * @param id The resource id to wrap within a ResourceNotFoundException if needed
     * @param resources the collection of resource entries
     * @return Result refer to an expected single resource entry form a Collection of specified type or refer to an
     *         error if happened */
    default <R> Result<R> getSingleResource(final String id, final Collection<R> resources) {

        if (resources == null || resources.isEmpty()) {
            return Result.ofError(new ResourceNotFoundException(entityType(), id));
        } else if (resources.size() > 1) {
            return Result.ofError(
                    new RuntimeException("Unexpected resource count result. Expected is exactly one resource but is: "
                            + resources.size()));
        }

        return Result.of(resources.iterator().next());
    }

    default List<Long> extractIdsFromKeys(
            final Collection<EntityKey> keys,
            final Collection<Result<EntityKey>> result) {

        final EntityType entityType = entityType();
        final List<Long> ids = new ArrayList<>();
        for (final EntityKey key : keys) {
            if (key.entityType == entityType) {
                try {
                    ids.add(Long.valueOf(key.entityId));
                } catch (final Exception e) {
                    result.add(Result.ofError(new IllegalArgumentException("Invalid id for EntityKey: " + key)));
                }
            }
        }

        return ids;
    }

}
