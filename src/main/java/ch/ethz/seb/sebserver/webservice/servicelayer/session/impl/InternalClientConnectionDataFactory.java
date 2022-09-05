/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.monitoring.ClientGroupMatcherService;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

@Lazy
@Service
@WebServiceProfile
public class InternalClientConnectionDataFactory {

    private static final Logger log = LoggerFactory.getLogger(InternalClientConnectionDataFactory.class);

    private final ClientIndicatorFactory clientIndicatorFactory;
    private final SEBClientNotificationService sebClientNotificationService;
    private final ClientGroupDAO clientGroupDAO;
    private final ClientGroupMatcherService clientGroupMatcherService;

    public InternalClientConnectionDataFactory(
            final ClientIndicatorFactory clientIndicatorFactory,
            final SEBClientNotificationService sebClientNotificationService,
            final ClientGroupDAO clientGroupDAO,
            final ClientGroupMatcherService clientGroupMatcherService) {

        this.clientIndicatorFactory = clientIndicatorFactory;
        this.sebClientNotificationService = sebClientNotificationService;
        this.clientGroupDAO = clientGroupDAO;
        this.clientGroupMatcherService = clientGroupMatcherService;
    }

    public ClientConnectionDataInternal createClientConnectionData(final ClientConnection clientConnection) {

        ClientConnectionDataInternal result;
        if (clientConnection.status == ConnectionStatus.CLOSED
                || clientConnection.status == ConnectionStatus.DISABLED) {

            // dispose notification indication for closed or disabled connection
            result = new ClientConnectionDataInternal(
                    clientConnection,
                    () -> false,
                    this.clientIndicatorFactory.createFor(clientConnection));
        } else {

            result = new ClientConnectionDataInternal(
                    clientConnection,
                    () -> this.sebClientNotificationService
                            .hasAnyPendingNotification(clientConnection),
                    this.clientIndicatorFactory.createFor(clientConnection));
        }

        // set client groups for connection
        if (clientConnection.examId != null) {
            final Collection<ClientGroup> clientGroups = this.clientGroupDAO
                    .allForExam(clientConnection.examId)
                    .onError(
                            error -> log.error("Failed to get client groups for clientConnection: {}", clientConnection,
                                    error))
                    .getOr(Collections.emptyList());

            if (!clientGroups.isEmpty()) {
                clientGroups.forEach(clientGroup -> {
                    if (this.clientGroupMatcherService.isInGroup(clientConnection, clientGroup)) {
                        result.addToClientGroup(clientGroup);
                    }
                });
            }
        }

        return result;
    }

}
