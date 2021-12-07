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
                        this::persistentPingUpdate,
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

    public ClientPingMapper getClientPingMapper() {
        return this.clientPingMapper;
    }

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

    @Transactional(readOnly = true)
    public Long getPingRecordIdForConnectionId(final Long connectionId) {
        try {

            return this.clientPingMapper
                    .pingRecordIdByConnectionId(connectionId);

        } catch (final Exception e) {
            log.error("Failed to get ping record for connection id: {} cause: {}", connectionId, e.getMessage());
            return null;
        }
    }

    @Transactional
    public void deletePingForConnection(final Long connectionId) {
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

    public Long getLastPing(final Long pingRecordId, final boolean missing) {
        try {

            Long ping = this.pingCache.get(pingRecordId);
            if (ping == null && !missing) {

                if (log.isDebugEnabled()) {
                    log.debug("*** Get and cache ping time: {}", pingRecordId);
                }

                ping = this.clientPingMapper.selectPingTimeByPrimaryKey(pingRecordId);
            }

            // if we have a missing ping we need to check new ping from next update even if the cache was empty
            if (ping != null || missing) {
                this.pingCache.put(pingRecordId, ping);
            }

            return ping;
        } catch (final Exception e) {
            log.error("Error while trying to get last ping from storage: {}", e.getMessage());
            return 0L;
        }
    }

    public void persistentPingUpdate() {
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

    // Update last ping time on persistent storage asynchronously within a defines thread pool with no
    // waiting queue to skip further ping updates if all update threads are busy
    void updatePingAsync(final PingUpdate pingUpdate) {
        try {
            this.pingUpdateExecutor.execute(pingUpdate);
        } catch (final Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to schedule ping task: {}" + e.getMessage());
            }
        }
    }

    PingUpdate createPingUpdate(final Long connectionId) {
        return new PingUpdate(
                this.getClientPingMapper(),
                this.initPingForConnection(connectionId));
    }

    static final class PingUpdate implements Runnable {

        private final ClientPingMapper clientPingMapper;
        final Long pingRecord;

        public PingUpdate(final ClientPingMapper clientPingMapper, final Long pingRecord) {
            this.clientPingMapper = clientPingMapper;
            this.pingRecord = pingRecord;
        }

        @Override
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
