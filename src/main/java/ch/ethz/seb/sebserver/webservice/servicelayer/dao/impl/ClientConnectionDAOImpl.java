/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientNotificationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientNotificationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import io.micrometer.core.instrument.util.StringUtils;

@Lazy
@Component
@WebServiceProfile
public class ClientConnectionDAOImpl implements ClientConnectionDAO {

    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final ClientEventRecordMapper clientEventRecordMapper;
    private final ClientInstructionRecordMapper clientInstructionRecordMapper;
    private final ClientIndicatorRecordMapper clientIndicatorRecordMapper;
    private final ClientNotificationRecordMapper clientNotificationRecordMapper;

    protected ClientConnectionDAOImpl(
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final ClientEventRecordMapper clientEventRecordMapper,
            final ClientInstructionRecordMapper clientInstructionRecordMapper,
            final ClientIndicatorRecordMapper clientIndicatorRecordMapper,
            final ClientNotificationRecordMapper clientNotificationRecordMapper) {

        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
        this.clientEventRecordMapper = clientEventRecordMapper;
        this.clientInstructionRecordMapper = clientInstructionRecordMapper;
        this.clientIndicatorRecordMapper = clientIndicatorRecordMapper;
        this.clientNotificationRecordMapper = clientNotificationRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_CONNECTION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientConnection> byPK(final Long id) {
        return recordById(id)
                .flatMap(ClientConnectionDAOImpl::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> allMatching(
            final FilterMap filterMap,
            final Predicate<ClientConnection> predicate) {

        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientConnectionRecord>>>.QueryExpressionWhereBuilder whereClause =
                    (filterMap.getBoolean(FilterMap.ATTR_ADD_INSITUTION_JOIN))
                            ? this.clientConnectionRecordMapper
                                    .selectByExample()
                                    .join(InstitutionRecordDynamicSqlSupport.institutionRecord)
                                    .on(InstitutionRecordDynamicSqlSupport.id,
                                            SqlBuilder.equalTo(ClientConnectionRecordDynamicSqlSupport.institutionId))
                                    .where(
                                            ClientConnectionRecordDynamicSqlSupport.institutionId,
                                            isEqualToWhenPresent(filterMap.getInstitutionId()))

                            : this.clientConnectionRecordMapper
                                    .selectByExample()
                                    .where(
                                            ClientConnectionRecordDynamicSqlSupport.institutionId,
                                            isEqualToWhenPresent(filterMap.getInstitutionId()));
            return whereClause
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.examId,
                            isEqualToWhenPresent(filterMap.getClientConnectionExamId()))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.status,
                            isEqualToWhenPresent(filterMap.getClientConnectionStatus()))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.examUserSessionId,
                            isLikeWhenPresent(filterMap.getClientConnectionUserId()))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.clientAddress,
                            isLikeWhenPresent(filterMap.getClientConnectionIPAddress()))
                    .build()
                    .execute()
                    .stream()
                    .map(ClientConnectionDAOImpl::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> allOf(final Set<Long> pks) {
        if (pks == null || pks.isEmpty()) {
            return Result.of(Collections.emptyList());
        }
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.id, SqlBuilder.isIn(new ArrayList<>(pks)))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getConnectionTokens(final Long examId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionRecord::getConnectionToken)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getActiveConnctionTokens(final Long examId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualTo(ConnectionStatus.ACTIVE.name()))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionRecord::getConnectionToken)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<Collection<ClientConnectionRecord>> getAllConnectionIdsForRoomUpdateActive() {
        return Result.<Collection<ClientConnectionRecord>> tryCatch(() -> {

            final Collection<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomUpdate, isNotEqualTo(0))
                    .and(ClientConnectionRecordDynamicSqlSupport.status, isEqualTo(ConnectionStatus.ACTIVE.name()))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            final List<Long> ids = records
                    .stream()
                    .map(rec -> rec.getId())
                    .collect(Collectors.toList());

            final ClientConnectionRecord updateRecord = createProctoringRoomUpdateRecord(0);
            this.clientConnectionRecordMapper.updateByExampleSelective(updateRecord)
                    .where(ClientConnectionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return records;
        })
                .onError(TransactionHandler::rollback);
    }

    private ClientConnectionRecord createProctoringRoomUpdateRecord(final int remoteProctoringRoomUpdate) {
        final ClientConnectionRecord updateRecord = new ClientConnectionRecord(
                null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                remoteProctoringRoomUpdate,
                null, null, null);
        return updateRecord;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getInactiveConnctionTokens(final Set<String> connectionTokens) {
        return Result.tryCatch(() -> {
            if (connectionTokens == null || connectionTokens.isEmpty()) {
                return Collections.emptyList();
            }
            return this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.connectionToken,
                            SqlBuilder.isIn(new ArrayList<>(connectionTokens)))
                    .and(ClientConnectionRecordDynamicSqlSupport.status, isNotEqualTo(ConnectionStatus.ACTIVE.name()))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.status,
                            isNotEqualTo(ConnectionStatus.AUTHENTICATED.name()))
                    .and(ClientConnectionRecordDynamicSqlSupport.status,
                            isNotEqualTo(ConnectionStatus.CONNECTION_REQUESTED.name()))
                    .build()
                    .execute()
                    .stream()
                    .map(r -> r.getConnectionToken())
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<Collection<ClientConnectionRecord>> getAllConnectionIdsForRoomUpdateInactive() {
        return Result.<Collection<ClientConnectionRecord>> tryCatch(() -> {
            final Collection<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomUpdate, isNotEqualTo(0))
                    .and(ClientConnectionRecordDynamicSqlSupport.status, isNotEqualTo(ConnectionStatus.ACTIVE.name()))
                    .build()
                    .execute();

            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            final List<Long> ids = records
                    .stream()
                    .map(rec -> rec.getId())
                    .collect(Collectors.toList());

            final ClientConnectionRecord updateRecord = createProctoringRoomUpdateRecord(0);
            this.clientConnectionRecordMapper.updateByExampleSelective(updateRecord)
                    .where(ClientConnectionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return records;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> getRoomConnections(final Long roomId) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomId, isEqualTo(roomId))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientConnection>> getCollectingRoomConnections(final Long examId, final String roomName) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .leftJoin(RemoteProctoringRoomRecordDynamicSqlSupport.remoteProctoringRoomRecord)
                .on(RemoteProctoringRoomRecordDynamicSqlSupport.id,
                        SqlBuilder.equalTo(ClientConnectionRecordDynamicSqlSupport.remoteProctoringRoomId))
                .where(ClientConnectionRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .and(RemoteProctoringRoomRecordDynamicSqlSupport.name, SqlBuilder.isLike(roomName))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionDAOImpl::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ClientConnection> createNew(final ClientConnection data) {
        return Result.tryCatch(() -> {

            final long millisecondsNow = Utils.getMillisecondsNow();
            final ClientConnectionRecord newRecord = new ClientConnectionRecord(
                    null,
                    data.institutionId,
                    data.examId,
                    ConnectionStatus.CONNECTION_REQUESTED.name(),
                    data.connectionToken,
                    null,
                    data.clientAddress,
                    data.virtualClientId,
                    BooleanUtils.toInteger(data.vdi, 1, 0, 0),
                    data.vdiPairToken,
                    millisecondsNow,
                    millisecondsNow,
                    data.remoteProctoringRoomId,
                    null,
                    Utils.truncateText(data.sebMachineName, 255),
                    Utils.truncateText(data.sebOSName, 255),
                    Utils.truncateText(data.sebVersion, 255));

            this.clientConnectionRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(ClientConnectionDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ClientConnection> save(final ClientConnection data) {
        return Result.tryCatch(() -> {

            final long millisecondsNow = Utils.getMillisecondsNow();
            final ClientConnectionRecord updateRecord = new ClientConnectionRecord(
                    data.id,
                    null,
                    data.examId,
                    data.status != null ? data.status.name() : null,
                    null,
                    data.userSessionId,
                    data.clientAddress,
                    data.virtualClientId,
                    BooleanUtils.toInteger(data.vdi, 1, 0, 0),
                    data.vdiPairToken,
                    null,
                    millisecondsNow,
                    null,
                    null,
                    Utils.truncateText(data.sebMachineName, 255),
                    Utils.truncateText(data.sebOSName, 255),
                    Utils.truncateText(data.sebVersion, 255));

            this.clientConnectionRecordMapper.updateByPrimaryKeySelective(updateRecord);
            return this.clientConnectionRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(ClientConnectionDAOImpl::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Void> assignToProctoringRoom(
            final Long connectionId,
            final String connectionToken,
            final Long roomId) {

        return Result.tryCatch(() -> {
            this.clientConnectionRecordMapper.updateByPrimaryKeySelective(new ClientConnectionRecord(
                    connectionId,
                    null, null, null, null, null,
                    null, null, null, null, null, null,
                    roomId,
                    0, null, null, null));
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Void> markForProctoringUpdate(final Long id) {
        return Result.tryCatch(() -> {
            this.clientConnectionRecordMapper.updateByPrimaryKeySelective(new ClientConnectionRecord(
                    id,
                    null, null, null, null, null,
                    null, null, null, null, null, null,
                    null,
                    1, null, null, null));
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Void> removeFromProctoringRoom(final Long connectionId, final String connectionToken) {
        return Result.tryCatch(() -> {
            final ClientConnectionRecord record = this.clientConnectionRecordMapper.selectByPrimaryKey(connectionId);
            if (record != null) {
                this.clientConnectionRecordMapper.updateByPrimaryKey(new ClientConnectionRecord(
                        connectionId,
                        record.getInstitutionId(),
                        record.getExamId(),
                        record.getStatus(),
                        record.getConnectionToken(),
                        record.getExamUserSessionId(),
                        record.getClientAddress(),
                        record.getVirtualClientAddress(),
                        record.getVdi(),
                        record.getVdiPairToken(),
                        record.getCreationTime(),
                        record.getUpdateTime(),
                        null,
                        0,
                        record.getClientMachineName(),
                        record.getClientOsName(),
                        record.getClientVersion()));
            } else {
                throw new ResourceNotFoundException(EntityType.CLIENT_CONNECTION, String.valueOf(connectionId));
            }
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only for deletion
        if (bulkAction.type == BulkActionType.ACTIVATE || bulkAction.type == BulkActionType.DEACTIVATE) {
            return Collections.emptySet();
        }
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.CLIENT_CONNECTION)) {
            return Collections.emptySet();
        }

        // define the select function in case of source type
        Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction;
        switch (bulkAction.sourceType) {
            case INSTITUTION:
                selectionFunction = this::allIdsOfInstitution;
                break;
            case LMS_SETUP:
                selectionFunction = this::allIdsOfLmsSetup;
                break;
            case USER:
                selectionFunction = this::allIdsOfUser;
                break;
            case EXAM:
                selectionFunction = this::allIdsOfExam;
                break;
            default:
                selectionFunction = key -> Result.of(Collections.emptyList()); //empty select function
                break;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.<Collection<EntityKey>> tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // delete all related client indicators
            this.clientIndicatorRecordMapper.deleteByExample()
                    .where(
                            ClientIndicatorRecordDynamicSqlSupport.clientConnectionId,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            // delete all related client events
            this.clientEventRecordMapper.deleteByExample()
                    .where(
                            ClientEventRecordDynamicSqlSupport.clientConnectionId,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            // delete all related client notifications
            this.clientNotificationRecordMapper.deleteByExample()
                    .where(
                            ClientNotificationRecordDynamicSqlSupport.clientConnectionId,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            // then delete all related client instructions
            final List<String> connectionTokens = this.clientConnectionRecordMapper.selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.id,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(r -> r.getConnectionToken())
                    .collect(Collectors.toList());

            if (connectionTokens != null && !connectionTokens.isEmpty()) {

                this.clientInstructionRecordMapper.deleteByExample()
                        .where(
                                ClientInstructionRecordDynamicSqlSupport.connectionToken,
                                SqlBuilder.isIn(connectionTokens))
                        .build()
                        .execute();
            }

            // then delete all requested client-connections
            this.clientConnectionRecordMapper.deleteByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.id,
                            SqlBuilder.isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CLIENT_CONNECTION))
                    .collect(Collectors.toList());
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientConnection> byConnectionToken(final String connectionToken) {
        return Result.tryCatch(() -> {
            final List<ClientConnectionRecord> list = this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.connectionToken,
                            SqlBuilder.isEqualTo(connectionToken))

                    .build()
                    .execute();

            if (list.isEmpty()) {
                throw new ResourceNotFoundException(EntityType.CLIENT_CONNECTION, "connectionToken");
            }

            if (list.size() > 1) {
                throw new IllegalStateException("Only one ClientConnection expected but there are: " + list.size());
            }

            return list.get(0);
        })
                .flatMap(ClientConnectionDAOImpl::toDomainModel);
    }

    @Override
    public Result<Boolean> isInInstructionStatus(final Long examId, final String connectionToken) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.connectionToken,
                        SqlBuilder.isEqualTo(connectionToken))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualTo(ConnectionStatus.ACTIVE.name()))
                .or(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualTo(ConnectionStatus.CONNECTION_REQUESTED.name()))
                .build()
                .execute()
                .stream()
                .findFirst()
                .isPresent());
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> isUpToDate(final ClientConnection clientConnection) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .countByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.connectionToken,
                        SqlBuilder.isEqualTo(clientConnection.connectionToken))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.updateTime,
                        SqlBuilder.isEqualTo(clientConnection.updateTime))
                .build()
                .execute() > 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Set<String>> getClientConnectionsOutOfSyc(final Long examId, final Set<Long> timestamps) {
        return Result.tryCatch(() -> {
            final Set<String> result = new HashSet<>();

            this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.examId,
                            SqlBuilder.isEqualTo(examId))
                    .build()
                    .execute()
                    .stream()
                    .forEach(cc -> {
                        if (!timestamps.contains(cc.getUpdateTime())) {
                            result.add(cc.getConnectionToken());
                        }
                    });

            return result;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Set<String>> filterForInstructionStatus(final Long examId, final Set<String> connectionToken) {
        if (connectionToken == null || connectionToken.isEmpty()) {
            return Result.ofRuntimeError("Null or empty set reference");
        }
        return Result.tryCatch(() -> this.clientConnectionRecordMapper
                .selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.connectionToken,
                        SqlBuilder.isIn(new ArrayList<>(connectionToken)))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualTo(ConnectionStatus.ACTIVE.name()))
                .or(
                        ClientConnectionRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualTo(ConnectionStatus.CONNECTION_REQUESTED.name()))
                .build()
                .execute()
                .stream()
                .map(ClientConnectionRecord::getConnectionToken)
                .collect(Collectors.toSet()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientConnectionRecord> getVDIPairCompanion(
            final Long examId,
            final String clientName) {

        return Result.tryCatch(() -> {
            final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                    .selectByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.examId,
                            SqlBuilder.isEqualTo(examId))
                    .and(
                            ClientConnectionRecordDynamicSqlSupport.virtualClientAddress,
                            SqlBuilder.isEqualTo(clientName))
                    .build()
                    .execute();

            if (records == null || records.isEmpty() || records.size() > 1) {
                throw new ResourceNotFoundException(EntityType.CLIENT_CONNECTION, clientName);
            }

            return records.get(0);
        });
    }

    @Override
    @Transactional
    public Result<Exam> deleteClientIndicatorValues(final Exam exam) {
        return Result.tryCatch(() -> {

            final List<Long> clientConnections = this.clientConnectionRecordMapper.selectIdsByExample()
                    .where(
                            ClientConnectionRecordDynamicSqlSupport.examId,
                            SqlBuilder.isEqualTo(exam.id))
                    .build()
                    .execute();

            if (clientConnections == null || clientConnections.isEmpty()) {
                return exam;
            }

            this.clientIndicatorRecordMapper.deleteByExample()
                    .where(
                            ClientIndicatorRecordDynamicSqlSupport.clientConnectionId,
                            SqlBuilder.isIn(clientConnections))
                    .build()
                    .execute();

            return exam;
        });
    }

    private Result<ClientConnectionRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {

            final ClientConnectionRecord record = this.clientConnectionRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        entityType(),
                        String.valueOf(id));
            }

            return record;
        });
    }

    public static Result<ClientConnection> toDomainModel(final ClientConnectionRecord record) {
        return Result.tryCatch(() -> {

            final String status = record.getStatus();
            return new ClientConnection(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getExamId(),
                    (StringUtils.isNotBlank(status))
                            ? ConnectionStatus.valueOf(status)
                            : ConnectionStatus.UNDEFINED,
                    record.getConnectionToken(),
                    record.getExamUserSessionId(),
                    record.getClientAddress(),
                    record.getClientOsName(),
                    record.getClientMachineName(),
                    record.getClientVersion(),
                    record.getVirtualClientAddress(),
                    BooleanUtils.toBooleanObject(record.getVdi()),
                    record.getVdiPairToken(),
                    record.getCreationTime(),
                    record.getUpdateTime(),
                    record.getRemoteProctoringRoomId(),
                    BooleanUtils.toBooleanObject(record.getRemoteProctoringRoomUpdate()));
        });
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.parseLong(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_CONNECTION),
                        getDependencyName(rec),
                        rec.getClientAddress()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ClientConnectionRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.lmsSetupId,
                        isEqualTo(Long.parseLong(lmsSetupKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        lmsSetupKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_CONNECTION),
                        getDependencyName(rec),
                        rec.getClientAddress()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ClientConnectionRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.owner,
                        isEqualTo(userKey.modelId))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        userKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_CONNECTION),
                        getDependencyName(rec),
                        rec.getClientAddress()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfExam(final EntityKey examKey) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByExample()
                .where(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualTo(Long.parseLong(examKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        examKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_CONNECTION),
                        getDependencyName(rec),
                        rec.getClientAddress()))
                .collect(Collectors.toList()));
    }

    private String getDependencyName(final ClientConnectionRecord record) {
        final String examUserSessionId = record.getExamUserSessionId();
        if (StringUtils.isNotBlank(examUserSessionId)) {
            return examUserSessionId;
        }

        return record.getConnectionToken();
    }

}
