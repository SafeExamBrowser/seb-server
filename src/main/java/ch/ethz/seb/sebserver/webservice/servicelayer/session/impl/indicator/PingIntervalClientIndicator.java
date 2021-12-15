/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import java.util.Comparator;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PingIntervalClientIndicator.class);

    // This is the default ping error threshold that is set if the threshold cannot be get
    // from the ping threshold settings. If the last ping is older then this interval back in time
    // then the ping is considered and marked as missing
    private static final long DEFAULT_PING_ERROR_THRESHOLD = Constants.SECOND_IN_MILLIS * 5;

    private long pingErrorThreshold;
    private boolean missingPing = false;
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

        // init ping error threshold
        try {

            indicatorDefinition
                    .getThresholds()
                    .stream()
                    .max(Comparator.naturalOrder())
                    .ifPresent(t -> this.pingErrorThreshold = t.value.longValue());

        } catch (final Exception e) {
            log.error("Failed to initialize pingErrorThreshold: {}", e.getMessage());
            this.pingErrorThreshold = DEFAULT_PING_ERROR_THRESHOLD;
        }

        // init missing ping indicator
        if (!cachingEnabled) {
            try {
                this.missingPing = this.pingErrorThreshold < getValue();
            } catch (final Exception e) {
                log.error("Failed to initialize missingPing: {}", e.getMessage());
                this.missingPing = true;
            }
        }

    }

    @JsonIgnore
    public final boolean isMissingPing() {
        return this.missingPing;
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
        return DateTimeUtils.currentTimeMillis() - this.currentValue;
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
    public boolean missingPingUpdate(final long now) {
        if (this.currentValue <= 0) {
            return false;
        }

        final long value = now - (long) super.currentValue;
        if (this.missingPing) {
            if (this.pingErrorThreshold > value) {
                this.missingPing = false;
                return true;
            }
        } else {
            if (this.pingErrorThreshold < value) {
                this.missingPing = true;
                return true;
            }
        }

        return false;
    }

}
