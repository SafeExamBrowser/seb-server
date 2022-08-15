/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.math.BigDecimal;
import java.util.List;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
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
    public void notifyValueChange(final ClientEvent event) {
        valueChanged(event.text, event.getValue());
    }

    @Override
    public void notifyValueChange(final ClientEventRecord clientEventRecord) {
        final BigDecimal numericValue = clientEventRecord.getNumericValue();
        if (numericValue != null) {
            valueChanged(clientEventRecord.getText(), numericValue.doubleValue());
        }
    }

    private void valueChanged(final String text, final double value) {
        if (this.tags == null || this.tags.length == 0 || hasTag(text)) {
            if (super.ditributedIndicatorValueRecordId != null) {
                if (!this.distributedIndicatorValueService.updateIndicatorValueAsync(
                        this.ditributedIndicatorValueRecordId,
                        Double.valueOf(value).longValue())) {

                    this.currentValue = computeValueAt(Utils.getMillisecondsNow());
                } else {
                    this.currentValue = value;
                }
            } else {
                this.currentValue = value;
            }
        }
    }

    @Override
    public double computeValueAt(final long timestamp) {

        if (log.isTraceEnabled()) {
            log.trace("computeValueAt: {}", timestamp);
        }

        try {

            final List<ClientEventRecord> execute = this.clientEventRecordMapper.selectByExample()
                    .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                    .and(ClientEventRecordDynamicSqlSupport.type, isIn(this.eventTypeIds))
                    .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                    .and(
                            ClientEventRecordDynamicSqlSupport.text,
                            isLikeWhenPresent(getfirstTagSQL()),
                            getSubTagSQL())
                    .orderBy(ClientEventRecordDynamicSqlSupport.serverTime)
                    .build()
                    .execute();

            if (execute == null || execute.isEmpty()) {
                return super.currentValue;
            }

            final BigDecimal numericValue = execute.get(execute.size() - 1).getNumericValue();
            if (numericValue != null) {

                // update active indicator value record on persistent when caching is not enabled
                if (this.active && this.ditributedIndicatorValueRecordId != null) {
                    this.distributedIndicatorValueService.updateIndicatorValue(
                            this.ditributedIndicatorValueRecordId,
                            numericValue.longValue());
                }

                return numericValue.doubleValue();
            } else {
                return super.currentValue;
            }

        } catch (final Exception e) {
            log.error("Failed to get indicator number from persistent storage: {}", e.getMessage());
            return this.currentValue;
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
