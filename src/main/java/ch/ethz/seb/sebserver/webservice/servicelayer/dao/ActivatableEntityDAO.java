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

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Interface of a DAO for an Entity that has activation feature.
 *
 * @param <T> the type of Entity */
public interface ActivatableEntityDAO<T extends Entity, M extends ModelIdAware> extends EntityDAO<T, M> {

    /** Load all active entities of concrete type for the given institution and of given activity
     *
     * NOTE: institutionId may be null. In that case this method uses a query to get all active entities of
     * concrete type from all institutions. Anyways, to not pollute the memory it is recommended to set a limit by
     * using the <code>PaginationService</code> before calling this method
     *
     * @param institutionId the identifier of the institution.
     * @param active activity flag: true = all active, false = all inactive, null = all active and inactive
     * @return Result of Collection of Entity of the given institution and activity */
    Result<Collection<T>> all(Long institutionId, Boolean active);

    /** Set all entities referred by the given Collection of EntityKey active / inactive
     *
     * @param all The Collection of EntityKeys to set active or inactive
     * @param active The active flag
     * @return The Collection of Results refer to the EntityKey instance or refer to an error if happened */
    Result<Collection<EntityKey>> setActive(Set<EntityKey> all, boolean active);

}
