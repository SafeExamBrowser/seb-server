/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PingHandlingStrategy;

@Lazy
@Component
@WebServiceProfile
public class DistributedServerPingHandler implements PingHandlingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DistributedServerPingHandler.class);

    private final ExamSessionCacheService examSessionCacheService;
    private final ClientEventRecordMapper clientEventRecordMapper;

    protected DistributedServerPingHandler(
            final ExamSessionCacheService examSessionCacheService,
            final ClientEventRecordMapper clientEventRecordMapper) {

        this.examSessionCacheService = examSessionCacheService;
        this.clientEventRecordMapper = clientEventRecordMapper;
    }

    @Override
    @Transactional
    public void notifyPing(final String connectionToken, final long timestamp, final int pingNumber) {

        // store last ping in event
        final ClientEventRecord pingRecord = this.examSessionCacheService.getPingRecord(connectionToken);
        if (pingRecord != null) {
            pingRecord.setTimestamp(timestamp);
            pingRecord.setNumericValue(new BigDecimal(pingNumber));
            this.clientEventRecordMapper.updateByPrimaryKeySelective(pingRecord);
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

    @Override
    public void initForConnection(final Long connectionId, final String connectionToken) {

        if (log.isDebugEnabled()) {
            log.debug("Intitalize distributed ping handler for connection: {}", connectionId);
        }

        final ClientEventRecord clientEventRecord = new ClientEventRecord();
        clientEventRecord.setConnectionId(connectionId);
        clientEventRecord.setType(EventType.LAST_PING.id);
        clientEventRecord.setTimestamp(Utils.getMillisecondsNow());
        this.clientEventRecordMapper.insertSelective(clientEventRecord);
    }

}
