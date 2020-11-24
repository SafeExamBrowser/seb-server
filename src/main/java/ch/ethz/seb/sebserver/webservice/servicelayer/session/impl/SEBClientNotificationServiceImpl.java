/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

@Lazy
@Service
@WebServiceProfile
public class SEBClientNotificationServiceImpl implements SEBClientNotificationService {

    private final ClientEventDAO clientEventDAO;

    public SEBClientNotificationServiceImpl(final ClientEventDAO clientEventDAO) {
        this.clientEventDAO = clientEventDAO;
    }

    @Override
    public Boolean hasAnyPendingNotification(final Long clientConnectionId) {
        return !getPendingNotifications(clientConnectionId)
                .getOr(Collections.emptyList())
                .isEmpty();
    }

    @Override
    public Result<List<ClientEvent>> getPendingNotifications(final Long clientConnectionId) {
        return this.clientEventDAO.getPendingNotifications(clientConnectionId);
    }

    @Override
    public Result<ClientEvent> confirmPendingNotification(final Long notificatioId, final Long clientConnectionId) {
        return this.clientEventDAO.confirmPendingNotification(notificatioId, clientConnectionId);
    }

}
