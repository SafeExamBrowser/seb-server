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
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;

public interface UserActivityLogDAO
        extends EntityDAO<UserActivityLog, UserActivityLog>, UserRelatedEntityDAO<UserActivityLog> {

    enum ActivityType {
        CREATE,
        IMPORT,
        MODIFY,
        DEACTIVATE,
        ACTIVATE,
        ARCHIVE,
        DELETE
    }

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message */
    <E extends Entity> Result<E> log(ActivityType activityType, E entity, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param actionType the action type
     * @param entity the Entity */
    <E extends Entity> Result<E> log(ActivityType activityType, E entity);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entityType the EntityType
     * @param message the message */
    void log(ActivityType activityType, EntityType entityType, String entityId, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entityType the EntityType
     * @param message the message */
    <T> Result<T> log(ActivityType activityType, EntityType entityType, String entityId, String message, T data);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message */
    <E extends Entity> Result<E> log(
            SEBServerUser user,
            ActivityType activityType,
            E entity,
            String message);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param activityType the activity type
     * @param entityType the entity type
     * @param entityId the entity id (primary key or UUID) */
    default <E extends Entity> Result<E> log(
            final SEBServerUser user,
            final ActivityType activityType,
            final E entity) {

        return log(user, activityType, entity, null);
    }

    Result<Collection<UserActivityLog>> all(
            Long InstitutionId,
            String userId,
            Long from,
            Long to,
            String activityTypes,
            String entityTypes,
            Predicate<UserActivityLog> predicate);

}
