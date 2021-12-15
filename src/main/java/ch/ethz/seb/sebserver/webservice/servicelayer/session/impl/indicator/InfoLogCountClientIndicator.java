/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

@Lazy
@Component(IndicatorType.Names.INFO_COUNT)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InfoLogCountClientIndicator extends AbstractLogLevelCountIndicator {

    protected InfoLogCountClientIndicator(
            final DistributedIndicatorValueService distributedPingCache,
            final ClientEventRecordMapper clientEventRecordMapper) {

        super(distributedPingCache, clientEventRecordMapper, EventType.INFO_LOG);
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.INFO_COUNT;
    }

}
