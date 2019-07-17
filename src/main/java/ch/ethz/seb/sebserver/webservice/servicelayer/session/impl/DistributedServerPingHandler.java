/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.math.BigDecimal;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PingHandlingStrategy;

@Lazy
@Component
@WebServiceProfile
public class DistributedServerPingHandler implements PingHandlingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DistributedServerPingHandler.class);

    private final ExamSessionCacheService examSessionCacheService;
    private final EventHandlingStrategy eventHandlingStrategy;
    private final ClientEventRecordMapper clientEventRecordMapper;

    protected DistributedServerPingHandler(
            final ExamSessionCacheService examSessionCacheService,
            final EventHandlingStrategyFactory eventHandlingStrategyFactory,
            final ClientEventRecordMapper clientEventRecordMapper) {

        this.examSessionCacheService = examSessionCacheService;
        this.eventHandlingStrategy = eventHandlingStrategyFactory.get();
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    public void notifyPing(final String connectionToken, final long timestamp, final int pingNumber) {
        // store last ping in event
        final ClientEventRecord pingRecord = this.examSessionCacheService.getPingRecord(connectionToken);
        final boolean update = pingRecord.getId() == null && pingRecord.getTimestamp() != null;
        pingRecord.setTimestamp(timestamp);
        pingRecord.setNumericValue(new BigDecimal(pingNumber));
        if (update) {
            updatePingEventId(pingRecord);
        } else {
            this.eventHandlingStrategy.accept(pingRecord);
        }

        // update ping indicators
        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionToken);

        if (activeClientConnection != null) {
            activeClientConnection.pingMappings
                    .stream()
                    .forEach(pingIndicator -> pingIndicator.notifyPing(timestamp, pingNumber));
        }

    }

    private void updatePingEventId(final ClientEventRecord pingRecord) {
        try {
            final Long id = this.clientEventRecordMapper.selectIdsByExample()
                    .where(
                            ClientEventRecordDynamicSqlSupport.connectionId,
                            SqlBuilder.isEqualTo(pingRecord.getConnectionId()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.type,
                            SqlBuilder.isEqualTo(EventType.LAST_PING.id))
                    .build()
                    .execute()
                    .stream()
                    .collect(Utils.toSingleton());
            if (id != null) {
                pingRecord.setId(id);
            }
        } catch (final Exception e) {
            log.error("Failed to verify record identifier for re-usable ClientEvent for ping");
        }
    }

}
