/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.concurrent.Executor;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
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

    public PingIntervalClientIndicator(
            final DistributedPingCache distributedPingCache,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME) final Executor executor) {
        super(distributedPingCache, executor);
        this.cachingEnabled = true;
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);

        final long now = DateTimeUtils.currentTimeMillis();
        this.currentValue = computeValueAt(now);
        if (Double.isNaN(this.currentValue)) {
            this.currentValue = now;
        }

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

        if (!cachingEnabled) {
            try {
                final double value = getValue();
                this.missingPing = this.pingErrorThreshold < value;
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
        final double value = super.getValue();
        return DateTimeUtils.currentTimeMillis() - value;
    }

    @Override
    public void notifyValueChange(final ClientEvent event) {

    }

    @Override
    public void notifyValueChange(final ClientEventRecord clientEventRecord) {

    }

    @Override
    public final double computeValueAt(final long timestamp) {
        if (!this.cachingEnabled && super.pingUpdate != null) {

            final Long lastPing = this.distributedPingCache.getLastPing(super.pingUpdate.pingRecord, this.missingPing);
            if (lastPing != null) {
                final double doubleValue = lastPing.doubleValue();
                return Math.max(Double.isNaN(this.currentValue) ? doubleValue : this.currentValue, doubleValue);
            }

            return this.currentValue;
        }

        return !this.valueInitializes ? timestamp : this.currentValue;
    }

    @Override
    public ClientEventRecord updateLogEvent(final long now) {
        final long value = now - (long) super.currentValue;
        if (this.missingPing) {
            if (this.pingErrorThreshold > value) {
                this.missingPing = false;
                return new ClientEventRecord(
                        null,
                        this.connectionId,
                        EventType.INFO_LOG.id,
                        now,
                        now,
                        new BigDecimal(value),
                        "Client Ping Back To Normal");
            }
        } else {
            if (this.pingErrorThreshold < value) {
                this.missingPing = true;
                return new ClientEventRecord(
                        null,
                        this.connectionId,
                        EventType.ERROR_LOG.id,
                        now,
                        now,
                        new BigDecimal(value),
                        "Missing Client Ping");
            }
        }

        return null;
    }

}
