/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Service to maintain SEB Client notifications. */
public interface SEBClientNotificationService {

    public static final String CACHE_CLIENT_NOTIFICATION = "LIENT_NOTIFICATION_CACHE";

    /** Indicates whether the client connection with the specified identifier has any
     * pending notification or not. Pending means a non-confirmed notification
     *
     * @param clientConnectionId the client connection identifier
     * @return true if there is any pending notification for the specified client connection */
    @Cacheable(
            cacheNames = CACHE_CLIENT_NOTIFICATION,
            key = "#clientConnectionId",
            condition = "#result != null && #result")
    Boolean hasAnyPendingNotification(Long clientConnectionId);

    Result<List<ClientEvent>> getPendingNotifications(Long clientConnectionId);

    @CacheEvict(
            cacheNames = CACHE_CLIENT_NOTIFICATION,
            key = "#clientConnectionId")
    Result<ClientEvent> confirmPendingNotification(Long notificationId, final Long clientConnectionId);

    @CacheEvict(
            cacheNames = CACHE_CLIENT_NOTIFICATION,
            key = "#clientConnectionId")
    default void notifyNewNotification(final Long clientConnectionId) {
    }

}
