/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.PreDestroy;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification.NotificationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

@Lazy
@Component
@WebServiceProfile
public class SEBClientEventBatchService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientEventBatchService.class);

    private final SEBClientNotificationService sebClientNotificationService;
    private final TransactionTemplate transactionTemplate;
    private final ExamSessionCacheService examSessionCacheService;
    private final JSONMapper jsonMapper;

    private final SqlSessionTemplate sqlSessionTemplate;
    private final ClientEventRecordMapper clientEventMapper;

    public SEBClientEventBatchService(
            final SEBClientNotificationService sebClientNotificationService,
            final SqlSessionFactory sqlSessionFactory,
            final PlatformTransactionManager transactionManager,
            final ExamSessionCacheService examSessionCacheService,
            final JSONMapper jsonMapper) {

        this.sebClientNotificationService = sebClientNotificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.examSessionCacheService = examSessionCacheService;
        this.jsonMapper = jsonMapper;

        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
        this.clientEventMapper = this.sqlSessionTemplate.getMapper(ClientEventRecordMapper.class);
    }

    private final BlockingDeque<EventData> eventDataQueue = new LinkedBlockingDeque<>();

    public void accept(final String connectionToken, final String jsonBody) {
        this.eventDataQueue.add(new EventData(
                connectionToken,
                Utils.getMillisecondsNow(),
                jsonBody));
    }

    public void accept(final EventData eventData) {
        this.eventDataQueue.add(eventData);
    }

    private final Collection<EventData> events1 = new ArrayList<>();

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.exam.session.event.batch.interval:1000}",
            initialDelay = 100)
    public void worker1() {
        processEvents("worker1", this.events1);
    }

    private final Collection<EventData> events2 = new ArrayList<>();

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.exam.session.event.batch.interval:1000}",
            initialDelay = 300)
    public void worker2() {
        processEvents("worker2", this.events2);
    }

    private final Collection<EventData> events3 = new ArrayList<>();

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.exam.session.event.batch.interval:1000}",
            initialDelay = 600)
    public void worker3() {
        processEvents("worker3", this.events3);
    }

    private final Collection<EventData> events4 = new ArrayList<>();

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.api.exam.session.event.batch.interval:1000}",
            initialDelay = 900)
    public void worker4() {
        processEvents("worker4", this.events4);
    }

    public void processOneTime() {
        processEvents("One Time Call", new ArrayList<>());
    }

    private void processEvents(final String workerName, final Collection<EventData> events) {

        long start = 0L;
        if (log.isDebugEnabled()) {
            start = Utils.getMillisecondsNow();
        }

        final int size = this.eventDataQueue.size();
        if (size > 1000) {
            log.warn("-----> There are more then 1000 SEB client logs in the waiting queue: {}, worker: {}",
                    size,
                    workerName);
        }

        if (size == 0) {
            return;
        }

        try {

            events.clear();
            this.eventDataQueue.drainTo(events);

            if (events.isEmpty()) {
                return;
            }

            final List<ClientEventRecord> records = events
                    .stream()
                    .map(this::convertData)
                    .map(this::storeNotifications)
                    .filter(Objects::nonNull)
                    .map(this::toEventRecord)
                    .filter(Objects::nonNull)
                    .toList();

            this.transactionTemplate
                    .execute(status -> {
                        records.forEach(this.clientEventMapper::insert);
                        return null;
                    });

            this.sqlSessionTemplate.flushStatements();

            if (log.isTraceEnabled()) {
                log.debug("SEBClientEventBatchService worker {} processes batch of size {} in {} ms",
                        workerName,
                        size,
                        Utils.getMillisecondsNow() - start);
            }

        } catch (final Exception e) {
            log.error("Failed to process SEB events from eventDataQueue: ", e);
        }
    }

    private EventData convertData(final EventData eventData) {
        if (eventData == null || eventData.jsonBody == null) {
            return eventData;
        }

        try {

            final ClientEvent eventModel = this.jsonMapper.readValue(
                    eventData.jsonBody,
                    ClientEvent.class);

            eventData.setEvent(eventModel);
            return eventData;

        } catch (final Exception e) {
            log.error("Failed to convert SEB event JSON data to internal data for: {}", eventData);
            return eventData;
        }
    }

    private EventData storeNotifications(final EventData eventData) {
        try {

            if (!eventData.event.eventType.isNotificationEvent) {
                return eventData;
            }

            final ClientConnectionDataInternal clientConnection = this.examSessionCacheService
                    .getClientConnection(eventData.connectionToken);

            if (clientConnection == null) {
                log.error("Failed to get ClientConnectionDataInternal for: {}", eventData.connectionToken);
                return null;
            }

            final Pair<NotificationType, String> typeAndPlainText =
                    ClientNotification.extractTypeAndPlainText(eventData.event.text);
            final ClientNotification notification = new ClientNotification(
                    eventData.event.id,
                    clientConnection.getConnectionId(),
                    eventData.event.eventType,
                    eventData.event.getClientTime(),
                    eventData.event.getServerTime(),
                    (eventData.event.numValue != null) ? eventData.event.numValue : null,
                    typeAndPlainText.b,
                    typeAndPlainText.a);

            switch (notification.eventType) {
                case NOTIFICATION: {
                    this.sebClientNotificationService.newNotification(notification);
                    break;
                }
                case NOTIFICATION_CONFIRMED: {
                    this.sebClientNotificationService.confirmPendingNotification(notification);
                    break;
                }
                default:
            }

            // skip this for further event processing
            return null;

        } catch (final Exception e) {
            log.error("Failed to verify and process notification for SEB event: {}", eventData, e);
            return eventData;
        }
    }

    private ClientEventRecord toEventRecord(final EventData eventData) {
        try {

            final ClientConnectionDataInternal clientConnection = this.examSessionCacheService
                    .getClientConnection(eventData.connectionToken);

            if (clientConnection == null) {
                log.warn("Failed to retrieve ClientConnection for token {}. Skip this event",
                        eventData.connectionToken);
                return null;
            }

            // handle indicator update
            clientConnection
                    .getIndicatorMapping(eventData.event.eventType)
                    .forEach(indicator -> indicator.notifyValueChange(
                            eventData.event.text,
                            (eventData.event.numValue != null) ? eventData.event.numValue : Double.NaN));

            return ClientEvent.toRecord(eventData.event, clientConnection.clientConnection.id);

        } catch (final Exception e) {
            log.error(
                    "Unexpected error while converting SEB event data to record for: {} Skip this event", eventData,
                    e);
            return null;
        }
    }

    @PreDestroy
    protected void shutdown() {
        log.info("Shutdown SEBClientEventBatchStore...");
        if (this.sqlSessionTemplate != null) {
            try {
                this.sqlSessionTemplate.destroy();
            } catch (final Exception e) {
                log.error("Failed to close and destroy the SqlSessionTemplate for this thread: {}",
                        Thread.currentThread(),
                        e);
            }
        }
    }

    public final static class EventData {
        final String connectionToken;
        final Long serverTime;
        final String jsonBody;
        ClientEvent event;

        public EventData(final String connectionToken, final Long serverTime, final String jsonBody) {
            this.connectionToken = connectionToken;
            this.serverTime = serverTime;
            this.jsonBody = jsonBody;
            this.event = null;
        }

        public EventData(final String connectionToken, final Long serverTime, final ClientEvent event) {
            this.connectionToken = connectionToken;
            this.serverTime = serverTime;
            this.jsonBody = null;
            this.event = event;
        }

        void setEvent(final ClientEvent event) {
            this.event = event;
        }
    }

}
