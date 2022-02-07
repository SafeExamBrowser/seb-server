/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public abstract class AbstractClientIndicator implements ClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractClientIndicator.class);

    protected final DistributedIndicatorValueService distributedPingCache;

    protected Long indicatorId = -1L;
    protected Long examId = -1L;
    protected Long connectionId;
    protected boolean cachingEnabled;
    protected boolean active = true;

    protected Long ditributedIndicatorValueRecordId = null;

    protected boolean initialized = false;
    protected double currentValue = Double.NaN;

    protected double incidentThreshold = 0.0;

    protected long lastUpdate = 0;

    public AbstractClientIndicator(final DistributedIndicatorValueService distributedPingCache) {
        super();
        this.distributedPingCache = distributedPingCache;
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        if (indicatorDefinition != null) {
            this.incidentThreshold = (!indicatorDefinition.type.inverse)
                    ? indicatorDefinition.thresholds.stream()
                            .map(t -> t.value)
                            .max(Comparator.naturalOrder())
                            .orElse(0.0)
                    : indicatorDefinition.thresholds.stream()
                            .map(t -> t.value)
                            .min(Comparator.naturalOrder())
                            .orElse(0.0);
            this.indicatorId = indicatorDefinition.id;
            this.examId = indicatorDefinition.examId;
        }

        this.connectionId = connectionId;
        this.active = active;
        this.cachingEnabled = cachingEnabled;

        if (!this.cachingEnabled && this.active) {
            try {
                this.ditributedIndicatorValueRecordId = this.distributedPingCache.initIndicatorForConnection(
                        connectionId,
                        getType(),
                        initValue());
            } catch (final Exception e) {
                tryRecoverIndicatorRecord();
            }
        }

        this.currentValue = computeValueAt(Utils.getMillisecondsNow());
        this.initialized = true;
    }

    protected long initValue() {
        return 0;
    }

    protected void tryRecoverIndicatorRecord() {

        if (log.isWarnEnabled()) {
            log.warn("*** Missing indicator value record for connection: {}. Try to recover...", this.connectionId);
        }

        try {
            this.ditributedIndicatorValueRecordId = this.distributedPingCache.initIndicatorForConnection(
                    this.connectionId,
                    getType(),
                    initValue());
        } catch (final Exception e) {
            log.error("Failed to recover indicator value record for connection: {}", this.connectionId, e);
        }
    }

    @Override
    public Long getIndicatorId() {
        return this.indicatorId;
    }

    @Override
    public Long examId() {
        return this.examId;
    }

    @Override
    public Long connectionId() {
        return this.connectionId;
    }

    public void reset() {
        this.currentValue = computeValueAt(Utils.getMillisecondsNow());
    }

    @Override
    public double getValue() {

        if (this.initialized && !this.cachingEnabled && this.active
                && this.lastUpdate != this.distributedPingCache.lastUpdate()) {

            if (this.ditributedIndicatorValueRecordId == null) {
                this.tryRecoverIndicatorRecord();
            }

            final Long indicatorValue = this.distributedPingCache
                    .getIndicatorValue(this.ditributedIndicatorValueRecordId);
            if (indicatorValue != null) {
                this.currentValue = indicatorValue.doubleValue();
            }
            this.lastUpdate = this.distributedPingCache.lastUpdate();
        }

        return this.currentValue;
    }

}
