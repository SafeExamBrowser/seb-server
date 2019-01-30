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

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface EntityDAO<T extends Entity, M extends ModelIdAware> {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    /** Use this to get an Entity instance of concrete type by database identifier/primary-key (PK)
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
    @Transactional(readOnly = true)
    default Result<T> byModelId(final String id) {
        return Result.tryCatch(() -> {
            return Long.parseLong(id);
        }).flatMap(this::byPK);
    }

    /** Use this to get a Collection of all entities of concrete type of the given institution.
     *
     * NOTE: institutionId may be null. In that case this method uses a query to get all entities of
     * concrete type from all institutions. Anyways, to not pollute the memory it is recommended to set a limit by
     * using the <code>PaginationService</code> before calling this method
     *
     * @param institutionId the identifier of the institution.
     * @return Result of Collection of Entity of the given institution */
    Result<Collection<T>> all(Long institutionId);

    Result<Collection<T>> loadEntities(Collection<EntityKey> keys);

    @Transactional(readOnly = true)
    default Result<Collection<EntityName>> loadEntityNames(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            return loadEntities(keys)
                    .getOrThrow()
                    .stream()
                    .map(entity -> new EntityName(
                            entity.entityType(),
                            entity.getModelId(),
                            entity.getName()))
                    .collect(Collectors.toList());
        });
    }

    Result<T> createNew(M data);

    /** Use this to save/modify an entity.
     *
     * @param modelId the model id of the entity to save
     * @param data entity instance containing all data that should be saved
     * @return A Result of the entity instance where the successfully saved/modified entity data is available or a
     *         reported exception on error case */
    Result<T> save(String modelId, M data);

    /** Use this to delete a set Entity by a Collection of EntityKey
     *
     * @param all The Collection of EntityKey to delete
     * @return Result of a collection of all entities that has been deleted or refer to an error if
     *         happened */
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

    @Transactional(readOnly = true)
    default Result<Collection<T>> allMatching(final FilterMap filterMap) {
        return allMatching(filterMap, e -> true);
    }

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
