/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventLastPingMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventLastPingMapper.ClientEventLastPingRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

@Lazy
@Component
@WebServiceProfile
public class DistributedPingCache implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DistributedPingCache.class);

    private final ClientEventLastPingMapper clientEventLastPingMapper;
    private final ClientEventRecordMapper clientEventRecordMapper;
    private final long pingUpdateTolerance;

    private ScheduledFuture<?> taskRef;
    private final Map<Long, Long> pingCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0L;

    public DistributedPingCache(
            final ClientEventLastPingMapper clientEventLastPingMapper,
            final ClientEventRecordMapper clientEventRecordMapper,
            final WebserviceInfo webserviceInfo,
            final TaskScheduler taskScheduler,
            @Value("${sebserver.webservice.distributed.pingUpdate:3000}") final long pingUpdate) {

        this.clientEventLastPingMapper = clientEventLastPingMapper;
        this.clientEventRecordMapper = clientEventRecordMapper;
        this.pingUpdateTolerance = pingUpdate * 2 / 3;
        if (webserviceInfo.isDistributed()) {
            try {
                this.taskRef = taskScheduler.scheduleAtFixedRate(this::updatePings, pingUpdate);
            } catch (final Exception e) {
                log.error("Failed to initialize distributed ping cache update task");
                this.taskRef = null;
            }
        } else {
            this.taskRef = null;
        }
    }

    public ClientEventLastPingMapper getClientEventLastPingMapper() {
        return this.clientEventLastPingMapper;
    }

    @Transactional
    public Long initPingForConnection(final Long connectionId) {
        try {

            if (log.isDebugEnabled()) {
                log.trace("*** Initialize ping record for SEB connection: {}", connectionId);
            }

            final Long recordId = this.clientEventLastPingMapper
                    .pingRecordIdByConnectionId(connectionId);

            if (recordId == null) {
                final long millisecondsNow = DateTimeUtils.currentTimeMillis();
                final ClientEventRecord clientEventRecord = new ClientEventRecord();
                clientEventRecord.setClientConnectionId(connectionId);
                clientEventRecord.setType(EventType.LAST_PING.id);
                clientEventRecord.setClientTime(millisecondsNow);
                clientEventRecord.setServerTime(millisecondsNow);
                this.clientEventRecordMapper.insert(clientEventRecord);

                try {
                    // This also double-check by trying again. If we have more then one entry here
                    // this will throw an exception that causes a rollback
                    return this.clientEventLastPingMapper
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

            return this.clientEventLastPingMapper
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

            final Collection<ClientEventLastPingRecord> records = this.clientEventLastPingMapper
                    .selectByExample()
                    .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(connectionId))
                    .and(ClientEventRecordDynamicSqlSupport.type, isEqualTo(EventType.LAST_PING.id))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return;
            }

            final Long id = records.iterator().next().id;
            this.pingCache.remove(id);
            this.clientEventRecordMapper.deleteByPrimaryKey(id);

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

                ping = this.clientEventLastPingMapper.selectPingTimeByPrimaryKey(pingRecordId);
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

    private void updatePings() {

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

            final ArrayList<Long> pks = new ArrayList<>(this.pingCache.keySet());
            final Map<Long, Long> mapping = this.clientEventLastPingMapper
                    .selectByExample()
                    .where(
                            ClientEventRecordDynamicSqlSupport.id,
                            isIn(pks))
                    .build()
                    .execute()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.id, entry -> entry.lastPingTime));

            if (mapping != null) {
                this.pingCache.clear();
                this.pingCache.putAll(mapping);
            }

        } catch (final Exception e) {
            log.error("Error while trying to update distributed ping cache: {}", this.pingCache, e);
        }

        this.lastUpdate = millisecondsNow;
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
