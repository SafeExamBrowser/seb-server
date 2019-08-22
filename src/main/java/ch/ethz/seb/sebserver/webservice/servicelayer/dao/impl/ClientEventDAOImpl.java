/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualToWhenPresent;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper.ConnectionEventJoinRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ClientEventDAOImpl implements ClientEventDAO {

    private final ClientEventRecordMapper clientEventRecordMapper;
    private final ClientEventExtentionMapper clientEventExtentionMapper;

    protected ClientEventDAOImpl(
            final ClientEventRecordMapper clientEventRecordMapper,
            final ClientEventExtentionMapper clientEventExtentionMapper) {

        this.clientEventRecordMapper = clientEventRecordMapper;
        this.clientEventExtentionMapper = clientEventExtentionMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_EVENT;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientEvent> byPK(final Long id) {
        return recordById(id)
                .flatMap(ClientEventDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientEvent>> allMatching(
            final FilterMap filterMap,
            final Predicate<ClientEvent> predicate) {

        return Result.tryCatch(() -> {

            return this.clientEventRecordMapper
                    .selectByExample()
                    .where(
                            ClientEventRecordDynamicSqlSupport.connectionId,
                            isEqualToWhenPresent(filterMap.getClientEventConnectionId()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.type,
                            isEqualToWhenPresent(filterMap.getClientEventTypeId()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.type,
                            SqlBuilder.isNotEqualTo(EventType.LAST_PING.id))
                    .and(
                            ClientEventRecordDynamicSqlSupport.clientTime,
                            SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeFrom()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.clientTime,
                            SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeTo()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.serverTime,
                            SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeFrom()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.serverTime,
                            SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeTo()))
                    .and(
                            ClientEventRecordDynamicSqlSupport.text,
                            SqlBuilder.isLikeWhenPresent(filterMap.getClientEventText()))
                    .build()
                    .execute()
                    .stream()
                    .map(ClientEventDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Result<Collection<ExtendedClientEvent>> allMatchingExtended(
            final FilterMap filterMap,
            final Predicate<ExtendedClientEvent> predicate) {

        return Result.tryCatch(() -> this.clientEventExtentionMapper.selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getClientEventExamId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examUserSessionIdentifer,
                        SqlBuilder.isLikeWhenPresent(filterMap.getSQLWildcard(ClientConnection.FILTER_ATTR_SESSION_ID)))
                .and(
                        ClientEventRecordDynamicSqlSupport.connectionId,
                        isEqualToWhenPresent(filterMap.getClientEventConnectionId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        isEqualToWhenPresent(filterMap.getClientEventTypeId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        SqlBuilder.isNotEqualTo(EventType.LAST_PING.id))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientTime,
                        SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeFrom()))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientTime,
                        SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeTo()))
                .and(
                        ClientEventRecordDynamicSqlSupport.serverTime,
                        SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeFrom()))
                .and(
                        ClientEventRecordDynamicSqlSupport.serverTime,
                        SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeTo()))
                .and(
                        ClientEventRecordDynamicSqlSupport.text,
                        SqlBuilder.isLikeWhenPresent(filterMap.getClientEventText()))
                .build()
                .execute()
                .stream()
                .map(ClientEventDAOImpl::toDomainModelExtended)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientEvent>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {
            return this.clientEventRecordMapper.selectByExample()
                    .where(ClientEventRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(ClientEventDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<ClientEvent> createNew(final ClientEvent data) {
        return Result.tryCatch(() -> {

            final EventType eventType = data.getEventType();

            final ClientEventRecord newRecord = new ClientEventRecord(
                    null,
                    data.connectionId,
                    (eventType != null) ? eventType.id : EventType.UNKNOWN.id,
                    data.clientTime,
                    data.serverTime,
                    (data.numValue != null) ? new BigDecimal(data.numValue) : null,
                    data.text);

            this.clientEventRecordMapper.insertSelective(newRecord);
            return newRecord;
        })
                .flatMap(ClientEventDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ClientEvent> save(final ClientEvent data) {
        throw new UnsupportedOperationException("Update is not supported for client events");
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        throw new UnsupportedOperationException(
                "Delete is not supported for particular client events. "
                        + "Use delete of a client connection to delete also all client events of this connection.");
    }

    private Result<ClientEventRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {

            final ClientEventRecord record = this.clientEventRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        entityType(),
                        String.valueOf(id));
            }

            return record;
        });
    }

    private static Result<ClientEvent> toDomainModel(final ClientEventRecord record) {
        return Result.tryCatch(() -> {

            final Integer type = record.getType();
            final BigDecimal numericValue = record.getNumericValue();
            return new ClientEvent(
                    record.getId(),
                    record.getConnectionId(),
                    (type != null) ? EventType.byId(type) : EventType.UNKNOWN,
                    record.getClientTime(),
                    record.getServerTime(),
                    (numericValue != null) ? numericValue.doubleValue() : null,
                    record.getText());
        });
    }

    private static Result<ExtendedClientEvent> toDomainModelExtended(final ConnectionEventJoinRecord record) {
        return Result.tryCatch(() -> {

            return new ExtendedClientEvent(
                    record.institution_id,
                    record.exam_id,
                    record.exam_user_session_identifer,
                    record.id,
                    record.connection_id,
                    (record.type != null) ? EventType.byId(record.type) : EventType.UNKNOWN,
                    record.client_time,
                    record.server_time,
                    (record.numeric_value != null) ? record.numeric_value.doubleValue() : null,
                    record.text);
        });
    }

}
