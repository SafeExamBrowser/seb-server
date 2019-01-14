/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface BulkActionSupport {

    /** Get the entity type for a concrete EntityDAO implementation.
     *
     * @return The EntityType for a concrete EntityDAO implementation */
    EntityType entityType();

    Set<EntityKey> getDependencies(BulkAction bulkAction);

    Result<Collection<Entity>> bulkLoadEntities(Collection<EntityKey> keys);

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

    Collection<Result<EntityKey>> processBulkAction(BulkAction bulkAction);

}
