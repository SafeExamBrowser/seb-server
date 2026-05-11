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

import java.util.function.Consumer;

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
            double value = getValue() + 1d;
            if (super.distributedIndicatorValueRecordId != null && !Double.isNaN(value)) {
                distributedIndicatorValueService.updateIndicatorValueAsync(
                        super.distributedIndicatorValueRecordId,
                        (long) value
                );
            } else {
                currentValue = value;
            }
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

        distributedIndicatorValueService.addIndicatorValueFetch(
                new AsyncValueFetch(timestamp, value -> this.currentValue = value));

        return currentValue;
    }

    protected Double fetchValue(final long timestamp) {
        Long value = this.clientEventRecordMapper
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

        if (value == null) {
            return null;
        }

        return value.doubleValue();
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

    private final class AsyncValueFetch implements FetchIndicatorValue {

        private final long timestamp;
        private final Consumer<Double> callback;

        private AsyncValueFetch(long timestamp, Consumer<Double> callback) {
            this.timestamp = timestamp;
            this.callback = callback;
        }

        @Override
        public void fetch() {
            try {

                final Double numberOfLogs = fetchValue(timestamp);

                if (numberOfLogs != null) {
                    // update active indicator value record on persistent when caching is not enabled
                    if (active && distributedIndicatorValueRecordId != null) {
                        distributedIndicatorValueService.updateIndicatorValueAsync(
                                distributedIndicatorValueRecordId,
                                numberOfLogs.longValue());
                    }

                    callback.accept(numberOfLogs);
                } else {
                    callback.accept(currentValue);
                }

            } catch (final Exception e) {
                log.error("Failed to get indicator count from persistent storage: ", e);
                callback.accept(currentValue);
            }
        }
    }

}
