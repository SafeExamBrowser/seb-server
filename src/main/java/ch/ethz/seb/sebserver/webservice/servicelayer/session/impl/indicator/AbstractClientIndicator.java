/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public abstract class AbstractClientIndicator implements ClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractClientIndicator.class);

    protected final DistributedIndicatorValueService distributedPingCache;

    protected Long indicatorId;
    protected Long examId;
    protected Long connectionId;
    protected boolean cachingEnabled;
    protected boolean active = true;

    protected Long ditributedIndicatorValueRecordId = null;

    protected boolean initialized = false;
    protected double currentValue = Double.NaN;

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

        this.indicatorId = (indicatorDefinition != null && indicatorDefinition.id != null)
                ? indicatorDefinition.id
                : -1;
        this.examId = (indicatorDefinition != null && indicatorDefinition.examId != null)
                ? indicatorDefinition.examId
                : -1;
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
        return this.currentValue;
    }

}
