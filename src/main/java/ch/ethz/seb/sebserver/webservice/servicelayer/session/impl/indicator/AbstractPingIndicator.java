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

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected AbstractPingIndicator(final DistributedIndicatorValueService distributedPingCache) {
        super(distributedPingCache);
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    public final void notifyPing(final long timestamp, final int pingNumber) {
        super.currentValue = timestamp;

        if (!this.cachingEnabled) {

            if (super.ditributedIndicatorValueRecordId == null) {
                tryRecoverIndicatorRecord();
                if (this.ditributedIndicatorValueRecordId == null) {
                    return;
                }
            }

            this.distributedPingCache.updatePingAsync(this.ditributedIndicatorValueRecordId);
        }
    }

}
