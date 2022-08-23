/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientGroupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientGroupRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientGroupRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ClientGroupDAOImpl implements ClientGroupDAO {

    private final ClientGroupRecordMapper clientGroupRecordMapper;

    public ClientGroupDAOImpl(final ClientGroupRecordMapper clientGroupRecordMapper) {
        this.clientGroupRecordMapper = clientGroupRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_GROUP;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ClientGroup> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientGroup>> allMatching(
            final FilterMap filterMap,
            final Predicate<ClientGroup> predicate) {

        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        SqlBuilder.equalTo(ClientGroupRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ClientGroupRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getLong(ClientGroup.FILTER_ATTR_EXAM_ID)))
                .and(
                        ClientGroupRecordDynamicSqlSupport.name,
                        isLikeWhenPresent(filterMap.getString(ClientGroup.FILTER_ATTR_NAME)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientGroup>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.clientGroupRecordMapper
                    .selectByExample()
                    .where(ClientGroupRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<ClientGroup> createNew(final ClientGroup data) {
        return Result.tryCatch(() -> {

            final ClientGroupRecord newRecord = new ClientGroupRecord(
                    null,
                    data.examId,
                    data.name,
                    data.type.name(),
                    data.color,
                    data.icon,
                    data.data);

            this.clientGroupRecordMapper.insert(newRecord);

            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ClientGroup> save(final ClientGroup data) {
        return Result.tryCatch(() -> {

            final ClientGroupRecord newRecord = new ClientGroupRecord(
                    data.id,
                    data.examId,
                    data.name,
                    data.type.name(),
                    data.color,
                    data.icon,
                    data.data);

            this.clientGroupRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.clientGroupRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // then delete all client groups
            this.clientGroupRecordMapper
                    .deleteByExample()
                    .where(ClientGroupRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CLIENT_GROUP))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ClientGroup>> allForExam(final Long examId) {
        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .where(ClientGroupRecordDynamicSqlSupport.examId, isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only for deletion
        if (bulkAction.type == BulkActionType.ACTIVATE || bulkAction.type == BulkActionType.DEACTIVATE) {
            return Collections.emptySet();
        }
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.CLIENT_GROUP)) {
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

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ClientGroupRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.parseLong(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_GROUP),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ClientGroupRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.lmsSetupId,
                        isEqualTo(Long.parseLong(lmsSetupKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        lmsSetupKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_GROUP),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ClientGroupRecordDynamicSqlSupport.examId))
                .where(
                        ExamRecordDynamicSqlSupport.owner,
                        isEqualTo(userKey.modelId))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        userKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_GROUP),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfExam(final EntityKey examKey) {
        return Result.tryCatch(() -> this.clientGroupRecordMapper
                .selectByExample()
                .where(
                        ClientGroupRecordDynamicSqlSupport.examId,
                        isEqualTo(Long.parseLong(examKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        examKey,
                        new EntityKey(rec.getId(), EntityType.CLIENT_GROUP),
                        rec.getName(),
                        rec.getType()))
                .collect(Collectors.toList()));
    }

    private Result<ClientGroupRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {

            final ClientGroupRecord record = this.clientGroupRecordMapper
                    .selectByPrimaryKey(id);

            if (record == null) {
                throw new ResourceNotFoundException(
                        entityType(),
                        String.valueOf(id));
            }

            return record;
        });
    }

    private Result<ClientGroup> toDomainModel(final ClientGroupRecord record) {
        return Result.tryCatch(() -> {
            return new ClientGroup(
                    record.getId(),
                    record.getExamId(),
                    record.getName(),
                    ClientGroupType.valueOf(record.getType()),
                    record.getColor(),
                    record.getIcon(),
                    record.getData());
        });
    }

}
