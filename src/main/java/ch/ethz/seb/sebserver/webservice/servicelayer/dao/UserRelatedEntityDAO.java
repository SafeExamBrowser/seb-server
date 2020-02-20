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

/** Interface for all Data Access Objects handling an Entity with relations to an user (account)
 *
 * @param <T> the concrete type of the Entity */
public interface UserRelatedEntityDAO<T extends Entity> {

    /** Get all Entity instances that has a relation to the user-account
     * of a given user identity (UUID)
     *
     * @param userUuid the users identity
     * @return A Result of all Entity instances that has a relation to the user-account
     *         of a given user identity (UUID) or a Result with error if happen */
    Result<Collection<T>> getAllForUser(String userUuid);

    /** Overwrite all user-references for entities that belongs to the user with the given identity
     * to refer to an internal anonymous user-account
     *
     * @param userUuid the users identity
     * @param deactivate indicates if the effected entities should also be deactivated if possible
     * @return A Result with the number of overwrite Entity instances or with an error if happen */
    Result<Integer> overwriteUserReferences(final String userUuid, boolean deactivate);

    /** Delete all user-references for entities that belongs to the user with the given identity
     *
     * NOTE: This processes a hard-delete. All effected data will be lost.
     *
     * @param userUuid the users identity
     * @return A Result with the number of deleted Entity instances or with an error if happen */
    Result<Integer> deleteUserEntities(final String userUuid);

}
