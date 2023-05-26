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

@Lazy
@Component(IndicatorType.Names.LAST_PING)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class PingIntervalClientIndicator extends AbstractPingIndicator {

    // This is the default ping error threshold that is set if the threshold cannot be get
    // from the ping threshold settings. If the last ping is older then this interval back in time
    // then the ping is considered and marked as missing
    private static final long DEFAULT_PING_ERROR_THRESHOLD = Constants.SECOND_IN_MILLIS * 5;

    private boolean hidden = false;

    public PingIntervalClientIndicator(final DistributedIndicatorValueService distributedIndicatorValueService) {
        super(distributedIndicatorValueService);
        this.cachingEnabled = true;
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

        // take samples, current value before current time to prevent negative ping times
        final double value = this.currentValue;
        final long currentTimeMillis = DateTimeUtils.currentTimeMillis();

        if (this.initialized && !this.cachingEnabled && this.active
                && this.lastUpdate != this.distributedIndicatorValueService.lastUpdate()) {

            this.currentValue = computeValueAt(currentTimeMillis);
            this.lastUpdate = this.distributedIndicatorValueService.lastUpdate();
        }

        final double res = currentTimeMillis - value;
        return res >= 0.0D ? res : 0.0D;
    }

    @Override
    public void notifyValueChange(final String textValue, final double numValue) {

    }

    @Override
    public final double computeValueAt(final long timestamp) {
        if (super.ditributedIndicatorValueRecordId != null) {

            final Long lastPing = this.distributedIndicatorValueService
                    .getIndicatorValue(super.ditributedIndicatorValueRecordId);

            return (lastPing != null)
                    ? lastPing.doubleValue()
                    : this.currentValue;
        }

        return !this.initialized ? timestamp : this.currentValue;
    }

    @Override
    public final boolean hasIncident() {
        if (!this.active) {
            return false;
        }

        return getValue() >= super.incidentThreshold;
    }

    private double lastCheckVal = 0;

    public final boolean changeOnIncident() {
        if (!this.active || this.currentValue <= 0) {
            return false;
        }

        final double val = getValue();
        // check if incident threshold has passed (up or down) since last update
        final boolean changed = (this.lastCheckVal < this.incidentThreshold && val >= this.incidentThreshold) ||
                (this.lastCheckVal >= this.incidentThreshold && val < this.incidentThreshold);

        this.lastCheckVal = val;
        return changed;
    }

}
