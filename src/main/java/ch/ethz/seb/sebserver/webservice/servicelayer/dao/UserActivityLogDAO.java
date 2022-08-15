/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;

/** Concrete EntityDAO interface of UserActivityLog entities */
public interface UserActivityLogDAO extends
        EntityDAO<UserActivityLog, UserActivityLog>,
        UserRelatedEntityDAO<UserActivityLog> {

    /** Create a user activity log entry for the current users login action
     *
     * @param user the UserInfo
     * @return Result of the Entity or referring to an Error if happened */
    Result<UserInfo> logLogin(UserInfo user);

    /** Create a user activity log entry for the current user logut action
     *
     * @param user the UserInfo
     * @return Result of the Entity or referring to an Error if happened */
    Result<UserInfo> logLogout(UserInfo user);

    /** Create a user activity log entry for the current user of activity type CREATE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logCreate(E entity);

    /** Create a user activity log entry for a user registration event
     *
     * @param account the UserAccount
     * @return Result of the UserAccount or referring to an Error if happened */
    Result<UserAccount> logRegisterAccount(UserAccount account);

    /** Creates a user activity log entry for SEB Exam Configuration save in history action
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logSaveToHistory(E entity);

    /** Creates a user activity log entry for SEB Exam Configuration undo action
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logUndo(E entity);

    /** Create a user activity log entry for the current user of activity type IMPORT
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logImport(E entity);

    /** Create a user activity log entry for the current user of activity type EXPORT
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logExport(E entity);

    /** Create a user activity log entry for the current user of activity type MODIFY
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logModify(E entity);

    /** Create a user activity log entry for the current user of activity type FINISHED
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logFinished(E entity);

    /** Create a user activity log entry for the current user of activity type DELETE
     *
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> logDelete(E entity);

    /** Create a user activity log entry for the current user of activity type DELETE
     *
     * @param entityKey the EntityKey of the deleted object
     * @return Result of the EntityKey or referring to an Error if happened */
    Result<EntityKey> logDelete(EntityKey entityKey);

    /** Used to log a successful bulk action and uses the EntityProcessingReport from the
     * bulk action to log all details.
     *
     * @param bulkActionReport the bulk action report containing all processed entity keys for logging
     * @return Result refer to the given EntityProcessingReport or to an error when happened */
    Result<EntityProcessingReport> logBulkAction(EntityProcessingReport bulkActionReport);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> log(UserLogActivityType activityType, E entity, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entity the Entity
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> log(UserLogActivityType activityType, E entity);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entityType the EntityType
     * @param message the message */
    void log(UserLogActivityType activityType, EntityType entityType, String entityId, String message);

    /** Creates a user activity log entry for the current user.
     *
     * @param activityType the activity type
     * @param entityType the EntityType
     * @param message the message
     * @return Result of the Entity or referring to an Error if happened */
    <T> Result<T> log(UserLogActivityType activityType, EntityType entityType, String entityId, String message, T data);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param activityType the activity type
     * @param entity the Entity
     * @param message an optional message
     * @return Result of the Entity or referring to an Error if happened */
    <E extends Entity> Result<E> log(
            SEBServerUser user,
            UserLogActivityType activityType,
            E entity,
            String message);

    /** Creates a user activity log entry.
     *
     * @param user for specified SEBServerUser instance
     * @param activityType the activity type
     * @param entity the entity instance
     * @return Result of the Entity or referring to an Error if happened */
    default <E extends Entity> Result<E> log(
            final SEBServerUser user,
            final UserLogActivityType activityType,
            final E entity) {

        return log(user, activityType, entity, null);
    }

}
