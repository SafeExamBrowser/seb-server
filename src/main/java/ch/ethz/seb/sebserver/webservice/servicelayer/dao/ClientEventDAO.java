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
import java.util.function.Predicate;

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

    Result<ClientNotification> getPendingNotification(Long notificationId);

    Result<List<ClientNotification>> getPendingNotifications(Long clientConnectionId);

    Result<ClientNotification> confirmPendingNotification(Long notificationId, Long clientConnectionId);

}
