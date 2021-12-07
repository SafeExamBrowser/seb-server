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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.DistributedPingService.PingUpdate;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractPingIndicator.class);

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected final DistributedPingService distributedPingCache;
    protected PingUpdate pingUpdate = null;

    protected AbstractPingIndicator(final DistributedPingService distributedPingCache) {
        super();
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
                this.pingUpdate = this.distributedPingCache.createPingUpdate(connectionId);
            } catch (final Exception e) {
                this.pingUpdate = this.distributedPingCache.createPingUpdate(connectionId);
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

            this.distributedPingCache.updatePingAsync(this.pingUpdate);
        }
    }

    private void tryRecoverPingRecord() {

        if (log.isWarnEnabled()) {
            log.warn("*** Missing ping record for connection: {}. Try to recover...", this.connectionId);
        }

        try {
            this.pingUpdate = this.distributedPingCache.createPingUpdate(this.connectionId);
            if (this.pingUpdate == null) {
                this.pingUpdate = this.distributedPingCache.createPingUpdate(this.connectionId);
            }
        } catch (final Exception e) {
            log.error("Failed to recover ping record for connection: {}", this.connectionId, e);
        }
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    public abstract ClientEventRecord updateLogEvent(final long now);

}
