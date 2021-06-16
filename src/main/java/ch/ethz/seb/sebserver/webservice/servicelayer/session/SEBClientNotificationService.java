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
     *
     * @param clientConnectionId the client connection identifier
     * @return true if there is any pending notification for the specified client connection */
    Boolean hasAnyPendingNotification(final ClientConnection clientConnection);

    Result<List<ClientNotification>> getPendingNotifications(Long clientConnectionId);

    void confirmPendingNotification(ClientEvent event, String connectionToken);

    Result<ClientNotification> confirmPendingNotification(
            Long notificationId,
            Long examId,
            String connectionToken);

    void notifyNewNotification(final Long clientConnectionId);

}
