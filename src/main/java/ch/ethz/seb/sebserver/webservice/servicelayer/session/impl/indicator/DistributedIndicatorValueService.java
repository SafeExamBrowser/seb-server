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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

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
import org.springframework.transaction.interceptor.TransactionInterceptor;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientIndicatorValueMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientIndicatorValueMapper.ClientIndicatorValueRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientIndicatorRecord;

@Lazy
@Component
@WebServiceProfile
/** This service is only needed within a distributed setup where more then one webservice works
 * simultaneously within one SEB Server and one persistent storage.
 * </p>
 * This service handles the SEB client indicator updates within such a setup and implements functionality to
 * efficiently store and load indicator values from and to shared store.
 * </p>
 * The update from the persistent store is done done periodically within a batch while the indicator value writes
 * are done individually per SEB client when they arrive. The update can be done within a dedicated task executor with
 * minimal task
 * queue to do not overflow other executor services when it comes to a leak on storing lot of ping times for example.
 * In this case some ping time updates will be just dropped and not go to the persistent store until the leak
 * is resolved. */
public class DistributedIndicatorValueService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DistributedIndicatorValueService.class);

    private final Executor indicatorValueUpdateExecutor;
    private final ClientIndicatorRecordMapper clientIndicatorRecordMapper;
    private final ClientIndicatorValueMapper clientIndicatorValueMapper;
    private final WebserviceInfo webserviceInfo;
    long distributedUpdateInterval = 2000;
    private long updateTolerance;

    private ScheduledFuture<?> taskRef;
    private final Map<Long, Long> indicatorValueCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0L;

    public DistributedIndicatorValueService(
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME) final Executor pingUpdateExecutor,
            final ClientIndicatorRecordMapper clientIndicatorRecordMapper,
            final ClientIndicatorValueMapper clientIndicatorValueMapper,
            final WebserviceInfo webserviceInfo) {

        this.indicatorValueUpdateExecutor = pingUpdateExecutor;
        this.clientIndicatorRecordMapper = clientIndicatorRecordMapper;
        this.clientIndicatorValueMapper = clientIndicatorValueMapper;
        this.webserviceInfo = webserviceInfo;
    }

    long lastUpdate() {
        return this.lastUpdate;
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
            SEBServerInit.INIT_LOGGER.info("------> Activate distributed indicator value service:");

            final TaskScheduler taskScheduler = applicationContext.getBean(TaskScheduler.class);
            this.distributedUpdateInterval = webserviceInfo.getDistributedUpdateInterval();
            this.updateTolerance = this.distributedUpdateInterval * 2 / 3;

            SEBServerInit.INIT_LOGGER.info("------> with distributedUpdateInterval: {}",
                    this.distributedUpdateInterval);
            SEBServerInit.INIT_LOGGER.info("------> with taskScheduler: {}", taskScheduler);

            try {

                this.taskRef = taskScheduler.scheduleAtFixedRate(
                        this::updateIndicatorValueCache,
                        this.distributedUpdateInterval);

                SEBServerInit.INIT_LOGGER.info("------> distributed indicator value service successfully initialized!");

            } catch (final Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to initialize distributed indicator value service:", e);
                log.error("Failed to initialize distributed indicator value cache update task");
                this.taskRef = null;
            }
        } else {
            this.taskRef = null;
        }
    }

    /** This creates a distributed indicator value cache record for a given SEB connection and indicator
     * if it not already exists and returns the PK for the specified distributed indicator value cache record
     *
     * @param connectionId the client connection identifier
     * @param type the indicator type
     * @param value the initialization value
     * @return the PK of the created or existing distributed indicator value cache record or null when a unexpected
     *         error happened */
    @Transactional
    public Long createIndicatorForConnection(
            final Long connectionId,
            final IndicatorType type,
            final long initValue) {

        if (!this.webserviceInfo.isDistributed()) {
            log.warn("No distributed setup, skip createIndicatorForConnection");
            return null;
        }

        try {

            // first check if the record already exists
            final Long recId = this.clientIndicatorValueMapper.indicatorRecordIdByConnectionId(
                    connectionId,
                    type);
            if (recId != null) {
                log.debug("Distributed indicator value cache already exists for: {}, {}", connectionId, type);
                return recId;
            }

            // if not, create new one and return PK
            final ClientIndicatorRecord clientEventRecord = new ClientIndicatorRecord(
                    null, connectionId, type.id, initValue);
            this.clientIndicatorRecordMapper.insert(clientEventRecord);

            try {
                // This also double-check by trying again. If we have more then one entry here
                // this will throw an exception that causes a rollback
                return this.clientIndicatorValueMapper
                        .indicatorRecordIdByConnectionId(connectionId, type);

            } catch (final Exception e) {

                log.warn(
                        "Detected multiple client indicator entries for connection: {} and type: {}. Force rollback to prevent",
                        connectionId, type);

                // force rollback
                TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
                throw new RuntimeException("Detected multiple client indicator value entries");
            }
        } catch (final Exception e) {
            log.error(
                    "Failed to initialize distributed indicator value cache in persistent store. connectionId: {} type: {}",
                    connectionId, type, e);

            return null;
        }
    }

    /** Get the distributed indicator value cache record PK for a given SEB connection and indicator if available.
     * If not existing for the specified connection and indicator this return null
     *
     * @param connectionId the client connection identifier
     * @param type the indicator type
     * @return the indicator value cache record PK or null of not defined */
    @Transactional(readOnly = true)
    public Long getIndicatorForConnection(final Long connectionId, final IndicatorType type) {
        try {

            return this.clientIndicatorValueMapper
                    .indicatorRecordIdByConnectionId(connectionId, type);

        } catch (final Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Failed to get indicator PK for connection: {} type: {} cause: {}",
                        connectionId,
                        type,
                        e.getMessage());
            }

            return null;
        }
    }

    /** Deletes a existing SEB client indicator value record for a given SEB client connection identifier
     * on the persistent storage.
     *
     * @param connectionId SEB client connection identifier */
    @Transactional
    public void deleteIndicatorValues(final Long connectionId) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("Delete indicator value record for SEB connection: {}", connectionId);
            }

            final Collection<ClientIndicatorValueRecord> records = this.clientIndicatorValueMapper
                    .selectByExample()
                    .where(ClientIndicatorRecordDynamicSqlSupport.clientConnectionId, isEqualTo(connectionId))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return;
            }

            final List<Long> toDelete = records.stream().map(rec -> {
                this.indicatorValueCache.remove(rec.id);
                return rec.id;
            }).collect(Collectors.toList());

            this.clientIndicatorRecordMapper
                    .deleteByExample()
                    .where(ClientIndicatorRecordDynamicSqlSupport.id, isIn(toDelete))
                    .build()
                    .execute();

        } catch (final Exception e) {
            log.error("Failed to delete indicator value for connection -> {}", connectionId, e);
            try {
                log.info(
                        "Because of failed indicator value record deletion, "
                                + "flushing the indicator value cache to ensure no dead connections remain in the cache");
                this.indicatorValueCache.clear();
            } catch (final Exception ee) {
                log.error("Failed to force flushing the indicator value cache: ", e);
            }
        }
    }

    /** Use this to get the last indicator value with a given indicator identifier (PK)
     * This fist tries to get the indicator value from internal cache. If not present, tries to get
     * the indicator value from persistent storage and put it to the cache.
     *
     * @param indicatorPK The indicator record id (PK).
     * @return The actual (last) indicator value. */
    public Long getIndicatorValue(final Long indicatorPK) {
        if (indicatorPK == null) {
            return null;
        }

        Long value = this.indicatorValueCache.get(indicatorPK);
        if (value == null) {
            try {

                value = this.clientIndicatorValueMapper.selectValueByPrimaryKey(indicatorPK);
                if (value != null) {
                    this.indicatorValueCache.put(indicatorPK, value);
                }

            } catch (final Exception e) {
                log.error("Error while trying to get last indicator value from storage: {}", e.getMessage());
                return -1L;
            }
        }
        return value;
    }

    /** Updates the internal indicator value cache by loading all actual SEB client indicators from persistent storage
     * and put it in the cache.
     * This is internally periodically scheduled by the task scheduler but also implements an execution drop if
     * the last update was less then 2/3 of the schedule interval ago. This is to prevent task queue overflows
     * and wait with update when there is a persistent storage leak or a lot of network latency. */
    private void updateIndicatorValueCache() {
        if (this.indicatorValueCache.isEmpty()) {
            return;
        }

        final long millisecondsNow = Utils.getMillisecondsNow();
        if (millisecondsNow - this.lastUpdate < this.updateTolerance) {
            log.warn("Skip indicator value update schedule because the last one was less then 2 seconds ago");
            return;
        }

        if (log.isDebugEnabled()) {
            log.trace("*** Update distributed indicator value cache: {}", this.indicatorValueCache);
        }

        try {

            final Map<Long, Long> mapping = this.clientIndicatorValueMapper
                    .selectByExample()
                    .build()
                    .execute()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.id, entry -> entry.indicatorValue));

            if (mapping != null) {
                this.indicatorValueCache.clear();
                this.indicatorValueCache.putAll(mapping);
                this.lastUpdate = millisecondsNow;
            }

            // System.out.println("*** Update distributed indicator value cache: " + this.indicatorValueCache);

        } catch (final Exception e) {
            log.error("Error while trying to update distributed indicator value cache: {}", this.indicatorValueCache,
                    e);
        }

        this.lastUpdate = millisecondsNow;
    }

    /** Update last ping time on persistent storage asynchronously within a defines thread pool with no
     * waiting queue to skip further ping updates if all update threads are busy
     *
     * TODO: we need a better handling strategy here.
     * Try to apply a batch update and store the pings in a concurrent hash map **/
    void updatePingAsync(final Long pingRecord) {
        try {
            this.indicatorValueUpdateExecutor
                    .execute(() -> this.clientIndicatorValueMapper.updateIndicatorValue(
                            pingRecord,
                            Utils.getMillisecondsNow()));
        } catch (final Exception e) {
            if (log.isTraceEnabled()) {
                log.warn("Failed to schedule ping task: {}" + e.getMessage());
            }
        }
    }

    /** Update indicator value on persistent storage asynchronously within a defined thread pool with no
     * waiting queue to skip further indicator value updates if all update threads are busy **/
    boolean updateIndicatorValueAsync(final Long pk, final Long value) {
        try {
            this.indicatorValueUpdateExecutor
                    .execute(() -> this.clientIndicatorValueMapper.updateIndicatorValue(pk, value));
            return true;
        } catch (final Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to schedule indicator update task: {}" + e.getMessage());
            }
            return false;
        }
    }

    /** Update an indicator value within a transaction */
    @Transactional
    void updateIndicatorValue(final Long pk, final Long value) {
        try {
            this.clientIndicatorValueMapper.updateIndicatorValue(pk, value);
        } catch (final Exception e) {
            log.warn("Failed to update indicator value: {}" + e.getMessage());
        }
    }

    /** Simply increment a given indicator value */
    void incrementIndicatorValue(final Long pk) {
        try {
            this.clientIndicatorValueMapper.incrementIndicatorValue(pk);
        } catch (final Exception e) {
            log.warn("Failed to increment indicator value: {}" + e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        if (this.taskRef != null) {

            SEBServerInit.INIT_LOGGER.info("----> Shout down distributed indicator service...");

            try {
                final boolean cancel = this.taskRef.cancel(true);
                if (!cancel) {
                    log.warn("Failed to cancel distributed indicator cache update task");
                }

                SEBServerInit.INIT_LOGGER.info("----> Distributed indicator service down");

            } catch (final Exception e) {
                log.error("Failed to cancel distributed indicator cache update task: ", e);
            }
        }
    }

}
