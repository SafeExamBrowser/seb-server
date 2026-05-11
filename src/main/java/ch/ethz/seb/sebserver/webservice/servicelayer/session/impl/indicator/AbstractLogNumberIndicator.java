/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.apache.ibatis.ognl.OgnlOps.doubleValue;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

public abstract class AbstractLogNumberIndicator extends AbstractLogIndicator {

    private static final Logger log = LoggerFactory.getLogger(AbstractLogNumberIndicator.class);

    protected final ClientEventRecordMapper clientEventRecordMapper;

    protected AbstractLogNumberIndicator(
            final DistributedIndicatorValueService distributedPingCache,
            final ClientEventRecordMapper clientEventRecordMapper,
            final EventType... eventTypes) {

        super(distributedPingCache, eventTypes);
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    public void notifyValueChange(final String textValue, final double numValue) {
        if (this.tags == null || this.tags.length == 0 || hasTag(textValue)) {
            if (super.distributedIndicatorValueRecordId != null) {
                if (!this.distributedIndicatorValueService.updateIndicatorValueAsync(
                        this.distributedIndicatorValueRecordId,
                        Double.valueOf(numValue).longValue())) {

                    this.currentValue = computeValueAt(Utils.getMillisecondsNow());
                } else {
                    this.currentValue = numValue;
                }
            } else {
                this.currentValue = numValue;
            }
        }
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
        final List<ClientEventRecord> execute = this.clientEventRecordMapper.selectByExample()
                .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                .and(ClientEventRecordDynamicSqlSupport.type, isIn(this.eventTypeIds))
                .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                .and(
                        ClientEventRecordDynamicSqlSupport.text,
                        isLikeWhenPresent(getFirstTagSQL()),
                        getSubTagSQL())
                .orderBy(ClientEventRecordDynamicSqlSupport.serverTime.descending())
                .limit(1)
                .build()
                .execute();

        if (execute == null || execute.isEmpty()) {
            return super.currentValue;
        }

        BigDecimal numericValue = execute.getLast().getNumericValue();
        if (numericValue != null) {
            return numericValue.doubleValue();
        } else {
            return null;
        }
    }

    private String getFirstTagSQL() {
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

                final Double numericValue = fetchValue(timestamp);
                if (numericValue != null) {

                    // update active indicator value record on persistent when caching is not enabled
                    if (active && distributedIndicatorValueRecordId != null) {
                        distributedIndicatorValueService.updateIndicatorValueAsync(
                                distributedIndicatorValueRecordId,
                                numericValue.longValue());
                    }

                    callback.accept(numericValue);
                } else {
                    callback.accept(currentValue);
                }

            } catch (final Exception e) {
                log.error("Failed to get indicator number from persistent storage: {}", e.getMessage());
                callback.accept(currentValue);
            }
        }
    }

}
