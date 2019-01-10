/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Interface of a DAO for an Entity that has activation feature.
 *
 * @param <T> the concrete Entity type */
public interface ActivatableEntityDAO<T extends Entity> extends EntityDAO<T> {

    /** Get a Collection of all active Entity instances for a concrete entity-domain.
     *
     * @return A Result refer to a Collection of all active Entity instances for a concrete entity-domain
     *         or refer to an error if happened */
    Result<Collection<T>> allActive();

    /** Set the entity with specified identifier active / inactive
     *
     * @param entityId The Entity identifier
     * @param active The active flag
     * @return A Result refer to the Entity instance or refer to an error if happened */
    Result<T> setActive(String entityId, boolean active);

    /** Get notified if some Entity instance has been activated
     * This can be used to take action in dependency of an activation of an Entity of different type.
     * For example a user-account DAO want to react on a Institution activation to also activate all user
     * accounts for this institution.
     *
     * @param source The source Entity that has been activated */
    void notifyActivation(Entity source);

    /** Get notified if some Entity instance has been deactivated
     * This can be used to take action in dependency of an deactivation of an Entity of different type.
     * For example a user-account DAO want to react on a Institution deactivation to also deactivate all user
     * accounts for this institution.
     *
     * @param source The source Entity that has been deactivated */
    void notifyDeactivation(Entity source);

}
