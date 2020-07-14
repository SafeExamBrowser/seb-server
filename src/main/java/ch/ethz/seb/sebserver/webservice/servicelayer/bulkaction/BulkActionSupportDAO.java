/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;

/** Defines overall DAO support for bulk-actions like activate, deactivate, delete...
 *
 * @param <T> The type of the Entity of a concrete BulkActionSupportDAO */
public interface BulkActionSupportDAO<T extends Entity> {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    /** Gets a Set of EntityKey for all dependent entities for a given BulkAction
     * and the type of this BulkActionSupportDAO.
     *
     * @param bulkAction the BulkAction to get keys of dependencies for the concrete type of this BulkActionSupportDAO
     * @return Collection of Result. Each Result refers to the EntityKey of processed entity or to an error if
     *         happened */
    Set<EntityDependency> getDependencies(BulkAction bulkAction);

    /** This processed a given BulkAction for all entities of the concrete type of this BulkActionSupportDAO
     * that are defined by this given BulkAction.
     *
     * This returns a Collection of EntityKey results of each Entity that has been processed.
     * If there was an error for a particular Entity, the Result will have an error reference.
     *
     * @param bulkAction the BulkAction containing the source entity and all dependencies
     * @return a Collection of EntityKey results of each Entity that has been processed. */
    @Transactional
    default Collection<Result<EntityKey>> processBulkAction(final BulkAction bulkAction) {
        final Set<EntityKey> all = bulkAction.extractKeys(entityType());

        switch (bulkAction.type) {
            case ACTIVATE:
                return (this instanceof ActivatableEntityDAO)
                        ? ((ActivatableEntityDAO<?, ?>) this).setActive(all, true)
                                .map(BulkActionSupportDAO::transformResult)
                                .get(error -> handleBulkActionError(error, all))
                        : Collections.emptyList();
            case DEACTIVATE:
                return (this instanceof ActivatableEntityDAO)
                        ? ((ActivatableEntityDAO<?, ?>) this).setActive(all, false)
                                .map(BulkActionSupportDAO::transformResult)
                                .get(error -> handleBulkActionError(error, all))
                        : Collections.emptyList();
            case HARD_DELETE:
                return (this instanceof EntityDAO)
                        ? ((EntityDAO<?, ?>) this).delete(all)
                                .map(BulkActionSupportDAO::transformResult)
                                .get(error -> handleBulkActionError(error, all))
                        : Collections.emptyList();
        }

        // should never happen
        throw new UnsupportedOperationException("Unsupported Bulk Action: " + bulkAction);
    }

    /** This creates a collection of Results refer the given entity keys.
     *
     * @param keys Collection of entity keys to create Results from
     * @return a collection of Results refer the given entity keys. */
    static Collection<Result<EntityKey>> transformResult(final Collection<EntityKey> keys) {
        return keys.stream()
                .map(Result::of)
                .collect(Collectors.toList());
    }

    /** This creates a list of Result refer to a given error for all given EntityKey instances.
     *
     * @param error the error that shall be referred by created Result's
     * @param all all entity keys to create error Result for
     * @return List of Result refer to a given error for all given EntityKey instances */
    static List<Result<EntityKey>> handleBulkActionError(final Exception error, final Set<EntityKey> all) {
        return all.stream()
                .map(key -> Result.<EntityKey> ofError(new BulkActionEntityException(key, error)))
                .collect(Collectors.toList());
    }

    /** Get dependency keys of all source entities of a given BulkAction
     * This method simply goes through all source EntityKeys of the given BulkAction
     * and applies the selection functions for each, collecting the resulting dependency EntityDependency
     * into one Set of all dependency keys for all source keys
     *
     *
     * @param bulkAction The BulkAction that defines the source keys
     * @param selectionFunction a selection functions that gives all dependency keys for a given source key
     * @return Set of EntityDependency instances that define all entities that depends on the given bulk action */
    default Set<EntityDependency> getDependencies(
            final BulkAction bulkAction,
            final Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction) {

        return bulkAction.sources
                .stream()
                .map(selectionFunction) // apply select function for each source key
                .flatMap(DAOLoggingSupport::logAndSkipOnError) // handle and skip results with error
                .flatMap(Collection::stream) // Flatten stream of Collection in to one stream
                .collect(Collectors.toSet());
    }

}
