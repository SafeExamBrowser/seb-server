/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValueHolder;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

public abstract class AbstractLogLevelCountIndicator extends AbstractClientIndicator {

    private final Set<EventType> observed;
    private final List<Integer> eventTypeIds;
    private final ClientEventRecordMapper clientEventRecordMapper;

    protected AbstractLogLevelCountIndicator(
            final ClientEventRecordMapper clientEventRecordMapper,
            final EventType... eventTypes) {

        this.clientEventRecordMapper = clientEventRecordMapper;
        this.observed = Collections.unmodifiableSet(EnumSet.of(eventTypes[0], eventTypes));
        this.eventTypeIds = Utils.immutableListOf(Arrays.asList(eventTypes)
                .stream()
                .map(et -> et.id)
                .collect(Collectors.toList()));
    }

    @Override
    public double computeValueAt(final long timestamp) {

        final Long errors = this.clientEventRecordMapper.countByExample()
                .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                .and(ClientEventRecordDynamicSqlSupport.type, isIn(this.eventTypeIds))
                .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                .build()
                .execute();

        return errors.doubleValue();
    }

    @Override
    public void notifyValueChange(final IndicatorValueHolder indicatorValueHolder) {
        this.currentValue = getValue() + 1d;
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.observed;
    }

}
