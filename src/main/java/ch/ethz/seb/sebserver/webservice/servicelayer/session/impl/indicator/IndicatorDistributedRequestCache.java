/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import java.util.stream.Collectors;

import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventLastPingMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;

@Lazy
@Service
@WebServiceProfile
public class IndicatorDistributedRequestCache {

    public static final String LAST_PING_TIME_CACHE = "LAST_PING_TIME_CACHE";

    private static final Logger log = LoggerFactory.getLogger(IndicatorDistributedRequestCache.class);

    private final ClientEventLastPingMapper clientEventLastPingMapper;

    public IndicatorDistributedRequestCache(final ClientEventLastPingMapper clientEventLastPingMapper) {
        this.clientEventLastPingMapper = clientEventLastPingMapper;
    }

    @Cacheable(
            cacheNames = LAST_PING_TIME_CACHE,
            key = "#examId",
            unless = "#result == null")
    public ConcurrentHashMap<Long, Long> getPingTimes(final Long examId) {
        return new ConcurrentHashMap<>(this.clientEventLastPingMapper.selectByExample()
                .join(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientEventRecordDynamicSqlSupport.clientConnectionId,
                        SqlBuilder.equalTo(ClientConnectionRecordDynamicSqlSupport.id))
                .where(ClientConnectionRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(ClientEventRecordDynamicSqlSupport.type, isEqualTo(EventType.LAST_PING.id))
                .build()
                .execute()
                .stream().collect(Collectors.toMap(rec -> rec.connectionId, rec -> rec.lastPingTime)));
    }

    @CacheEvict(
            cacheNames = LAST_PING_TIME_CACHE,
            key = "#examId")
    public void evictPingTimes(final Long examId) {
        if (log.isDebugEnabled()) {
            log.debug("Evict LAST_PING_TIME_CACHE for examId: {}", examId);
        }
    }

}
