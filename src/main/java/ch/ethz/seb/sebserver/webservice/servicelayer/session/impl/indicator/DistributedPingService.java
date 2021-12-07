/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientPingMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientPingMapper.ClientLastPingRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientIndicatorRecord;

@Lazy
@Component
@WebServiceProfile
/** This service is only needed within a distributed setup where more then one webservice works
 * simultaneously within one SEB Server and one persistent storage.
 * </p>
 * This service handles the SEB client ping updates within such a setup and implements functionality to
 * efficiently store and load ping time indicators form and to shared store.
 * </p>
 * The update from the persistent store is done periodically within a batch while the ping time writes
 * are done individually per SEB client when they arrive but within a dedicated task executor with minimal task
 * queue to do not overflow other executor services when it comes to a leak on storing lot of ping times.
 * In this case some ping time updates will be just dropped and not go to the persistent store until the leak
 * is resolved.
 * </p>
 * Note that the ping time update and read operations are also not within a transaction for performance reasons
 * and because it is not a big deal to loose one ore two ping updates for a SEB client. */
public class DistributedPingService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DistributedPingService.class);

    private final Executor pingUpdateExecutor;
    private final ClientIndicatorRecordMapper clientIndicatorRecordMapper;
    private final ClientPingMapper clientPingMapper;
    private long pingUpdateTolerance;

    private ScheduledFuture<?> taskRef;
    private final Map<Long, Long> pingCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0L;

    public DistributedPingService(
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME) final Executor pingUpdateExecutor,
            final ClientIndicatorRecordMapper clientIndicatorRecordMapper,
            final ClientPingMapper clientPingMapper) {

        this.pingUpdateExecutor = pingUpdateExecutor;
        this.clientIndicatorRecordMapper = clientIndicatorRecordMapper;
        this.clientPingMapper = clientPingMapper;
    }

    /** Initializes the service by attaching it to the scheduler for periodical update.
     * If the webservice is not initialized within a distributed setup, this will do nothing
     *
     * @param initEvent the SEB Server webservice init event */
    @EventListener(SEBServerInitEvent.class)
    public void init(final SEBServerInitEvent initEvent) {
        final ApplicationContext applicationContext = initEvent.webserviceInit.getApplicationContext();
        final WebserviceInfo webserviceInfo = applicationContext.getBean(WebserviceInfo.class);
        if (webserviceInfo.isDistributed()) {

            SEBServerInit.INIT_LOGGER.info("------>");
            SEBServerInit.INIT_LOGGER.info("------> Activate distributed ping service:");

            final TaskScheduler taskScheduler = applicationContext.getBean(TaskScheduler.class);
            final long distributedPingUpdateInterval = webserviceInfo.getDistributedPingUpdateInterval();
            this.pingUpdateTolerance = distributedPingUpdateInterval * 2 / 3;

            SEBServerInit.INIT_LOGGER.info("------> with distributedPingUpdateInterval: {}",
                    distributedPingUpdateInterval);
            SEBServerInit.INIT_LOGGER.info("------> with taskScheduler: {}", taskScheduler);

            try {
                this.taskRef = taskScheduler.scheduleAtFixedRate(
                        this::updatePingCache,
                        distributedPingUpdateInterval);

                SEBServerInit.INIT_LOGGER.info("------> distributed ping service successfully initialized!");

            } catch (final Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to initialize distributed ping service:", e);
                log.error("Failed to initialize distributed ping cache update task");
                this.taskRef = null;
            }
        } else {
            this.taskRef = null;
        }
    }

    /** This initializes a SEB client ping indicator on the persistent storage for a given SEB client
     * connection identifier.
     * If there is already such a ping indicator for the specified SEB client connection identifier, returns
     * the id of the existing one.
     *
     * @param connectionId SEB client connection identifier
     * @return SEB client ping indicator identifier (PK) */
    @Transactional
    public Long initPingForConnection(final Long connectionId) {
        try {

            if (log.isDebugEnabled()) {
                log.trace("*** Initialize ping record for SEB connection: {}", connectionId);
            }

            final Long recordId = this.clientPingMapper
                    .pingRecordIdByConnectionId(connectionId);

            if (recordId == null) {
                final long millisecondsNow = DateTimeUtils.currentTimeMillis();
                final ClientIndicatorRecord clientEventRecord = new ClientIndicatorRecord(
                        null, connectionId, ClientIndicatorType.LAST_PING.id, millisecondsNow, null);

                this.clientIndicatorRecordMapper.insert(clientEventRecord);

                try {
                    // This also double-check by trying again. If we have more then one entry here
                    // this will throw an exception that causes a rollback
                    return this.clientPingMapper
                            .pingRecordIdByConnectionId(connectionId);

                } catch (final Exception e) {

                    log.warn("Detected multiple client ping entries for connection: " + connectionId
                            + ". Force rollback to prevent");

                    // force rollback
                    throw new RuntimeException("Detected multiple client ping entries");
                }
            }

            return recordId;
        } catch (final Exception e) {

            log.error("Failed to initialize ping for connection -> {}", connectionId, e);

            // force rollback
            throw new RuntimeException("Failed to initialize ping for connection -> " + connectionId, e);
        }
    }

    /** Deletes a existing SEB client ping indicator for a given SEB client connection identifier
     * on the persistent storage.
     *
     * @param connectionId SEB client connection identifier */
    @Transactional
    public void deletePingIndicator(final Long connectionId) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("*** Delete ping record for SEB connection: {}", connectionId);
            }

            final Collection<ClientLastPingRecord> records = this.clientPingMapper
                    .selectByExample()
                    .where(ClientIndicatorRecordDynamicSqlSupport.clientConnectionId, isEqualTo(connectionId))
                    .and(ClientIndicatorRecordDynamicSqlSupport.type, isEqualTo(ClientIndicatorType.LAST_PING.id))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return;
            }

            final Long id = records.iterator().next().id;
            this.pingCache.remove(id);
            this.clientIndicatorRecordMapper.deleteByPrimaryKey(id);

        } catch (final Exception e) {
            log.error("Failed to delete ping for connection -> {}", connectionId, e);
            try {
                log.info(
                        "Because of failed ping record deletion, "
                                + "flushing the ping cache to ensure no dead connections pings remain in the cache");
                this.pingCache.clear();
            } catch (final Exception ee) {
                log.error("Failed to force flushing the ping cache: ", e);
            }
        }
    }

    /** Use this to get the last ping time indicator value with a given indicator identifier (PK)
     * This fist tries to get the ping time from internal cache. If not present, tries to get
     * the ping indicator value from persistent storage and put it to the cache.
     *
     * @param pingRecordId The ping indicator record id (PK). Get one for a given SEB client connection identifier by
     *            calling: initPingForConnection
     * @return The actual (last) ping time. */
    public Long getLastPing(final Long pingRecordId) {
        try {

            Long ping = this.pingCache.get(pingRecordId);
            if (ping == null) {

                if (log.isDebugEnabled()) {
                    log.debug("*** Get and cache ping time: {}", pingRecordId);
                }

                ping = this.clientPingMapper.selectPingTimeByPrimaryKey(pingRecordId);
            }

            return ping;
        } catch (final Exception e) {
            log.error("Error while trying to get last ping from storage: {}", e.getMessage());
            return 0L;
        }
    }

    /** Updates the internal ping cache by loading all actual SEB client ping indicators from persistent storage
     * and put it in the cache.
     * This is internally periodically scheduled by the task scheduler but also implements an execution drop if
     * the last update was less then 2/3 of the schedule interval ago. This is to prevent task queue overflows
     * and wait with update when there is a persistent storage leak or a lot of network latency. */
    private void updatePingCache() {
        if (this.pingCache.isEmpty()) {
            return;
        }

        final long millisecondsNow = Utils.getMillisecondsNow();
        if (millisecondsNow - this.lastUpdate < this.pingUpdateTolerance) {
            log.warn("Skip ping update schedule because the last one was less then 2 seconds ago");
            return;
        }

        if (log.isDebugEnabled()) {
            log.trace("*** Update distributed ping cache: {}", this.pingCache);
        }

        try {

            final Map<Long, Long> mapping = this.clientPingMapper
                    .selectByExample()
                    .where(
                            ClientIndicatorRecordDynamicSqlSupport.type,
                            isEqualTo(ClientIndicatorType.LAST_PING.id))
                    .build()
                    .execute()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.id, entry -> entry.lastPingTime));

            if (mapping != null) {
                this.pingCache.clear();
                this.pingCache.putAll(mapping);
                this.lastUpdate = millisecondsNow;
            }

        } catch (final Exception e) {
            log.error("Error while trying to update distributed ping cache: {}", this.pingCache, e);
        }

        this.lastUpdate = millisecondsNow;
    }

    /** Update last ping time on persistent storage asynchronously within a defines thread pool with no
     * waiting queue to skip further ping updates if all update threads are busy **/
    void updatePingAsync(final PingUpdate pingUpdate) {
        try {
            this.pingUpdateExecutor.execute(pingUpdate);
        } catch (final Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to schedule ping task: {}" + e.getMessage());
            }
        }
    }

    /** Create a PingUpdate for a specified SEB client connectionId.
     *
     * @param connectionId SEB client connection identifier
     * @return PingUpdate for a specified SEB client connectionId */
    PingUpdate createPingUpdate(final Long connectionId) {
        return new PingUpdate(
                this.clientPingMapper,
                this.initPingForConnection(connectionId));
    }

    /** Encapsulates a SEB client ping update on persistent storage */
    static final class PingUpdate implements Runnable {

        private final ClientPingMapper clientPingMapper;
        final Long pingRecord;

        public PingUpdate(final ClientPingMapper clientPingMapper, final Long pingRecord) {
            this.clientPingMapper = clientPingMapper;
            this.pingRecord = pingRecord;
        }

        @Override
        /** Processes the ping update on persistent storage by using the current time stamp. */
        public void run() {
            try {
                this.clientPingMapper
                        .updatePingTime(this.pingRecord, Utils.getMillisecondsNow());
            } catch (final Exception e) {
                log.error("Failed to update ping: {}", e.getMessage());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if (this.taskRef != null) {
            try {
                final boolean cancel = this.taskRef.cancel(true);
                if (!cancel) {
                    log.warn("Failed to cancel distributed ping cache update task");
                }
            } catch (final Exception e) {
                log.error("Failed to cancel distributed ping cache update task: ", e);
            }
        }
    }

}
