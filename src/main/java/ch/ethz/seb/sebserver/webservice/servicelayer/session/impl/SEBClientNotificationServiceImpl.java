/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

@Lazy
@Service
@WebServiceProfile
public class SEBClientNotificationServiceImpl implements SEBClientNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientNotificationServiceImpl.class);

    private static final String CONFIRM_INSTRUCTION_ATTR_ID = "id";
    private static final String CONFIRM_INSTRUCTION_ATTR_TYPE = "type";

    private final ClientEventDAO clientEventDAO;
    private final SEBClientInstructionService sebClientInstructionService;
    private final Set<Long> pendingNotifications = new HashSet<>();

    public SEBClientNotificationServiceImpl(
            final ClientEventDAO clientEventDAO,
            final SEBClientInstructionService sebClientInstructionService) {

        this.clientEventDAO = clientEventDAO;
        this.sebClientInstructionService = sebClientInstructionService;
    }

    // TODO find a better (faster) way to update pending notifications
    // get them all at certain interval with background update for example
    // or update them all on every monitoring call

    @Override
    public Boolean hasAnyPendingNotification(final Long clientConnectionId) {

        if (this.pendingNotifications.contains(clientConnectionId)) {
            return true;
        }

        final boolean hasAnyPendingNotification = !getPendingNotifications(clientConnectionId)
                .getOr(Collections.emptyList())
                .isEmpty();

        if (hasAnyPendingNotification) {
            // NOTE this is a quick and dirty way to keep cache pendingNotifications cache size short.
            // TODO find a better way to do this.
            if (this.pendingNotifications.size() > 100) {
                this.pendingNotifications.clear();
            }
            this.pendingNotifications.add(clientConnectionId);
        }

        return hasAnyPendingNotification;
    }

    @Override
    public Result<List<ClientNotification>> getPendingNotifications(final Long clientConnectionId) {
        return this.clientEventDAO.getPendingNotifications(clientConnectionId);
    }

    @Override
    public void confirmPendingNotification(final ClientEvent event, final String connectionToken) {
        try {
            final Long notificationId = (long) event.getValue();

            this.clientEventDAO.getPendingNotification(notificationId)
                    .flatMap(notification -> this.clientEventDAO.confirmPendingNotification(
                            notificationId,
                            notification.connectionId))
                    .map(this::removeFromCache);

        } catch (final Exception e) {
            log.error(
                    "Failed to confirm pending notification from SEB Client side. Connection token: {} confirm event: {}",
                    connectionToken, event, e);
        }
    }

    @Override
    public Result<ClientNotification> confirmPendingNotification(
            final Long notificationId,
            final Long examId,
            final String connectionToken) {

        return this.clientEventDAO.getPendingNotification(notificationId)
                .map(notification -> this.confirmClientSide(notification, examId, connectionToken))
                .flatMap(notification -> this.clientEventDAO.confirmPendingNotification(
                        notificationId,
                        notification.connectionId))
                .map(this::removeFromCache);
    }

    @Override
    public void notifyNewNotification(final Long clientConnectionId) {
        this.pendingNotifications.add(clientConnectionId);
    }

    private ClientNotification confirmClientSide(
            final ClientNotification notification,
            final Long examId,
            final String connectionToken) {

        // create and send confirming SEB instruction to confirm the notification on client side
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(CONFIRM_INSTRUCTION_ATTR_TYPE, notification.getNotificationType().typeName);
        attributes.put(CONFIRM_INSTRUCTION_ATTR_ID, String.valueOf((int) notification.getValue()));
        final ClientInstruction clientInstruction = new ClientInstruction(
                null,
                examId,
                InstructionType.NOTIFICATION_CONFIRM,
                connectionToken,
                attributes);
        this.sebClientInstructionService.registerInstruction(clientInstruction);

        return notification;
    }

    private ClientNotification removeFromCache(final ClientNotification notification) {
        this.pendingNotifications.remove(notification.connectionId);
        return notification;
    }

}
