/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ClientEventDAO extends EntityDAO<ClientEvent, ClientEvent> {

    /** Use this to get all matching ExtendedClientEvent from persistent storage.
     *
     * @param filterMap the FilterMap containing all the filter criteria
     * @param predicate an additional predicate to filter the list
     * @return Result refer to all matching ExtendedClientEvent from persistent storage or to an error if happened. */
    Result<Collection<ExtendedClientEvent>> allMatchingExtended(
            FilterMap filterMap,
            Predicate<ExtendedClientEvent> predicate);

    /** Used to create/insert a new client notification.
     *
     * @param notification The client notification model data
     * @return Result refer to the resulting client notification or to an error when appened. */
    Result<ClientNotification> createNewNotification(ClientNotification notification);

    /** Get a specified notification by id (PK)
     *
     * @param notificationId The PK of the notification
     * @return Result refer to the specified ClientNotification or to an error when happened */
    Result<ClientNotification> getPendingNotification(Long notificationId);

    /** Get a pending notification by the notification value identifier (sent by SEB)
     *
     * @param notificationValueId notification value identifier (sent by SEB)
     * @return Result refer to the specified ClientNotification or to an error when happened */
    Result<ClientNotification> getPendingNotificationByValue(
            Long clientConnectionId,
            Long notificationValueId);

    /** Get all pending notifications for a given client connection.
     *
     * @param clientConnectionId The client connection identifier
     * @return Result refer to the list of pending notifications or to an error when happened */
    Result<List<ClientNotification>> getPendingNotifications(Long clientConnectionId);

    /** Get all identifiers (PKs) of client connections of a given exam that has any pending notification
     *
     * @param examId the exam identifier
     * @return Result refer to a set of identifiers of client connections or to an error when happened */
    Result<Set<Long>> getClientConnectionIdsWithPendingNotification(Long examId);

    /** Used to confirm a pending notification so that the notification is not pending anymore
     *
     * @param notificationId the notification identifier
     * @return Result refer to the confirmed notification or to en error when happened */
    Result<ClientNotification> confirmPendingNotification(Long notificationId);

    /** Used to get all client notification identifiers/PKs that are mapped to a specified exam.
     *
     * @param examId The identifier/PK of the exam
     * @return Result refer to a set of client notification identifiers or to an error when happened */
    Result<Set<EntityKey>> getNotificationIdsForExam(Long examId);

    Result<Collection<EntityKey>> deleteClientNotification(Set<EntityKey> keys);

}
