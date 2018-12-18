/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;

public interface UserActivityLogDAO extends UserRelatedEntityDAO<UserActivityLog> {

    enum ActivityType {
        CREATE,
        MODIFY,
        DEACTIVATE,
        ACTIVATE,
        ARCHIVE,
        DELETE
    }

    /** Creates a user activity log entry for the current user.
     *
     * @param actionType the action type
     * @param entity the Entity
     * @param message an optional message */
    <E extends Entity> Result<E> logUserActivity(ActivityType actionType, E entity, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param actionType the action type
     * @param entity the Entity */
    <E extends Entity> Result<E> logUserActivity(ActivityType actionType, E entity);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param actionType the action type
     * @param entity the Entity
     * @param message an optional message */
    <E extends Entity> Result<E> logUserActivity(
            SEBServerUser user,
            ActivityType actionType,
            E entity,
            String message);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param actionType the action type
     * @param entityType the entity type
     * @param entityId the entity id (primary key or UUID) */
    default <E extends Entity> Result<E> logUserActivity(
            final SEBServerUser user,
            final ActivityType actionType,
            final E entity) {

        return logUserActivity(user, actionType, entity, null);
    }

    Result<Collection<UserActivityLog>> all(
            String userId,
            Long from,
            Long to,
            Predicate<UserActivityLogRecord> predicate);

}
