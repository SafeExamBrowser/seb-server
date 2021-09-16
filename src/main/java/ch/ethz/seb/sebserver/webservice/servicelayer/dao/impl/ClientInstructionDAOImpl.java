/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientInstructionDAO;

@Lazy
@Component
@WebServiceProfile
public class ClientInstructionDAOImpl implements ClientInstructionDAO {

    private static final Logger log = LoggerFactory.getLogger(ClientInstructionDAOImpl.class);

    private final ClientInstructionRecordMapper clientInstructionRecordMapper;
    private final JSONMapper jsonMapper;

    protected ClientInstructionDAOImpl(
            final ClientInstructionRecordMapper clientInstructionRecordMapper,
            final JSONMapper jsonMapper) {

        this.clientInstructionRecordMapper = clientInstructionRecordMapper;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientInstructionRecord>> getAllActive() {
        return Result.tryCatch(() -> {
            final long millisNowMinusOneMinute = DateTime.now(DateTimeZone.UTC).minusMinutes(1).getMillis();
            return this.clientInstructionRecordMapper
                    .selectByExample()
                    .where(ClientInstructionRecordDynamicSqlSupport.timestamp,
                            SqlBuilder.isGreaterThanOrEqualTo(millisNowMinusOneMinute))
                    .build()
                    .execute();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientInstructionRecord>> getAllActive(final String connectionToken) {
        return Result.tryCatch(() -> {
            final long millisNowMinusOneMinute = DateTime.now(DateTimeZone.UTC).minusMinutes(1).getMillis();
            return this.clientInstructionRecordMapper
                    .selectByExample()
                    .where(ClientInstructionRecordDynamicSqlSupport.timestamp,
                            SqlBuilder.isGreaterThanOrEqualTo(millisNowMinusOneMinute))
                    .and(
                            ClientInstructionRecordDynamicSqlSupport.connectionToken,
                            SqlBuilder.isEqualTo(connectionToken))
                    .build()
                    .execute();
        });
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteAllInactive(final long timestamp) {
        return Result.tryCatch(() -> {

            final List<ClientInstructionRecord> inactive = this.clientInstructionRecordMapper
                    .selectByExample()
                    .where(ClientInstructionRecordDynamicSqlSupport.timestamp,
                            SqlBuilder.isLessThanOrEqualTo(timestamp))
                    .build()
                    .execute();

            if (inactive != null && !inactive.isEmpty()) {

                this.clientInstructionRecordMapper
                        .deleteByExample()
                        .where(ClientInstructionRecordDynamicSqlSupport.timestamp,
                                SqlBuilder.isLessThanOrEqualTo(timestamp))
                        .build()
                        .execute();

                return inactive.stream()
                        .map(r -> new EntityKey(r.getId(), EntityType.CLIENT_INSTRUCTION))
                        .collect(Collectors.toList());

            }

            return Collections.emptyList();
        });
    }

    @Override
    @Transactional
    public Result<Void> delete(final Long id) {
        return Result.tryCatch(() -> {
            final int deleteByPrimaryKey = this.clientInstructionRecordMapper.deleteByPrimaryKey(id);
            if (deleteByPrimaryKey != 1) {
                throw new RuntimeException("Failed to delete ClientInstruction with id: " + id);
            } else if (log.isDebugEnabled()) {
                log.debug("Deleted client instruction with id: {}", id);
            }
        });
    }

    @Override
    @Transactional
    public Result<ClientInstructionRecord> insert(
            final Long examId,
            final InstructionType type,
            final String attributes,
            final String connectionToken,
            final boolean needsConfirmation) {

        return Result.tryCatch(() -> {
            final ClientInstructionRecord clientInstructionRecord = new ClientInstructionRecord(
                    null,
                    examId,
                    connectionToken,
                    type.name(),
                    attributes,
                    (needsConfirmation) ? 1 : 0,
                    DateTime.now(DateTimeZone.UTC).getMillis());

            this.clientInstructionRecordMapper.insert(clientInstructionRecord);

            if (needsConfirmation) {

                final Map<String, String> attrs = (StringUtils.isNotBlank(attributes))
                        ? this.jsonMapper.readValue(
                                attributes,
                                new TypeReference<Map<String, String>>() {
                                })
                        : new HashMap<>();
                attrs.put(API.EXAM_API_PING_INSTRUCTION_CONFIRM, String.valueOf(clientInstructionRecord.getId()));

                this.clientInstructionRecordMapper.updateByPrimaryKeySelective(
                        new ClientInstructionRecord(
                                clientInstructionRecord.getId(),
                                null,
                                null,
                                null,
                                this.jsonMapper.writeValueAsString(attrs),
                                null,
                                null));

                return this.clientInstructionRecordMapper
                        .selectByPrimaryKey(clientInstructionRecord.getId());
            }

            return clientInstructionRecord;
        });
    }

}
