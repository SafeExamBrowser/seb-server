/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

import java.math.BigDecimal;
import java.util.List;

import static ch.ethz.seb.sebserver.gbl.api.API.LOG_EVENT_TAG_WLAN_STATUS;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

@Lazy
@Component(IndicatorType.Names.WLAN_STATUS)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WLANStatusIndicator extends AbstractLogNumberIndicator {

    protected WLANStatusIndicator(
            final DistributedIndicatorValueService distributedPingCache,
            final ClientEventRecordMapper clientEventRecordMapper) {

        super(distributedPingCache, clientEventRecordMapper, EventType.INFO_LOG);
        super.tags = new String[] { LOG_EVENT_TAG_WLAN_STATUS };
    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);
        super.tags = new String[] { LOG_EVENT_TAG_WLAN_STATUS };
    }

    @Override
    protected BigDecimal fetchValue(long timestamp) {
        final List<ClientEventRecord> execute = this.clientEventRecordMapper.selectByExample()
                .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                .and(ClientEventRecordDynamicSqlSupport.type, isEqualTo(EventType.INFO_LOG.id))
              //  .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                .and(ClientEventRecordDynamicSqlSupport.text, isLike(Utils.toSQLWildcard(LOG_EVENT_TAG_WLAN_STATUS)))
                .orderBy(ClientEventRecordDynamicSqlSupport.serverTime.descending())
                .limit(1)
                .build()
                .execute();

        if (execute == null || execute.isEmpty()) {
            return new BigDecimal(super.currentValue);
        }

        return  execute.getLast().getNumericValue();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.WLAN_STATUS;
    }

    @Override
    public final boolean hasIncident() {
        return this.currentValue <= this.dataMap.incidentThreshold;
    }

    @Override
    public boolean hasWarning() {
        return this.currentValue <= this.dataMap.warningThreshold;
    }
    
}
