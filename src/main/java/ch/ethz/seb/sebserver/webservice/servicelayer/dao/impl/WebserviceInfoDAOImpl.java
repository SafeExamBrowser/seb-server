/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.List;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.WebserviceServerInfoRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.WebserviceServerInfoRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.WebserviceServerInfoRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Lazy
@Component
@WebServiceProfile
public class WebserviceInfoDAOImpl implements WebserviceInfoDAO {

    private static final Logger log = LoggerFactory.getLogger(WebserviceInfoDAOImpl.class);

    private final WebserviceServerInfoRecordMapper webserviceServerInfoRecordMapper;
    private final long masterDelayTimeThreshold;
    private final boolean forceMaster;

    public WebserviceInfoDAOImpl(
            final WebserviceServerInfoRecordMapper webserviceServerInfoRecordMapper,
            @Value("${sebserver.webservice.forceMaster:false}") final boolean forceMaster,
            @Value("${sebserver.webservice.master.delay.threshold:30000}") final long masterDelayTimeThreshold) {

        this.webserviceServerInfoRecordMapper = webserviceServerInfoRecordMapper;
        this.masterDelayTimeThreshold = masterDelayTimeThreshold;
        this.forceMaster = forceMaster;
    }

    @Override
    @Transactional
    public boolean isInitialized() {
        try {
            this.webserviceServerInfoRecordMapper
                    .selectByExample()
                    .build()
                    .execute();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    @Transactional
    @Override
    public boolean register(final String uuid, final String address) {
        try {
            this.webserviceServerInfoRecordMapper.insert(new WebserviceServerInfoRecord(null, uuid, address, 0, 0L));
            return true;
        } catch (final Exception e) {
            log.error("Failed to register webservice: uuid: {}, address: {}", uuid, address, e);
            return false;
        }
    }

    @Transactional
    @Override
    public boolean isMaster(final String uuid) {
        try {
            final List<WebserviceServerInfoRecord> masters = this.webserviceServerInfoRecordMapper
                    .selectByExample()
                    .where(WebserviceServerInfoRecordDynamicSqlSupport.master, SqlBuilder.isNotEqualTo(0))
                    .build()
                    .execute();

            if (masters != null && !masters.isEmpty()) {
                if (masters.size() > 1) {

                    log.error("There are more then one master registered: ", masters);
                    log.info("Reset masters and set this webservice as new master");

                    masters.stream().forEach(masterRec -> this.webserviceServerInfoRecordMapper
                            .updateByPrimaryKeySelective(
                                    new WebserviceServerInfoRecord(
                                            masterRec.getId(),
                                            null,
                                            null,
                                            0,
                                            0L)));
                    return this.setMasterTo(uuid);
                }

                final WebserviceServerInfoRecord masterRec = masters.get(0);
                if (masterRec.getUuid().equals(uuid)) {
                    // This webservice is the master. Update time-stamp to remain being master
                    final long now = Utils.getMillisecondsNow();
                    this.webserviceServerInfoRecordMapper.updateByPrimaryKeySelective(
                            new WebserviceServerInfoRecord(masterRec.getId(), null, null, null, now));

                    if (log.isDebugEnabled()) {
                        log.trace("Update master webservice {} time: {}", uuid, now);
                    }

                    return true;
                } else {
                    // Another webservice is master. Check if still alive...
                    // Force this service to become master if the other master is not alive anymore
                    // Or if this service is forced to be the master service
                    final long now = Utils.getMillisecondsNow();
                    final long lastUpdateSince = now - masterRec.getUpdateTime();
                    if (lastUpdateSince > this.masterDelayTimeThreshold || this.forceMaster) {
                        return forceMaster(uuid, masterRec.getUuid(), masterRec.getId());
                    }
                }
            } else {
                // We have no master yet so set this as master service
                return setMasterTo(uuid);
            }

            return false;
        } catch (final Exception e) {
            log.error("Failed to check and set master webservice: ", e);
            TransactionHandler.rollback();
            return false;
        }
    }

    private boolean forceMaster(final String uuid, final String otherUUID, final Long otherId) {

        log.info("Change webservice master from uuid: {} to uuid: {}", otherUUID, uuid);

        this.webserviceServerInfoRecordMapper.updateByPrimaryKeySelective(
                new WebserviceServerInfoRecord(otherId, null, null, 0, 0L));

        return setMasterTo(uuid);
    }

    private boolean setMasterTo(final String uuid) {

        final long now = Utils.getMillisecondsNow();

        // check if this is registered
        final List<WebserviceServerInfoRecord> entries = this.webserviceServerInfoRecordMapper.selectByExample()
                .where(WebserviceServerInfoRecordDynamicSqlSupport.uuid, SqlBuilder.isEqualTo(uuid))
                .build()
                .execute();

        if (entries == null || entries.isEmpty()) {
            log.warn("The webservice with uuid: {} is not registered and cannot become a master", uuid);
            return false;
        }

        final Integer execute = this.webserviceServerInfoRecordMapper.updateByExampleSelective(
                new WebserviceServerInfoRecord(null, null, null, 1, now))
                .where(WebserviceServerInfoRecordDynamicSqlSupport.uuid, SqlBuilder.isEqualTo(uuid))
                .build()
                .execute();
        if (execute == null || execute.intValue() <= 0) {
            log.error("Failed to update webservice with uuid: {} to become master", uuid);
            return false;
        }

        log.info("Set webservice {} as master", uuid);

        return true;
    }

    @Transactional
    @Override
    public boolean unregister(final String uuid) {
        try {
            this.webserviceServerInfoRecordMapper.deleteByExample()
                    .where(WebserviceServerInfoRecordDynamicSqlSupport.uuid, SqlBuilder.isEqualTo(uuid))
                    .build()
                    .execute();
            return true;
        } catch (final Exception e) {
            log.warn("Failed to unregister webservice: uuid: {}, cause: ", uuid, e);
            return false;
        }
    }

}
