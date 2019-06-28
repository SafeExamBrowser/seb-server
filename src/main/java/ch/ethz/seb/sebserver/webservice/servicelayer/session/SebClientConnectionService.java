/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SebClientConnectionService {

    Result<ClientConnection> createClientConnection(
            Long institutionId,
            String clientAddress,
            Long examId);

    Result<ClientConnection> establishClientConnection(
            final Long institutionId,
            String connectionToken,
            Long examId,
            final String clientAddress,
            String userSessionId);

    Result<ClientConnection> closeConnection(String connectionToken);

    void notifyPing(Long connectionId, long timestamp, int pingNumber);

    void notifyClientEvent(final ClientEvent event, Long connectionId);

}
