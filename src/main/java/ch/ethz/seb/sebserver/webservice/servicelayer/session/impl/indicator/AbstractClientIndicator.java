/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public abstract class AbstractClientIndicator implements ClientIndicator {

    protected Long indicatorId;
    protected Long examId;
    protected Long connectionId;
    protected boolean cachingEnabled;

    protected boolean valueInitializes = false;
    protected double currentValue = Double.NaN;

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean cachingEnabled) {

        this.indicatorId = (indicatorDefinition != null) ? indicatorDefinition.id : -1;
        this.examId = (indicatorDefinition != null) ? indicatorDefinition.examId : -1;
        this.connectionId = connectionId;
        this.cachingEnabled = cachingEnabled;
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
        this.currentValue = Double.NaN;
        this.valueInitializes = false;
    }

    @Override
    public double getValue() {
        if (!this.valueInitializes || !this.cachingEnabled) {
            this.currentValue = computeValueAt(DateTime.now(DateTimeZone.UTC).getMillis());
            this.valueInitializes = true;
        }

        return this.currentValue;
    }

}
