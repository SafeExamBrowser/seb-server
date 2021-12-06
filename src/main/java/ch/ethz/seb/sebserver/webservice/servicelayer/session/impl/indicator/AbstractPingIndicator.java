/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventLastPingMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractPingIndicator.class);

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    private final Executor executor;
    protected final DistributedPingCache distributedPingCache;

    protected PingUpdate pingUpdate = null;

    protected AbstractPingIndicator(
            final DistributedPingCache distributedPingCache,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME) final Executor executor) {

        super();
        this.executor = executor;
        this.distributedPingCache = distributedPingCache;
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);

        if (!this.cachingEnabled && this.active) {
            try {
                createPingUpdate();
            } catch (final Exception e) {
                createPingUpdate();
            }
        }
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        super.currentValue = timestamp;

        if (!this.cachingEnabled) {

            if (this.pingUpdate == null) {
                tryRecoverPingRecord();
                if (this.pingUpdate == null) {
                    return;
                }
            }

            // Update last ping time on persistent storage asynchronously within a defines thread pool with no
            // waiting queue to skip further ping updates if all update threads are busy
            try {
                this.executor.execute(this.pingUpdate);
            } catch (final Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to schedule ping task: {}" + e.getMessage());
                }
            }
        }
    }

    private void tryRecoverPingRecord() {

        if (log.isWarnEnabled()) {
            log.warn("*** Missing ping record for connection: {}. Try to recover...", this.connectionId);
        }

        try {
            createPingUpdate();
            if (this.pingUpdate == null) {
                createPingUpdate();
            }
        } catch (final Exception e) {
            log.error("Failed to recover ping record for connection: {}", this.connectionId, e);
        }
    }

    private void createPingUpdate() {
        this.pingUpdate = new PingUpdate(
                this.distributedPingCache.getClientEventLastPingMapper(),
                this.distributedPingCache.initPingForConnection(this.connectionId));
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    public abstract ClientEventRecord updateLogEvent(final long now);

    static final class PingUpdate implements Runnable {

        private final ClientEventLastPingMapper clientEventLastPingMapper;
        final Long pingRecord;

        public PingUpdate(final ClientEventLastPingMapper clientEventLastPingMapper, final Long pingRecord) {
            this.clientEventLastPingMapper = clientEventLastPingMapper;
            this.pingRecord = pingRecord;
        }

        @Override
        public void run() {
            try {
                this.clientEventLastPingMapper
                        .updatePingTime(this.pingRecord, Utils.getMillisecondsNow());
            } catch (final Exception e) {
                log.error("Failed to update ping: {}", e.getMessage());
            }
        }

    }

}
