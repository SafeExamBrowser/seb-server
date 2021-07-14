/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

public abstract class AbstractClientIndicator implements ClientIndicator {

    private static final long PERSISTENT_UPDATE_INTERVAL = 3 * Constants.SECOND_IN_MILLIS;

    protected Long indicatorId;
    protected Long examId;
    protected Long connectionId;
    protected boolean cachingEnabled;
    protected boolean active = true;
    protected long lastPersistentUpdate = 0;

    protected boolean valueInitializes = false;
    protected double currentValue = Double.NaN;

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        this.indicatorId = (indicatorDefinition != null) ? indicatorDefinition.id : -1;
        this.examId = (indicatorDefinition != null) ? indicatorDefinition.examId : -1;
        this.connectionId = connectionId;
        this.active = active;
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
        if (!this.valueInitializes) {
            final long now = Utils.getMillisecondsNow();
            this.currentValue = computeValueAt(now);
            this.lastPersistentUpdate = now;
            this.valueInitializes = true;
        }

        if (!this.cachingEnabled && this.active) {
            final long now = System.currentTimeMillis();
            if (now - this.lastPersistentUpdate > PERSISTENT_UPDATE_INTERVAL) {
                this.currentValue = computeValueAt(now);
                this.lastPersistentUpdate = now;
            }
        }

        return this.currentValue;
    }

}
