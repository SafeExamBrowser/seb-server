/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Interface of a DAO for an Entity that has activation feature.
 *
 * @param <T> the concrete Entity type */
public interface ActivatableEntityDAO<T extends Entity, M extends ModelIdAware> extends EntityDAO<T, M> {

    /** Get a Collection of all active Entity instances for a concrete entity-domain.
     *
     * @return A Result refer to a Collection of all active Entity instances for a concrete entity-domain
     *         or refer to an error if happened */
    @Transactional(readOnly = true)
    default Result<Collection<T>> allActive() {
        return all(i -> true, true);
    }

    /** Set all entities referred by the given Collection of EntityKey active / inactive
     *
     * @param all The Collection of EntityKeys to set active or inactive
     * @param active The active flag
     * @return The Collection of Results refer to the EntityKey instance or refer to an error if happened */
    Collection<Result<EntityKey>> setActive(Set<EntityKey> all, boolean active);

}
