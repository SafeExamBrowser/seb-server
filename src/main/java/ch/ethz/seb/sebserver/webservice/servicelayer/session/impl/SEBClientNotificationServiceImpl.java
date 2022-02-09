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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
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
    private final ClientConnectionDAO clientConnectionDAO;
    private final SEBClientInstructionService sebClientInstructionService;
    private final Set<Long> pendingNotifications = new HashSet<>();
    private final Set<Long> examUpdate = new HashSet<>();

    private long lastUpdate = 0;
    private long updateInterval = 5 * Constants.SECOND_IN_MILLIS;

    public SEBClientNotificationServiceImpl(
            final ClientEventDAO clientEventDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final SEBClientInstructionService sebClientInstructionService,
            final WebserviceInfo webserviceInfo) {

        this.clientEventDAO = clientEventDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.sebClientInstructionService = sebClientInstructionService;
        if (webserviceInfo.isDistributed()) {
            this.updateInterval = 2 * Constants.SECOND_IN_MILLIS;
        }
    }

    @Override
    public Boolean hasAnyPendingNotification(final ClientConnection clientConnection) {
        updateCache(clientConnection.examId);
        return this.pendingNotifications.contains(clientConnection.id);
    }

    @Override
    public Result<List<ClientNotification>> getPendingNotifications(final Long clientConnectionId) {
        return this.clientEventDAO.getPendingNotifications(clientConnectionId);
    }

    @Override
    public void confirmPendingNotification(final ClientEvent event) {
        try {

            final ClientConnection clientConnection = this.clientConnectionDAO
                    .byPK(event.connectionId)
                    .getOrThrow();
            final Long notificationId = (long) event.getValue();

            this.clientEventDAO.getPendingNotificationByValue(clientConnection.id, notificationId)
                    .flatMap(notification -> this.clientEventDAO.confirmPendingNotification(notification.id))
                    .map(this::removeFromCache)
                    .onError(error -> log.error("Failed to confirm pending notification: {}", event, error));

        } catch (final Exception e) {
            log.error(
                    "Failed to confirm pending notification from SEB Client side. event: {}", event, e);
        }
    }

    @Override
    public Result<ClientNotification> confirmPendingNotification(
            final Long notificationId,
            final Long examId,
            final String connectionToken) {

        return this.clientEventDAO.getPendingNotification(notificationId)
                .map(notification -> this.confirmClientSide(notification, examId, connectionToken))
                .flatMap(notification -> this.clientEventDAO.confirmPendingNotification(notificationId))
                .map(this::removeFromCache)
                .onError(error -> log.error("Failed to confirm pending notification: {}", notificationId, error));
    }

    @Override
    public void newNotification(final ClientNotification notification) {
        this.clientEventDAO.createNewNotification(notification)
                .map(this::notifyNewNotifiaction)
                .onError(error -> log.error("Failed to store new client notification: {}", notification, error));
    }

    @EventListener(ExamDeletionEvent.class)
    public void notifyExamDeletionEvent(final ExamDeletionEvent event) {
        // delete all notifications for given exams
        event.ids.forEach(id -> this.clientEventDAO.getNotificationIdsForExam(id)
                .flatMap(this.clientEventDAO::deleteClientNotification)
                .map(deleted -> {
                    log.debug("Deleted client notifications during exam deletion: {}", deleted);
                    return deleted;
                })
                .onError(error -> log.error("Failed to delete client notifications for exam: {}", id, error)));
    }

    private ClientNotification notifyNewNotifiaction(final ClientNotification notification) {
        if (notification.eventType == EventType.NOTIFICATION) {
            this.pendingNotifications.add(notification.getConnectionId());
        }
        return notification;
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

    private final void updateCache(final Long examId) {
        if (System.currentTimeMillis() - this.lastUpdate > this.updateInterval) {
            this.examUpdate.clear();
            this.pendingNotifications.clear();
            this.lastUpdate = System.currentTimeMillis();
        }

        if (!this.examUpdate.contains(examId)) {
            this.pendingNotifications.addAll(
                    this.clientEventDAO
                            .getClientConnectionIdsWithPendingNotification(examId)
                            .getOr(Collections.emptySet()));
            this.examUpdate.add(examId);
        }
    }

}
