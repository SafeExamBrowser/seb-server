/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

@Lazy
@Component(IndicatorType.Names.LAST_PING)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class PingIntervalClientIndicator extends AbstractPingIndicator {

    // This is the default ping error threshold that is set if the threshold cannot be get
    // from the ping threshold settings. If the last ping is older then this interval back in time
    // then the ping is considered and marked as missing
    private static final long DEFAULT_PING_ERROR_THRESHOLD = Constants.SECOND_IN_MILLIS * 5;

    private boolean hidden = false;

    public PingIntervalClientIndicator(final DistributedIndicatorValueService distributedPingCache) {
        super(distributedPingCache);
        this.cachingEnabled = true;
    }

    @Override
    protected long initValue() {
        return Utils.getMillisecondsNow();
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);

        this.lastCheckVal = getValue();

        if (this.incidentThreshold <= 0.0) {
            this.incidentThreshold = DEFAULT_PING_ERROR_THRESHOLD;
        }
    }

    @JsonIgnore
    public final boolean isHidden() {
        return this.hidden;
    }

    @JsonIgnore
    public final void setHidden() {
        this.hidden = true;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.LAST_PING;
    }

    @Override
    public double getValue() {
        if (!this.initialized) {
            return Double.NaN;
        }
        final long currentTimeMillis = DateTimeUtils.currentTimeMillis();
        if (this.initialized && !this.cachingEnabled && this.active
                && this.lastUpdate != this.distributedPingCache.lastUpdate()) {
            this.currentValue = computeValueAt(currentTimeMillis);
        }
        return currentTimeMillis - this.currentValue;
    }

    @Override
    public void notifyValueChange(final ClientEvent event) {

    }

    @Override
    public void notifyValueChange(final ClientEventRecord clientEventRecord) {

    }

    @Override
    public final double computeValueAt(final long timestamp) {
        if (!this.cachingEnabled && super.ditributedIndicatorValueRecordId != null) {

            final Long lastPing = this.distributedPingCache
                    .getIndicatorValue(super.ditributedIndicatorValueRecordId);

            if (lastPing != null) {
                final double doubleValue = lastPing.doubleValue();
                return Math.max(Double.isNaN(this.currentValue) ? doubleValue : this.currentValue, doubleValue);
            }

            return this.currentValue;
        }

        return !this.initialized ? timestamp : this.currentValue;
    }

    @Override
    public final boolean hasIncident() {
        return getValue() >= super.incidentThreshold;
    }

    private double lastCheckVal = 0;

    public final boolean missingPingUpdate(final long now) {
        if (this.currentValue <= 0) {
            return false;
        }

        final double val = now - this.currentValue;
        // check if incidentThreshold was passed (up or down) since last update
        final boolean result = (this.lastCheckVal < this.incidentThreshold && val >= this.incidentThreshold) ||
                (this.lastCheckVal >= this.incidentThreshold && val < this.incidentThreshold);
        this.lastCheckVal = val;
        return result;
    }

}
