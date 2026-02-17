/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientIndicatorValueMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientIndicatorValueMapper.ClientIndicatorValueRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientIndicatorRecord;
import org.springframework.transaction.support.TransactionTemplate;

@Lazy
@Component
@WebServiceProfile
/* This service is only needed within a distributed setup where more than one webservice works
 * simultaneously within one SEB Server and one persistent storage.
 * </p>
 * This service handles the SEB client indicator updates within such a setup and implements functionality to
 * efficiently store and load indicator values from and to shared store.
 * </p>
 * There is a read batch that gets all indicator values from storage and put it to the cache.
 * And there is a write batch that collects all indicators values coming in from SEB clients
 * and makes a batch update on the storage table for every 500 milliseconds */
public class DistributedIndicatorValueService {

    private static final Logger log = LoggerFactory.getLogger(DistributedIndicatorValueService.class);

    private final TaskScheduler taskScheduler;
    private final ClientIndicatorRecordMapper clientIndicatorRecordMapper;
    private final ClientIndicatorValueMapper clientIndicatorValueMapper;
    private final WebserviceInfo webserviceInfo;

    // read batching
    private long updateTolerance;
    private final Map<Long, Long> indicatorValueCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0L;

    // write batching
    private final TransactionTemplate transactionTemplate;
    private final ClientIndicatorValueMapper clientIndicatorValueMapperBatch;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final Map<Long, Long> indicatorValueQueue = new ConcurrentHashMap<>();


    public DistributedIndicatorValueService(
            final TaskScheduler taskScheduler,
            final PlatformTransactionManager transactionManager,
            final SqlSessionFactory sqlSessionFactory,
            final ClientIndicatorRecordMapper clientIndicatorRecordMapper,
            final ClientIndicatorValueMapper clientIndicatorValueMapper,
            final WebserviceInfo webserviceInfo) {

        this.taskScheduler = taskScheduler;
        this.clientIndicatorRecordMapper = clientIndicatorRecordMapper;
        this.clientIndicatorValueMapper = clientIndicatorValueMapper;
        this.webserviceInfo = webserviceInfo;

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // create clientIndicatorValueMapperBatch
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
        final MapperRegistry mapperRegistry = this.sqlSessionTemplate.getConfiguration().getMapperRegistry();
        final Collection<Class<?>> mappers = mapperRegistry.getMappers();
        if (!mappers.contains(ClientIndicatorValueMapper.class)) {
            mapperRegistry.addMapper(ClientIndicatorValueMapper.class);
        }
        this.clientIndicatorValueMapperBatch = this.sqlSessionTemplate.getMapper(ClientIndicatorValueMapper.class);
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
        if (webserviceInfo.isDistributed()) {

            SEBServerInit.INIT_LOGGER.info("------>");
            SEBServerInit.INIT_LOGGER.info("------> Activate distributed indicator value service:");

            // read batching
            long readBatchInterval = webserviceInfo.getDistributedUpdateInterval();
            long writeBatchInterval = webserviceInfo.getDistributedWriteBatchInterval();
            this.updateTolerance = readBatchInterval * 2 / 3;

            SEBServerInit.INIT_LOGGER.info("------> with distributed read interval: {}",
                    readBatchInterval);
            SEBServerInit.INIT_LOGGER.info("------> with distributed write interval : {}",
                    writeBatchInterval);
            SEBServerInit.INIT_LOGGER.info("------> with taskScheduler: {}", taskScheduler);

            try {

                taskScheduler.scheduleAtFixedRate(
                        this::updateIndicatorValueCache,
                        readBatchInterval);

                SEBServerInit.INIT_LOGGER.info("------> distributed indicator value service successfully initialized!");

                Map<Long, Long> batchMap1 = new HashMap<>();
                taskScheduler.scheduleWithFixedDelay(
                        () -> processStoreUpdate(batchMap1),
                        Duration.ofMillis(writeBatchInterval));

            } catch (final Exception e) {
                SEBServerInit.INIT_LOGGER.error("------> Failed to initialize distributed indicator value service:", e);
                log.error("Failed to initialize distributed indicator value cache update task");
            }
        }
    }

    /** This creates a distributed indicator value cache record for a given SEB connection and indicator
     * if it not already exists and returns the PK for the specified distributed indicator value cache record
     *
     * @param connectionId the client connection identifier
     * @param type the indicator type
     * @param initValue the initialization value
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

            Long recId = null;
            // first check if the record already exists
            try {
                recId = this.clientIndicatorValueMapper.indicatorRecordIdByConnectionId(
                        connectionId,
                        type);
            } catch (final TooManyResultsException e) {
                // There are already to many yet, select with limit to get first one and use this
                recId = this.clientIndicatorValueMapper.indicatorRecordIdByConnectionIdLimit(
                        connectionId,
                        type);
            }

            if (recId != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Distributed indicator value cache already exists for: {}, {}", connectionId, type);
                }
                return recId;
            }

            if (log.isDebugEnabled()) {
                log.debug("Create distributed indicator value cache for: {}, {}", connectionId, type);
            }

            // if not, create new one and return PK
            final ClientIndicatorRecord clientEventRecord = new ClientIndicatorRecord(
                    null, connectionId, type.id, initValue);
            this.clientIndicatorRecordMapper.insert(clientEventRecord);

            try {
                // This also double-check by trying again. If we have more than one entry here
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
                    "Failed to initialize distributed indicator value cache in persistent store. connectionId: {} type: {} cause: {}",
                    connectionId, type, e.getMessage());

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

        try {

            final Map<Long, Long> mapping = this.clientIndicatorValueMapper
                    .selectByExample()
                    .build()
                    .execute()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.id, entry -> entry.indicatorValue));


            this.indicatorValueCache.clear();
            this.indicatorValueCache.putAll(mapping);

            //System.out.println("************** loaded indicatorValueCache: " + indicatorValueCache);

        } catch (final Exception e) {
            log.error("Error while trying to update distributed indicator value cache: {}", this.indicatorValueCache,
                    e);
        }

        this.lastUpdate = millisecondsNow;
    }

    void updatePingAsync(final Long pingRecord) {
        if (pingRecord == null) {
            return;
        }
        this.indicatorValueQueue.put(pingRecord, Utils.getMillisecondsNow());
    }

    boolean updateIndicatorValueAsync(final Long pk, final Long value) {
        if (pk == null) {
            return false;
        }
        this.indicatorValueQueue.put(pk, value);
        return true;
    }

    private void processStoreUpdate(final Map<Long, Long> batchMap) {
        try {

            if (this.indicatorValueQueue.isEmpty()) {
                return;
            }

            //long start = System.currentTimeMillis();

            // drain/copy the indicatorValueQueue into the local batchMap
            batchMap.clear();
            synchronized (this.indicatorValueQueue) {
                batchMap.putAll(this.indicatorValueQueue);
                this.indicatorValueQueue.clear();
            }


            this.transactionTemplate.executeWithoutResult(status -> {
                batchMap.forEach(clientIndicatorValueMapperBatch::updateIndicatorValue);
                this.sqlSessionTemplate.flushStatements();
            });

            //System.out.println("************* batchMap size: " + batchMap.size() + " took: " + (System.currentTimeMillis() - start));

        } catch (Exception e) {
            log.error("Failed write distributed indicator values to persistent store. Skip all. cause: {}", e.getMessage());
        }
    }

}
