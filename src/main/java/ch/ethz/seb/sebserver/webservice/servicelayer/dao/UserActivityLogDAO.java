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

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;

/** Concrete EntityDAO interface of UserActivityLog entities */
public interface UserActivityLogDAO extends
        EntityDAO<UserActivityLog, UserActivityLog>,
        UserRelatedEntityDAO<UserActivityLog> {

    /** All activity types */
    enum ActivityType {
        CREATE,
        IMPORT,
        MODIFY,
        PASSWORD_CHANGE,
        DEACTIVATE,
        ACTIVATE,
        DELETE
    }

    /** Create a user activity log entry for the current user of activity type CREATE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logCreate(E entity);

    /** Create a user activity log entry for the current user of activity type IMPORT
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logImport(E entity);

    /** Create a user activity log entry for the current user of activity type MODIFY
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logModify(E entity);

    /** Create a user activity log entry for the current user of activity type ACTIVATE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logActivate(E entity);

    /** Create a user activity log entry for the current user of activity type DEACTIVATE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logDeactivate(E entity);

    /** Create a user activity log entry for the current user of activity type DELETE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
    public <E extends Entity> Result<E> logDelete(E entity);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message
     * @return Result of the Entity or referring to an Error id happened */
    <E extends Entity> Result<E> log(ActivityType activityType, E entity, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param actionType the action type
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error id happened */
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
     * @param message the message
     * @return Result of the Entity or referring to an Error id happened */
    <T> Result<T> log(ActivityType activityType, EntityType entityType, String entityId, String message, T data);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message
     * @return Result of the Entity or referring to an Error id happened */
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
     * @param entityId the entity id (primary key or UUID)
     * @return Result of the Entity or referring to an Error id happened */
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
