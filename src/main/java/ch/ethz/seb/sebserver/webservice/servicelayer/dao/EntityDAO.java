/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface EntityDAO<T extends Entity, M extends ModelIdAware> {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    /** Use this to get an Entity instance of concrete type by database identifier
     *
     * @param id the data base identifier of the entity
     * @return Result refer the Entity instance with the specified database identifier or refer to an error if
     *         happened */
    Result<T> byPK(Long id);

    /** Use this to get an Entity instance of concrete type by model identifier
     *
     * NOTE: A model identifier may differ from the string representation of the database identifier
     * but usually they are the same.
     *
     * @param id the model identifier
     * @return Result refer the Entity instance with the specified model identifier or refer to an error if
     *         happened */
    default Result<T> byModelId(final String id) {
        return Result.tryCatch(() -> {
            return Long.parseLong(id);
        }).flatMap(this::byPK);
    }

    /** Use this to get a Collection of all entities of concrete type that matches a given predicate.
     *
     * NOTE: This first gets all records from database, for each creates new Entity instance and then
     * tests then matching predicate. So predicate filtering is not really fast
     * If you need a fast filtering implement a specific filtering in SQL level
     *
     * @param predicate Predicate expecting instance of type specific entity type
     * @param
     * @param active indicates if only active entities should be included (on SQL level). Can be null.
     * @return Result of Collection of Entity that matches a given predicate. Or an exception result on error
     *         case */
    Result<Collection<T>> all(Long institutionId);

    /** Use this to get a Collection of all entities of concrete type that matches a given predicate.
     *
     * NOTE: This first gets all records from database, for each creates new Entity instance and then
     * tests then matching predicate. So predicate filtering is not really fast
     * If you need a fast filtering implement a specific filtering in SQL level
     *
     * @param predicate Predicate expecting instance of type specific entity type
     * @return Result of Collection of Entity that matches a given predicate. Or an exception result on error
     *         case */
    default Result<Collection<T>> allOfInstitution(final long institutionId) {
        return all(institutionId);
    }

    Result<Collection<T>> loadEntities(Collection<EntityKey> keys);

    @Transactional(readOnly = true)
    default Result<Collection<EntityKeyAndName>> loadEntityNames(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            return loadEntities(keys)
                    .getOrThrow()
                    .stream()
                    .map(entity -> new EntityKeyAndName(
                            entity.entityType(),
                            entity.getModelId(),
                            entity.getName()))
                    .collect(Collectors.toList());
        });
    }

    /** Use this to save/modify an entity.
     * If the model identifier from given modified entity data is null or not exists already, a new entity is created.
     * If the model identifier is available and matches an existing entity, all entity data that are
     * not null on modified entity data instance are updated within the existing entity.
     *
     * @param modified modified data instance containing all data that should be modified
     * @return A Result of the entity instance where the successfully saved/modified entity data is available or a
     *         reported exception on error case */
    Result<T> save(M modified);

    /** Use this to delete a set Entity by a Collection of EntityKey
     *
     * @param all The Collection of EntityKey to delete
     * @return Result of a collection of all entities that has been deleted or refer to an error if
     *         happened */
    Collection<Result<EntityKey>> delete(Set<EntityKey> all);

    /** Context based utility method to extract an expected single resource entry form a Collection of specified type.
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

    /** Context based utility method to extract a list of id's (PK) from a collection of various EntityKey
     * This uses the EntityType defined by this instance to filter all EntityKey by the given type and
     * convert the matching EntityKey's to id's (PK's)
     *
     * Use this if you need to transform a Collection of EntityKey into a extracted List of id's of a specified
     * EntityType
     *
     * @param keys Collection of EntityKey of various types
     * @return List of id's (PK's) from the given key collection that match the concrete EntityType */
    default List<Long> extractPKsFromKeys(final Collection<EntityKey> keys) {

        if (keys == null) {
            return Collections.emptyList();
        }

        final EntityType entityType = entityType();
        return keys
                .stream()
                .filter(key -> key.entityType == entityType)
                .map(key -> Long.valueOf(key.modelId))
                .collect(Collectors.toList());
    }

}
