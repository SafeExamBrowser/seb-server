/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.List;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Service to maintain SEB Client notifications. */
public interface SEBClientNotificationService {

    public static final String CACHE_CLIENT_NOTIFICATION = "CLIENT_NOTIFICATION_CACHE";

    /** Indicates whether the client connection with the specified identifier has any
     * pending notification or not. Pending means a non-confirmed notification
     * This will only check client connections in ACTIVE state.
     *
     * @param clientConnectionId the client connection identifier
     * @return true if there is any pending notification for the specified client connection */
    Boolean hasAnyPendingNotification(final ClientConnection clientConnection);

    /** This gets a list of all pending notification for a given client connection independently
     * of it current status.
     *
     * @param clientConnectionId The identifier of the client connection
     * @return Result refer to pending client notifications or to an error when happened */
    Result<List<ClientNotification>> getPendingNotifications(Long clientConnectionId);

    /** This creates/register a new pending notification.
     *
     * @param notification The ClientNotification data */
    void newNotification(ClientNotification notification);

    /** This is used to confirm a pending notification from SEB client side where
     * a client event of type notification-confirm is involved
     *
     * @param event The notification confirmation event sent by a SEB client */
    void confirmPendingNotification(ClientEvent event);

    /** This is used to confirm a pending client notification from the SEB Server side
     *
     * @param notificationId The identifier of the client notification event
     * @param examId the exam identifier
     * @param connectionToken the SEB client connection token
     * @return Result refer to the confirmed client notification or to an error when happened */
    Result<ClientNotification> confirmPendingNotification(
            Long notificationId,
            Long examId,
            String connectionToken);

    //void notifyNewNotification(final Long clientConnectionId);

}
