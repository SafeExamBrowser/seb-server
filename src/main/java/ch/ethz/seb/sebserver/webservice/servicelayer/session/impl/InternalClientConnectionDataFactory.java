/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

@Lazy
@Service
@WebServiceProfile
public class InternalClientConnectionDataFactory {

    private final ClientIndicatorFactory clientIndicatorFactory;
    private final SEBClientNotificationService sebClientNotificationService;

    public InternalClientConnectionDataFactory(
            final ClientIndicatorFactory clientIndicatorFactory,
            final SEBClientNotificationService sebClientNotificationService) {

        this.clientIndicatorFactory = clientIndicatorFactory;
        this.sebClientNotificationService = sebClientNotificationService;
    }

    public ClientConnectionDataInternal createClientConnectionData(
            final ClientConnection clientConnection,
            final boolean examRunning) {

        // if the exam is not running, we just create a cached indicator anyways
        if (!examRunning) {
            return new ClientConnectionDataInternal(
                    clientConnection,
                    () -> false,
                    this.clientIndicatorFactory.createFor(clientConnection, true));
        }

        if (clientConnection.status == ConnectionStatus.CLOSED
                || clientConnection.status == ConnectionStatus.DISABLED) {

            // dispose notification indication for closed or disabled connection
            return new ClientConnectionDataInternal(
                    clientConnection,
                    () -> false,
                    this.clientIndicatorFactory.createFor(clientConnection));
        }

        return new ClientConnectionDataInternal(
                clientConnection,
                () -> this.sebClientNotificationService
                        .hasAnyPendingNotification(clientConnection),
                this.clientIndicatorFactory.createFor(clientConnection));
    }

}
