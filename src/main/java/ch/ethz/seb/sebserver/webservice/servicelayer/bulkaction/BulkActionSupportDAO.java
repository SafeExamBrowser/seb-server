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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;

public interface BulkActionSupportDAO<T extends Entity> {

    Logger log = LoggerFactory.getLogger(BulkActionSupportDAO.class);

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    Set<EntityKey> getDependencies(BulkAction bulkAction);

    Result<Collection<T>> bulkLoadEntities(Collection<EntityKey> keys);

    @Transactional(readOnly = true)
    default Result<Collection<EntityKeyAndName>> bulkLoadEntityNames(final Collection<EntityKey> keys) {
        return Result.tryCatch(() -> {
            return bulkLoadEntities(keys)
                    .getOrThrow()
                    .stream()
                    .map(entity -> new EntityKeyAndName(
                            EntityType.INSTITUTION,
                            entity.getModelId(),
                            entity.getName()))
                    .collect(Collectors.toList());
        });
    }

    @Transactional
    default Collection<Result<EntityKey>> processBulkAction(final BulkAction bulkAction) {
        final Set<EntityKey> all = bulkAction.extractKeys(entityType());

        switch (bulkAction.type) {
            case ACTIVATE:
                return (this instanceof ActivatableEntityDAO)
                        ? ((ActivatableEntityDAO<?>) this).setActive(all, true)
                        : Collections.emptyList();
            case DEACTIVATE:
                return (this instanceof ActivatableEntityDAO)
                        ? ((ActivatableEntityDAO<?>) this).setActive(all, false)
                        : Collections.emptyList();
            case HARD_DELETE:
                return (this instanceof EntityDAO)
                        ? ((EntityDAO<?>) this).delete(all)
                        : Collections.emptyList();
        }

        // should never happen
        throw new UnsupportedOperationException("Unsupported Bulk Action: " + bulkAction);
    }

    @Transactional(readOnly = true)
    default Set<EntityKey> getDependencies(
            final BulkAction bulkAction,
            final Function<EntityKey, Result<Collection<EntityKey>>> selectionFunction) {

        return bulkAction.sources
                .stream()
                .map(selectionFunction) // apply select function for each source key
                .peek(result -> result.onErrorDo(error -> log.error("Unexpected error: ", error)))
                .flatMap(Result::skipOnError) // handle and skip results with error
                .flatMap(Collection::stream) // Flatten stream of Collection in to one stream
                .collect(Collectors.toSet());
    }

}
