/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isLessThan;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValueHolder;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

@Lazy
@Component(IndicatorType.Names.ERROR_COUNT)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class ErrorCountClientIndicator extends AbstractClientIndicator {

    private final Set<EventType> OBSERVED_SET = Collections.unmodifiableSet(EnumSet.of(EventType.ERROR_LOG));

    private final ClientEventRecordMapper clientEventRecordMapper;

    protected ErrorCountClientIndicator(final ClientEventRecordMapper clientEventRecordMapper) {
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.ERROR_COUNT;
    }

    @Override
    public double computeValueAt(final long timestamp) {

        final Long errors = this.clientEventRecordMapper.countByExample()
                .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                .and(ClientEventRecordDynamicSqlSupport.type, isEqualTo(ClientEvent.EventType.ERROR_LOG.id))
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
        return this.OBSERVED_SET;
    }

}
