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
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtensionMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected final ClientEventExtensionMapper clientEventExtensionMapper;
    protected final IndicatorDistributedRequestCache indicatorDistributedRequestCache;

    protected long pingLatency;
    protected int pingCount = 0;
    protected int pingNumber = 0;

    protected AbstractPingIndicator(
            final ClientEventExtensionMapper clientEventExtensionMapper,
            final IndicatorDistributedRequestCache indicatorDistributedRequestCache) {

        super();
        this.clientEventExtensionMapper = clientEventExtensionMapper;
        this.indicatorDistributedRequestCache = indicatorDistributedRequestCache;
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        this.pingLatency = now - timestamp;
        super.currentValue = now;
        this.pingCount++;
        this.pingNumber = pingNumber;
    }

    @Override
    public final double computeValueAt(final long timestamp) {
        if (this.cachingEnabled) {
            return timestamp;
        } else {
            try {
                return this.indicatorDistributedRequestCache
                        .getPingTimes(this.examId)
                        .getOrDefault(this.connectionId, 0L);

            } catch (final Exception e) {
                return Double.NaN;
            }
        }
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    @JsonIgnore
    public int getPingCount() {
        return this.pingCount;
    }

    @JsonIgnore
    public int getPingNumber() {
        return this.pingNumber;
    }

    public abstract ClientEventRecord updateLogEvent(final long now);

}
