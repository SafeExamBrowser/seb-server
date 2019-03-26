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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Defines generic interface for all Entity based Data Access Objects
 *
 * @param <T> The specific type of the Entity domain model
 * @param <M> The specific type of the Entity domain model to create a new Entity */
public interface EntityDAO<T extends Entity, M extends ModelIdAware> {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    /** Use this to get an Entity instance of concrete type by database identifier/primary-key (PK)
     *
     * @param id the data base identifier of the entity
     * @return Result referring the Entity instance with the specified database identifier or refer to an error if
     *         happened */
    Result<T> byPK(Long id);

    /** Use this to get an Entity instance of concrete type by model identifier
     *
     * NOTE: A model identifier may differ from the string representation of the database identifier
     * but usually they are the same.
     *
     * @param id the model identifier
     * @return Result referring the Entity instance with the specified model identifier or refer to an error if
     *         happened */
    @Transactional(readOnly = true)
    default Result<T> byModelId(final String id) {
        return Result.tryCatch(() -> {
            return Long.parseLong(id);
        }).flatMap(this::byPK);
    }

    /** Get a collection of all entities for the given Set of entity keys.
     *
     * @param keys the Set of EntityKey to get the Entity's for
     * @return Result referring the collection or an error if happened */
    Result<Collection<T>> byEntityKeys(Set<EntityKey> keys);

    /** Get a collection of all EntityName for the given Set of EntityKey.
     *
     * @param keys the Set of EntityKey to get the EntityName's for
     * @return Result referring the collection or an error if happened */
    @Transactional(readOnly = true)
    default Result<Collection<EntityName>> getEntityNames(final Set<EntityKey> keys) {
        return Result.tryCatch(() -> {
            return byEntityKeys(keys)
                    .getOrThrow()
                    .stream()
                    .map(entity -> new EntityName(
                            entity.entityType(),
                            entity.getModelId(),
                            entity.getName()))
                    .collect(Collectors.toList());
        });
    }

    /** Create a new Entity from the given entity domain model data.
     *
     * @param data The entity domain model data
     * @return Result referring to the newly created Entity or an error if happened */
    Result<T> createNew(M data);

    /** Use this to save/modify an entity.
     *
     * @param data entity instance containing all data that should be saved
     * @return A Result referring the entity instance where the successfully saved/modified entity data is available or
     *         a
     *         reported exception on error case */
    Result<T> save(T data);

    /** Use this to delete all entities defined by a set of EntityKey
     * NOTE: the Set of EntityKey may contain EntityKey of other entity types like the concrete type of the DAO
     * use extractPKsFromKeys to get a list of concrete primary keys for entities to delete
     *
     * @param all The Collection of EntityKey to delete
     * @return Result referring a collection of all entities that has been deleted or refer to an error if
     *         happened */
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

    /** Get a (unordered) collection of all Entities that matches the given filter criteria.
     * The possible filter criteria for a specific Entity type is defined by the entity type.
     *
     * This adds filtering in SQL level by creating the select where clause from related
     * filter criteria of the specific Entity type. If the filterMap contains a value for
     * a particular filter criteria the value is extracted from the map and added to the where
     * clause of the SQL select statement.
     *
     * @param filterMap FilterMap instance containing all the relevant filter criteria
     * @return Result referring to collection of all matching entities or an error if happened */
    @Transactional(readOnly = true)
    default Result<Collection<T>> allMatching(final FilterMap filterMap) {
        return allMatching(filterMap, e -> true);
    }

    /** Get a (unordered) collection of all Entities that matches a given filter criteria
     * and a given predicate.
     *
     * The possible filter criteria for a specific Entity type is defined by the entity type.
     * This adds filtering in SQL level by creating the select where clause from related
     * filter criteria of the specific Entity type. If the filterMap contains a value for
     * a particular filter criteria the value is extracted from the map and added to the where
     * clause of the SQL select statement.
     *
     * The predicate is applied after the SQL query by filtering the resulting list with the
     * predicate after on the SQL query result, before returning.
     *
     * @param filterMap FilterMap instance containing all the relevant filter criteria
     * @return Result referring to collection of all matching entities or an error if happened */
    Result<Collection<T>> allMatching(FilterMap filterMap, Predicate<T> predicate);

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
