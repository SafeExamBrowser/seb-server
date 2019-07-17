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

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    private final ClientEventExtentionMapper clientEventExtentionMapper;

    protected int pingCount = 0;
    protected int pingNumber = 0;

    protected AbstractPingIndicator(final ClientEventExtentionMapper clientEventExtentionMapper) {
        super();
        this.clientEventExtentionMapper = clientEventExtentionMapper;
    }

    public void notifyPing(final long timestamp, final int pingNumber) {
        super.currentValue = timestamp;
        this.pingCount++;
        this.pingNumber = pingNumber;
    }

    @Override
    public double computeValueAt(final long timestamp) {
        if (this.cachingEnabled) {
            return timestamp;
        } else {

            final Long lastPing =
                    this.clientEventExtentionMapper.maxByExample(ClientEventRecordDynamicSqlSupport.timestamp)
                            .where(ClientEventRecordDynamicSqlSupport.connectionId, isEqualTo(this.connectionId))
                            .and(ClientEventRecordDynamicSqlSupport.type, isEqualTo(EventType.LAST_PING.id))
                            .and(ClientEventRecordDynamicSqlSupport.timestamp, isLessThan(timestamp))
                            .build()
                            .execute();

            if (lastPing == null) {
                return 0.0;
            } else {
                return lastPing.doubleValue();
            }
        }
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

    @JsonIgnore
    public int getPingCount() {
        return this.pingCount;
    }

    @JsonIgnore
    public int getPingNumber() {
        return this.pingNumber;
    }

}
