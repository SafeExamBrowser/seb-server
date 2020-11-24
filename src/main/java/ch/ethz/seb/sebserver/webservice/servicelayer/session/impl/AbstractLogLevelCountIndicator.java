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

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlCriterion;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;

public abstract class AbstractLogLevelCountIndicator extends AbstractClientIndicator {

    private final Set<EventType> observed;
    private final List<Integer> eventTypeIds;
    private final ClientEventRecordMapper clientEventRecordMapper;
    protected String[] tags;

    protected AbstractLogLevelCountIndicator(
            final ClientEventRecordMapper clientEventRecordMapper,
            final EventType... eventTypes) {

        this.clientEventRecordMapper = clientEventRecordMapper;
        this.observed = Collections.unmodifiableSet(EnumSet.of(eventTypes[0], eventTypes));
        this.eventTypeIds = Utils.immutableListOf(Arrays.stream(eventTypes)
                .map(et -> et.id)
                .collect(Collectors.toList()));

    }

    @Override
    public void init(final Indicator indicatorDefinition, final Long connectionId, final boolean cachingEnabled) {
        super.init(indicatorDefinition, connectionId, cachingEnabled);
        if (indicatorDefinition == null || indicatorDefinition.tags == null) {
            this.tags = null;
        } else {
            this.tags = StringUtils.split(indicatorDefinition.tags, Constants.COMMA);
            for (int i = 0; i < this.tags.length; i++) {
                this.tags[i] = Constants.ANGLE_BRACE_OPEN + this.tags[i] + Constants.ANGLE_BRACE_CLOSE;
            }
        }
    }

    @Override
    public double computeValueAt(final long timestamp) {

        final Long errors = this.clientEventRecordMapper.countByExample()
                .where(ClientEventRecordDynamicSqlSupport.clientConnectionId, isEqualTo(this.connectionId))
                .and(ClientEventRecordDynamicSqlSupport.type, isIn(this.eventTypeIds))
                .and(ClientEventRecordDynamicSqlSupport.serverTime, isLessThan(timestamp))
                .and(
                        ClientEventRecordDynamicSqlSupport.text,
                        isLikeWhenPresent(getfirstTagSQL()),
                        getSubTagSQL())
                .build()
                .execute();

        return errors.doubleValue();
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

    @Override
    public void notifyValueChange(final ClientEvent event) {
        if (this.tags == null || this.tags.length == 0) {
            this.currentValue = getValue() + 1d;
        } else if (hasTag(event.text)) {
            this.currentValue = getValue() + 1d;
        }
    }

    private boolean hasTag(final String text) {
        if (text == null) {
            return false;
        }

        for (int i = 0; i < this.tags.length; i++) {
            if (text.startsWith(this.tags[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.observed;
    }

}
