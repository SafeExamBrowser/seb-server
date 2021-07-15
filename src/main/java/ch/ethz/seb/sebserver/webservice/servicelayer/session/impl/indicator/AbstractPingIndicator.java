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

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private static final int PING_COUNT_INTERVAL_FOR_PERSISTENT_UPDATE = 2;

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected final ClientEventDAO clientEventDAO;

    protected long pingLatency;
    protected int pingCount = 0;
    protected int pingNumber = 0;

    protected ClientEventRecord pingRecord = null;

    protected AbstractPingIndicator(final ClientEventDAO clientEventDAO) {
        super();
        this.clientEventDAO = clientEventDAO;
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);

        if (!this.cachingEnabled) {
            this.pingRecord = this.clientEventDAO
                    .initPingEvent(this.connectionId)
                    .getOr(null);
        }
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        this.pingLatency = now - timestamp;
        super.currentValue = now;
        this.pingCount++;
        this.pingNumber = pingNumber;
        super.lastPersistentUpdate = now;

        if (!this.cachingEnabled &&
                this.pingCount > PING_COUNT_INTERVAL_FOR_PERSISTENT_UPDATE &&
                this.pingRecord != null) {

            // Update last ping time on persistent storage
            this.pingRecord.setClientTime(timestamp);
            this.pingRecord.setServerTime(Utils.getMillisecondsNow());
            this.clientEventDAO.updatePingEvent(this.pingRecord);
            this.pingCount = 0;
        }
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    @JsonIgnore
    public int getPingNumber() {
        return this.pingNumber;
    }

    public abstract ClientEventRecord updateLogEvent(final long now);

}
