/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValueHolder;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper;

@Lazy
@Component(IndicatorType.Names.LAST_PING)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class PingIntervalClientIndicator extends AbstractPingIndicator {

    public PingIntervalClientIndicator(final ClientEventExtentionMapper clientEventExtentionMapper) {
        super(clientEventExtentionMapper);
        this.cachingEnabled = true;
        this.currentValue = computeValueAt(Utils.getMillisecondsNow());
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

}
