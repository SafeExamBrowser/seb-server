/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.PreDestroy;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification.NotificationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientNotificationService;

/** Approach 2 to handle/save client events internally
 *
 * This Approach uses a queue to collect ClientEvents that are stored later. The queue is shared between some
 * worker-threads that batch gets and stores the events from the queue afterwards. this approach is less blocking from
 * the caller perspective and also faster on store data by using bulk-insert
 *
 * A disadvantage is an potentially multiple event data loss on total server fail. The data in the queue is state that
 * is not stored somewhere yet and can't be recovered on total server fail.
 *
 * If the performance of this approach is not enough or the potentially data loss on total server fail is a risk that
 * not can be taken, we have to consider using a messaging system/server like rabbitMQ or Apache-Kafka that brings the
 * ability to effectively store and recover message queues but also comes with more complexity on setup and installation
 * side as well as for the whole server system. */
@Lazy
@Component(EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_ASYNC_BATCH_STORE)
@WebServiceProfile
public class AsyncBatchEventSaveStrategy implements EventHandlingStrategy {

    private static final Logger log = LoggerFactory.getLogger(AsyncBatchEventSaveStrategy.class);

    private static final int NUMBER_OF_WORKER_THREADS = 4;
    private static final int BATCH_SIZE = 100;
    private static final int MIN_SLEEP_TIME = 100;
    private static final int SLEEP_TIME_EXPAND = 100;
    private static final int MAX_SLEEP_TIME = 5000;

    private final SEBClientNotificationService sebClientNotificationService;
    private final SqlSessionFactory sqlSessionFactory;
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;

    private final BlockingDeque<ClientEventRecord> eventQueue = new LinkedBlockingDeque<>();
    private final BlockingDeque<ClientNotification> notificationQueue = new LinkedBlockingDeque<>();
    private boolean workersRunning = false;
    private boolean enabled = false;

    public AsyncBatchEventSaveStrategy(
            final SEBClientNotificationService sebClientNotificationService,
            final SqlSessionFactory sqlSessionFactory,
            final PlatformTransactionManager transactionManager,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_EXECUTOR_BEAN_NAME) final Executor executor) {

        this.sebClientNotificationService = sebClientNotificationService;
        this.sqlSessionFactory = sqlSessionFactory;
        this.executor = executor;

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @EventListener(SEBServerInitEvent.class)
    protected void recover() {
        if (this.enabled) {
            SEBServerInit.INIT_LOGGER.info("------>");
            SEBServerInit.INIT_LOGGER.info("------> Start {} Event-Batch-Store Worker-Threads",
                    NUMBER_OF_WORKER_THREADS);

            runWorkers();

            try {
                Thread.sleep(Constants.SECOND_IN_MILLIS / 2);
            } catch (final Exception e) {
                log.error("Failed to wait");
            }
        }
    }

    @PreDestroy
    protected void shutdown() {
        log.info("Reset workersRunning flag to stop worker after event queue is empty");
        this.workersRunning = false;
    }

    @Override
    public void accept(final ClientEventRecord record) {
        if (record == null || !this.workersRunning) {
            return;
        }

        if (EventType.isNotificationEvent(record.getType())) {
            final Pair<NotificationType, String> typeAndPlainText =
                    ClientNotification.extractTypeAndPlainText(record.getText());
            this.notificationQueue.add(new ClientNotification(
                    record.getId(),
                    record.getClientConnectionId(),
                    EventType.byId(record.getType()),
                    record.getClientTime(),
                    record.getServerTime(),
                    (record.getNumericValue() != null) ? record.getNumericValue().doubleValue() : null,
                    typeAndPlainText.b,
                    typeAndPlainText.a));
        } else {
            this.eventQueue.add(record);
        }
    }

    private void runWorkers() {
        if (this.workersRunning) {
            log.warn("runWorkers called when workers are running already. Ignore that");
            return;
        }

        this.workersRunning = true;
        for (int i = 0; i < NUMBER_OF_WORKER_THREADS; i++) {
            this.executor.execute(batchSave());
        }
    }

    private Runnable batchSave() {
        return () -> {

            SEBServerInit.INIT_LOGGER.info("> Worker Thread {} running", Thread.currentThread());

            final Collection<ClientEventRecord> events = new ArrayList<>();
            @SuppressWarnings("resource")
            final SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(
                    this.sqlSessionFactory,
                    ExecutorType.BATCH);
            final ClientEventRecordMapper clientEventMapper = sqlSessionTemplate.getMapper(
                    ClientEventRecordMapper.class);

            long sleepTime = MIN_SLEEP_TIME;

            try {
                while (this.workersRunning) {
                    events.clear();
                    this.eventQueue.drainTo(events, BATCH_SIZE);

                    // batch store log events
                    try {
                        if (!events.isEmpty()) {
                            sleepTime = MIN_SLEEP_TIME;
                            this.transactionTemplate
                                    .execute(status -> {
                                        events.forEach(clientEventMapper::insert);
                                        return null;
                                    });

                            sqlSessionTemplate.flushStatements();
                        } else if (sleepTime < MAX_SLEEP_TIME) {
                            sleepTime += SLEEP_TIME_EXPAND;
                        }
                    } catch (final Exception e) {
                        log.error("unexpected Error while trying to batch store client-events: ", e);
                    }

                    // store notification events
                    if (!this.notificationQueue.isEmpty()) {
                        try {

                            final ClientNotification notification = this.notificationQueue.poll();
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
                        } catch (final Exception e) {
                            log.error("unexpected Error while trying to  store client-notification: ", e);
                        }
                    }

                    try {
                        Thread.sleep(sleepTime);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    sqlSessionTemplate.destroy();
                } catch (final Exception e) {
                    log.error("Failed to close and destroy the SqlSessionTemplate for this thread: {}",
                            Thread.currentThread(),
                            e);
                }
                log.debug("Worker Thread {} stopped", Thread.currentThread());
            }
        };
    }

}
