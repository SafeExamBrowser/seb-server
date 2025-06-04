/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public abstract class AbstractClientIndicator implements ClientIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractClientIndicator.class);

    protected final DistributedIndicatorValueService distributedIndicatorValueService;

    protected Long indicatorId = -1L;
    protected Long examId = -1L;
    protected Long connectionId;
    protected boolean cachingEnabled;
    protected boolean active = true;

    protected Long distributedIndicatorValueRecordId = null;

    protected boolean initialized = false;
    protected double currentValue = Double.NaN;

    protected long lastUpdate = 0;
    
    protected Indicator.DataMap dataMap;

    public AbstractClientIndicator(final DistributedIndicatorValueService distributedIndicatorValueService) {
        super();
        this.distributedIndicatorValueService = distributedIndicatorValueService;
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        if (indicatorDefinition != null) {
            this.dataMap = indicatorDefinition.dataMap;

//            this.incidentThreshold = (!indicatorDefinition.type.inverse)
//                    ? indicatorDefinition.thresholds.stream()
//                            .map(t -> t.value)
//                            .max(Comparator.naturalOrder())
//                            .orElse(0.0)
//                    : indicatorDefinition.thresholds.stream()
//                            .map(t -> t.value)
//                            .min(Comparator.naturalOrder())
//                            .orElse(0.0);
            this.indicatorId = indicatorDefinition.id;
            this.examId = indicatorDefinition.examId;
        } else {
            this.dataMap = new Indicator.DataMap(
                    Double.MAX_VALUE, 
                    Double.MAX_VALUE,
                    new double[0], 
                    new String[0]);
        }

        this.connectionId = connectionId;
        this.active = active;
        this.cachingEnabled = cachingEnabled;

        if (!this.cachingEnabled && this.active) {
            this.distributedIndicatorValueRecordId = this.distributedIndicatorValueService
                    .getIndicatorForConnection(connectionId, getType());
        }

        this.currentValue = computeValueAt(Utils.getMillisecondsNow());
        this.initialized = true;
    }

    @Override
    @JsonIgnore
    public Indicator.DataMap getDataMap() {
        return dataMap;
    }

    protected void tryRecoverIndicatorRecord() {
        this.distributedIndicatorValueRecordId = this.distributedIndicatorValueService
                .createIndicatorForConnection(
                        this.connectionId,
                        getType(),
                        0);

        if (this.distributedIndicatorValueRecordId == null && log.isDebugEnabled()) {
            log.debug("Failed to recover from missing indicator value cache record: {} type: {}",
                    this.connectionId,
                    getType());
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
                && this.lastUpdate != this.distributedIndicatorValueService.lastUpdate()) {

            if (this.distributedIndicatorValueRecordId == null) {
                this.tryRecoverIndicatorRecord();
            }

            final Long indicatorValue = this.distributedIndicatorValueService
                    .getIndicatorValue(this.distributedIndicatorValueRecordId);
            if (indicatorValue != null) {
                this.currentValue = indicatorValue.doubleValue();
            }
            this.lastUpdate = this.distributedIndicatorValueService.lastUpdate();
        }

        return this.currentValue;
    }

}
