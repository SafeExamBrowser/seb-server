/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.math.BigDecimal;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValueHolder;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

@Lazy
@Component(IndicatorType.Names.LAST_PING)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class PingIntervalClientIndicator extends AbstractPingIndicator {

    private static final Logger log = LoggerFactory.getLogger(PingIntervalClientIndicator.class);

    private long pingErrorThreshold;
    private boolean isOnError = false;

    public PingIntervalClientIndicator(final ClientEventExtentionMapper clientEventExtentionMapper) {
        super(clientEventExtentionMapper);
        this.cachingEnabled = true;
        this.currentValue = computeValueAt(Utils.getMillisecondsNow());
    }

    @Override
    public void init(final Indicator indicatorDefinition, final Long connectionId, final boolean cachingEnabled) {
        super.init(indicatorDefinition, connectionId, cachingEnabled);

        try {
            indicatorDefinition
                    .getThresholds()
                    .stream()
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .ifPresent(t -> this.pingErrorThreshold = t.value.longValue());
        } catch (final Exception e) {
            log.error("Failed to initialize pingErrorThreshold: {}", e.getMessage());
            this.pingErrorThreshold = Constants.SECOND_IN_MILLIS * 5;
        }
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.LAST_PING;
    }

    @Override
    public double getValue() {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        return now - super.currentValue;
    }

    @Override
    public void notifyValueChange(final IndicatorValueHolder indicatorValueHolder) {

    }

    @Override
    public ClientEventRecord updateLogEvent() {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        final long value = now - (long) super.currentValue;
        if (this.isOnError) {
            if (this.pingErrorThreshold > value) {
                this.isOnError = false;
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
                this.isOnError = true;
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
