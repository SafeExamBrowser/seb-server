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

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractPingIndicator.class);

    private static final long INTERVAL_FOR_PERSISTENT_UPDATE = Constants.SECOND_IN_MILLIS;

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected final DistributedPingCache distributedPingCache;

    private final long lastUpdate = 0;
    protected Long pingRecord = null;

    protected AbstractPingIndicator(final DistributedPingCache distributedPingCache) {

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
                this.pingRecord = this.distributedPingCache.initPingForConnection(this.connectionId);
            } catch (final Exception e) {
                this.pingRecord = this.distributedPingCache.getPingRecordIdForConnectionId(connectionId);
            }
        }
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        super.currentValue = now;
        super.lastPersistentUpdate = now;

        if (!this.cachingEnabled) {

            if (this.pingRecord == null) {
                tryRecoverPingRecord();
                if (this.pingRecord == null) {
                    return;
                }
            }

            // Update last ping time on persistent storage
            final long millisecondsNow = DateTimeUtils.currentTimeMillis();
            if (millisecondsNow - this.lastUpdate > INTERVAL_FOR_PERSISTENT_UPDATE) {
                this.distributedPingCache.updatePing(this.pingRecord, millisecondsNow);
            }
        }
    }

    private void tryRecoverPingRecord() {

        if (log.isWarnEnabled()) {
            log.warn("*** Missing ping record for connection: {}. Try to recover...", this.connectionId);
        }

        try {
            this.pingRecord = this.distributedPingCache.getPingRecordIdForConnectionId(this.connectionId);
            if (this.pingRecord == null) {
                this.pingRecord = this.distributedPingCache.initPingForConnection(this.connectionId);
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
