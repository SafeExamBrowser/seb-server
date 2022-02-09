/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification.NotificationType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtensionMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtensionMapper.ConnectionEventJoinRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientNotificationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientNotificationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientNotificationRecord;
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
    private final ClientEventExtensionMapper clientEventExtensionMapper;
    private final ClientNotificationRecordMapper clientNotificationRecordMapper;

    protected ClientEventDAOImpl(
            final ClientEventRecordMapper clientEventRecordMapper,
            final ClientEventExtensionMapper clientEventExtensionMapper,
            final ClientNotificationRecordMapper clientNotificationRecordMapper) {

        this.clientEventRecordMapper = clientEventRecordMapper;
        this.clientEventExtensionMapper = clientEventExtensionMapper;
        this.clientNotificationRecordMapper = clientNotificationRecordMapper;
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

        return Result.tryCatch(() -> this.clientEventRecordMapper
                .selectByExample()
                .leftJoin(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientConnectionRecordDynamicSqlSupport.id,
                        equalTo(ClientEventRecordDynamicSqlSupport.clientConnectionId))
                .where(
                        ClientConnectionRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getClientEventExamId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examUserSessionId,
                        SqlBuilder.isLikeWhenPresent(filterMap.getSQLWildcard(ClientConnection.FILTER_ATTR_SESSION_ID)))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientConnectionId,
                        isEqualToWhenPresent(filterMap.getClientEventConnectionId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        isEqualToWhenPresent(filterMap.getClientEventTypeId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        SqlBuilder.isNotEqualTo(EventType.REMOVED_EVENT_TYPE_LAST_PING))
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
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExtendedClientEvent>> allMatchingExtended(
            final FilterMap filterMap,
            final Predicate<ExtendedClientEvent> predicate) {

        return Result.tryCatch(() -> this.clientEventExtensionMapper.selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getClientEventExamId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examUserSessionId,
                        SqlBuilder.isLikeWhenPresent(filterMap.getSQLWildcard(ClientConnection.FILTER_ATTR_SESSION_ID)))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientConnectionId,
                        isEqualToWhenPresent(filterMap.getClientEventConnectionId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        isEqualToWhenPresent(filterMap.getClientEventTypeId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        SqlBuilder.isNotEqualTo(EventType.REMOVED_EVENT_TYPE_LAST_PING))
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

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

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
    @Transactional(readOnly = true)
    public Result<ClientNotification> getPendingNotification(final Long notificationId) {
        return Result.tryCatch(() -> this.clientNotificationRecordMapper
                .selectByPrimaryKey(notificationId))
                .flatMap(ClientEventDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Set<EntityKey>> getNotificationIdsForExam(final Long examId) {
        return Result.tryCatch(() -> this.clientNotificationRecordMapper
                .selectByExample()
                .leftJoin(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientConnectionRecordDynamicSqlSupport.id,
                        equalTo(ClientNotificationRecordDynamicSqlSupport.clientConnectionId))
                .where(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(examId))
                .build()
                .execute()
                .stream()
                .map(ClientNotificationRecord::getClientConnectionId)
                .map(id -> new EntityKey(id, EntityType.CLIENT_NOTIFICATION))
                .collect(Collectors.toSet()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientNotification> getPendingNotificationByValue(
            final Long clientConnectionId,
            final Long notificationValueId) {

        return Result.tryCatch(() -> {

            final List<ClientNotificationRecord> records = this.clientNotificationRecordMapper
                    .selectByExample()
                    .where(ClientNotificationRecordDynamicSqlSupport.clientConnectionId, isEqualTo(clientConnectionId))
                    .and(ClientNotificationRecordDynamicSqlSupport.eventType, isEqualTo(EventType.NOTIFICATION.id))
                    .and(ClientNotificationRecordDynamicSqlSupport.value, isEqualTo(notificationValueId))
                    .build()
                    .execute();

            if (log.isDebugEnabled()) {
                log.debug("Found notification for clientConnectionId: {} notification: {}",
                        clientConnectionId,
                        records);
            }

            if (records.isEmpty() || records.size() > 1) {
                throw new IllegalStateException(
                        "Failed to find pending notification event for confirm:" + notificationValueId);
            }

            return records.get(0);
        })
                .flatMap(ClientEventDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ClientNotification>> getPendingNotifications(final Long clientConnectionId) {
        return Result.tryCatch(() -> this.clientNotificationRecordMapper
                .selectByExample()
                .where(ClientNotificationRecordDynamicSqlSupport.clientConnectionId, isEqualTo(clientConnectionId))
                .and(ClientNotificationRecordDynamicSqlSupport.eventType, isEqualTo(EventType.NOTIFICATION.id))
                .build()
                .execute()
                .stream()
                .map(ClientEventDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Set<Long>> getClientConnectionIdsWithPendingNotification(final Long examId) {
        return Result.tryCatch(() -> this.clientNotificationRecordMapper
                .selectByExample()
                .leftJoin(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientConnectionRecordDynamicSqlSupport.id,
                        equalTo(ClientNotificationRecordDynamicSqlSupport.clientConnectionId))
                .where(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(examId))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        isIn(ConnectionStatus.ACTIVE.name(), ConnectionStatus.CONNECTION_REQUESTED.name()))
                .and(
                        ClientNotificationRecordDynamicSqlSupport.eventType,
                        isEqualTo(EventType.NOTIFICATION.id))
                .build()
                .execute()
                .stream()
                .map(ClientNotificationRecord::getClientConnectionId)
                .collect(Collectors.toSet()));
    }

    @Override
    @Transactional
    public Result<ClientNotification> confirmPendingNotification(final Long notificationId) {

        return Result.tryCatch(() -> {
            final Long pk = this.clientNotificationRecordMapper
                    .selectIdsByExample()
                    .where(ClientNotificationRecordDynamicSqlSupport.id, isEqualTo(notificationId))
                    .and(ClientNotificationRecordDynamicSqlSupport.eventType, isEqualTo(EventType.NOTIFICATION.id))
                    .build()
                    .execute()
                    .stream().collect(Utils.toSingleton());

            this.clientNotificationRecordMapper.updateByPrimaryKeySelective(new ClientNotificationRecord(
                    pk,
                    null,
                    EventType.NOTIFICATION_CONFIRMED.id,
                    null, null, null));

            return this.clientNotificationRecordMapper.selectByPrimaryKey(pk);
        })
                .flatMap(ClientEventDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
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
    public Result<ClientNotification> createNewNotification(final ClientNotification notification) {
        return Result.tryCatch(() -> {

            final EventType eventType = notification.getEventType();
            final NotificationType notificationType = notification.getNotificationType();

            final ClientNotificationRecord newRecord = new ClientNotificationRecord(
                    null,
                    notification.connectionId,
                    eventType != null ? eventType.id : EventType.UNKNOWN.id,
                    notificationType != null ? notificationType.id : NotificationType.UNKNOWN.id,
                    notification.numValue != null ? notification.numValue.longValue() : null,
                    notification.text);

            this.clientNotificationRecordMapper.insertSelective(newRecord);
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
        return Result.tryCatch(() -> {

            if (all == null || all.isEmpty()) {
                return Collections.emptyList();
            }

            final List<Long> pks = all
                    .stream()
                    .map(EntityKey::getModelId)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            this.clientEventRecordMapper
                    .deleteByExample()
                    .where(ClientEventRecordDynamicSqlSupport.id, isIn(pks))
                    .build()
                    .execute();

            return pks
                    .stream()
                    .map(pk -> new EntityKey(String.valueOf(pk), EntityType.CLIENT_EVENT))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteClientNotification(final Set<EntityKey> keys) {
        return Result.tryCatch(() -> {

            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }

            final List<Long> pks = keys
                    .stream()
                    .map(EntityKey::getModelId)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Going to delete all client notifications: {}", pks);
            }

            this.clientNotificationRecordMapper
                    .deleteByExample()
                    .where(ClientNotificationRecordDynamicSqlSupport.id, isIn(pks))
                    .build()
                    .execute();

            return pks
                    .stream()
                    .map(pk -> new EntityKey(String.valueOf(pk), EntityType.CLIENT_NOTIFICATION))
                    .collect(Collectors.toList());
        });
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
                    record.getClientConnectionId(),
                    (type != null) ? EventType.byId(type) : EventType.UNKNOWN,
                    record.getClientTime(),
                    record.getServerTime(),
                    (numericValue != null) ? numericValue.doubleValue() : null,
                    record.getText());
        });
    }

    private static Result<ClientNotification> toDomainModel(final ClientNotificationRecord record) {
        return Result.tryCatch(() -> {

            final Long value = record.getValue();
            final NotificationType notificationType = (record.getNotificationType() != null)
                    ? NotificationType.byId(record.getNotificationType())
                    : NotificationType.UNKNOWN;

            return new ClientNotification(
                    record.getId(),
                    record.getClientConnectionId(),
                    (record.getEventType() != null) ? EventType.byId(record.getEventType()) : EventType.UNKNOWN,
                    null,
                    null,
                    (value != null) ? value.doubleValue() : null,
                    record.getText(),
                    notificationType);
        });
    }

    private static Result<ExtendedClientEvent> toDomainModelExtended(final ConnectionEventJoinRecord record) {
        return Result.tryCatch(() -> new ExtendedClientEvent(
                record.institution_id,
                record.exam_id,
                record.exam_user_session_identifier,
                record.id,
                record.connection_id,
                (record.type != null) ? EventType.byId(record.type) : EventType.UNKNOWN,
                record.client_time,
                record.server_time,
                record.numeric_value,
                record.text));
    }

}
