/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

public abstract class AbstractLogLevelCountIndicator extends AbstractLogIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractLogLevelCountIndicator.class);

    protected final ClientEventRecordMapper clientEventRecordMapper;

    protected AbstractLogLevelCountIndicator(
            final DistributedIndicatorValueService distributedPingCache,
            final ClientEventRecordMapper clientEventRecordMapper,
            final EventType... eventTypes) {

        super(distributedPingCache, eventTypes);
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    public final void notifyValueChange(final String textValue, final double numValue) {
        if (this.tags == null || this.tags.length == 0 || hasTag(textValue)) {
            if (super.distributedIndicatorValueRecordId != null) {
                this.distributedIndicatorValueService.incrementIndicatorValue(super.distributedIndicatorValueRecordId);
            }
            this.currentValue = getValue() + 1d;
        }
    }

    @Override
    public final boolean hasIncident() {
        return this.currentValue >= this.dataMap.incidentThreshold;
    }

    @Override
    public boolean hasWarning() {
        return this.currentValue >= this.dataMap.warningThreshold;
    }

    @Override
    public Indicator.DataMap getDataMap() {
        return dataMap;
    }

    @Override
    public double computeValueAt(final long timestamp) {

        if (log.isTraceEnabled()) {
            log.trace("computeValueAt: {}", timestamp);
        }

        try {

            final Long numberOfLogs = this.clientEventRecordMapper
                    .countByExample()
                    .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                    .and(ClientEventRecordDynamicSqlSupport.type, isIn(this.eventTypeIds))
                    .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                    .and(
                            ClientEventRecordDynamicSqlSupport.text,
                            isLikeWhenPresent(getfirstTagSQL()),
                            getSubTagSQL())
                    .build()
                    .execute();

            // update active indicator value record on persistent when caching is not enabled
            if (this.active && this.distributedIndicatorValueRecordId != null) {
                this.distributedIndicatorValueService.updateIndicatorValue(
                        this.distributedIndicatorValueRecordId,
                        numberOfLogs.longValue());
            }

            return numberOfLogs.doubleValue();

        } catch (final Exception e) {
            log.error("Failed to get indicator count from persistent storage: ", e);
            return super.currentValue;
        }
    }

    private String getfirstTagSQL() {
        if (this.tags == null || this.tags.length == 0) {
            return null;
        }

        return Utils.toSQLWildcard(this.tags[0]);
    }

    @SuppressWarnings("unchecked")
    private SqlCriterion<String>[] getSubTagSQL() {
        if (this.tags == null || this.tags.length == 0 || this.tags.length == 1) {
            return new SqlCriterion[0];
        }

        final SqlCriterion<String>[] result = new SqlCriterion[this.tags.length - 1];
        for (int i = 1; i < this.tags.length; i++) {
            result[i - 1] = SqlBuilder.or(
                    ClientEventRecordDynamicSqlSupport.text,
                    isLike(Utils.toSQLWildcard(this.tags[1])));
        }

        return result;
    }

}
