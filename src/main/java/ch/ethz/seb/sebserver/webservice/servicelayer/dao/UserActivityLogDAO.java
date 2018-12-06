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

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserLogRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;

public interface UserActivityLogDAO extends UserRelatedEntityDAO<UserActivityLog> {

    enum ActionType {
        CREATE,
        MODIFY,
        DEACTIVATE,
        ACTIVATE,
        ARCHIVE,
        DELETE
    }

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param actionType the action type
     * @param entityType the entity type
     * @param entityId the entity id (primary key or UUID)
     * @param message an optional message */
    void logUserActivity(
            SEBServerUser user,
            ActionType actionType,
            EntityType entityType,
            String entityId,
            String message);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param actionType the action type
     * @param entityType the entity type
     * @param entityId the entity id (primary key or UUID) */
    default void logUserActivity(
            final SEBServerUser user,
            final ActionType actionType,
            final EntityType entityType,
            final String entityId) {

        logUserActivity(user, actionType, entityType, entityId);
    }

    Result<Collection<UserActivityLog>> allForUser(
            final String userId,
            Predicate<UserLogRecord> predicate);

    Result<Collection<UserActivityLog>> allBetween(
            Long from,
            Long to,
            Predicate<UserLogRecord> predicate);

    Result<Collection<UserActivityLog>> allForBetween(
            String userId,
            Long from,
            Long to,
            Predicate<UserLogRecord> predicate);

}
