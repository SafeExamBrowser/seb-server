/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.BatchActionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.BatchActionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.BatchActionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.BatchActionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class BatchActionDAOImpl implements BatchActionDAO {

    private static final long ABANDONED_BATCH_TIME = Constants.MINUTE_IN_MILLIS * 10;

    private final BatchActionRecordMapper batchActionRecordMapper;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final JSONMapper jsonMapper;

    public BatchActionDAOImpl(
            final BatchActionRecordMapper batchActionRecordMapper,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final JSONMapper jsonMapper) {

        this.batchActionRecordMapper = batchActionRecordMapper;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.BATCH_ACTION;
    }

    @Override
    @Transactional
    public Result<BatchAction> getAndReserveNext(final String processId) {
        return Result.tryCatch(() -> {

            final Long oldThreshold = Utils.getMillisecondsNow() - ABANDONED_BATCH_TIME;
            final BatchActionRecord nextRec = this.batchActionRecordMapper.selectByExample()
                    .where(BatchActionRecordDynamicSqlSupport.lastUpdate, isNull())
                    .or(BatchActionRecordDynamicSqlSupport.processorId, isNotLike("%" + BatchAction.FINISHED_FLAG))
                    .build()
                    .execute()
                    .stream()
                    .filter(rec -> rec.getLastUpdate() == null || rec.getLastUpdate() < oldThreshold)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            EntityType.BATCH_ACTION,
                            processId));

            final BatchActionRecord newRecord = new BatchActionRecord(
                    nextRec.getId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Utils.getMillisecondsNow(),
                    processId);

            this.batchActionRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.batchActionRecordMapper.selectByPrimaryKey(nextRec.getId());
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public void setSuccessfull(final Long actionId, final String processId, final String modelId) {
        try {

            final BatchActionRecord rec = this.batchActionRecordMapper.selectByPrimaryKey(actionId);

            if (!processId.equals(rec.getProcessorId())) {
                throw new RuntimeException("Batch action processor id mismatch: " + processId + " " + rec);
            }

            String successful = rec.getSuccessful();
            if (StringUtils.isBlank(successful)) {
                successful = modelId;
            } else {
                final Set<String> ids = new HashSet<>(Arrays.asList(StringUtils.split(
                        successful,
                        Constants.LIST_SEPARATOR)));
                ids.add(modelId);
                successful = StringUtils.join(ids, Constants.LIST_SEPARATOR);
            }

            final BatchActionRecord newRecord = new BatchActionRecord(
                    actionId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    successful,
                    Utils.getMillisecondsNow(),
                    processId);
            this.batchActionRecordMapper.updateByPrimaryKeySelective(newRecord);

        } catch (final Exception e) {
            log.error("Failed to mark entity successfully processed: modelId: {}, processId", modelId, e);
        }
    }

    @Override
    @Transactional
    public void setFailure(final Long actionId, final String processId, final String modelId, final Exception error) {
        try {
            String apiMessage = null;
            if (error instanceof APIMessageException) {
                apiMessage = this.jsonMapper.writeValueAsString(((APIMessageException) error).getMainMessage());
            } else {
                apiMessage = this.jsonMapper.writeValueAsString(APIMessage.ErrorMessage.UNEXPECTED.of(error));
            }

            this.additionalAttributesDAO
                    .saveAdditionalAttribute(EntityType.BATCH_ACTION, actionId, modelId, apiMessage)
                    .onError(err -> log.error("Failed to store batch action failure: actionId: {}, modelId: {}",
                            actionId,
                            modelId,
                            err));

        } catch (final Exception e) {
            log.error("Unexpected error while trying to persist batch action error: ", e);
        }
    }

    @Override
    @Transactional
    public Result<BatchAction> finishUp(final Long actionId, final String processId, final boolean force) {
        return Result.tryCatch(() -> {

            final BatchActionRecord rec = this.batchActionRecordMapper.selectByPrimaryKey(actionId);

            if (!processId.equals(rec.getProcessorId())) {
                throw new RuntimeException("Batch action processor id mismatch: " + processId + " " + rec);
            }

            if (!force) {
                // apply consistency check
                final Set<String> ids = new HashSet<>(Arrays.asList(StringUtils.split(
                        rec.getSourceIds(),
                        Constants.LIST_SEPARATOR)));

                // get all succeeded
                final String successful = rec.getSuccessful();
                final Set<String> success = StringUtils.isNotBlank(successful)
                        ? new HashSet<>(Arrays.asList(StringUtils.split(
                                successful,
                                Constants.LIST_SEPARATOR)))
                        : Collections.emptySet();

                // get all failed
                final Collection<AdditionalAttributeRecord> failed = this.additionalAttributesDAO
                        .getAdditionalAttributes(EntityType.BATCH_ACTION, actionId)
                        .getOrThrow();

                if (ids.size() != success.size() + failed.size()) {
                    throw new IllegalStateException(
                            "Processing ids mismatch source: " + ids + " success: " + success + " failed: " + failed);
                }
            }

            final BatchActionRecord newRecord = new BatchActionRecord(
                    actionId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Utils.getMillisecondsNow(),
                    processId + BatchAction.FINISHED_FLAG);

            this.batchActionRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.batchActionRecordMapper.selectByPrimaryKey(actionId);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<BatchAction> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModelWithFailures);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<BatchAction>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {
            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.batchActionRecordMapper.selectByExample()
                    .where(BatchActionRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<BatchAction>> allMatching(
            final FilterMap filterMap,
            final Predicate<BatchAction> predicate) {

        return Result.tryCatch(() -> this.batchActionRecordMapper
                .selectByExample()
                .where(
                        BatchActionRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        BatchActionRecordDynamicSqlSupport.actionType,
                        SqlBuilder.isEqualToWhenPresent(
                                filterMap.getString(Domain.BATCH_ACTION.ATTR_ACTION_TYPE)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<BatchAction> createNew(final BatchAction data) {
        return Result.tryCatch(() -> {

            final BatchActionRecord newRecord = new BatchActionRecord(
                    null,
                    data.institutionId,
                    data.ownerId,
                    data.actionType.toString(),
                    data.attributes != null ? this.jsonMapper.writeValueAsString(data.attributes) : null,
                    StringUtils.join(data.sourceIds, Constants.LIST_SEPARATOR),
                    null, null, null);

            this.batchActionRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<BatchAction> save(final BatchAction data) {
        return Result.tryCatch(() -> {

            final BatchActionRecord newRecord = new BatchActionRecord(
                    data.id,
                    null,
                    null,
                    null,
                    null,
                    null,
                    StringUtils.join(data.successful, Constants.LIST_SEPARATOR),
                    data.getLastUpdate(),
                    data.processorId);

            this.batchActionRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.batchActionRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            if (ids.isEmpty()) {
                return Collections.emptyList();
            }

            // try delete all additional attributes first
            ids.stream().forEach(id -> {
                try {
                    this.additionalAttributesDAO.deleteAll(EntityType.BATCH_ACTION, id);
                } catch (final Exception e) {
                    log.error("Failed to delete additional attributes for batch action: {}", id, e);
                }
            });

            this.batchActionRecordMapper.deleteByExample()
                    .where(BatchActionRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.BATCH_ACTION))
                    .collect(Collectors.toList());
        });
    }

    private Result<BatchActionRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final BatchActionRecord record = this.batchActionRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.BATCH_ACTION,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<BatchAction> toDomainModelWithFailures(final BatchActionRecord record) {
        return toDomainModel(record, true);
    }

    private Result<BatchAction> toDomainModel(final BatchActionRecord record) {
        return toDomainModel(record, false);
    }

    private Result<BatchAction> toDomainModel(final BatchActionRecord record, final boolean withFailures) {
        final String successful = record.getSuccessful();

        Map<String, APIMessage> failures = Collections.emptyMap();
        try {
            failures = this.additionalAttributesDAO
                    .getAdditionalAttributes(EntityType.BATCH_ACTION, record.getId())
                    .getOrThrow()
                    .stream()
                    .collect(Collectors.toMap(this::toEntityKey, this::toFailureMessage));
        } catch (final Exception e) {
            log.error("Failed to get batch action failure messages", e);
        }

        final Map<String, APIMessage> failuresMap = failures;
        return Result.tryCatch(() -> new BatchAction(
                record.getId(),
                record.getInstitutionId(),
                record.getOwner(),
                BatchActionType.valueOf(record.getActionType()),
                Utils.jsonToMap(record.getAttributes(), this.jsonMapper),
                Arrays.asList(record.getSourceIds().split(Constants.LIST_SEPARATOR)),
                StringUtils.isNoneBlank(successful) ? Arrays.asList(successful.split(Constants.LIST_SEPARATOR)) : null,
                record.getLastUpdate(),
                record.getProcessorId(),
                failuresMap));
    }

    private String toEntityKey(final AdditionalAttributeRecord rec) {
        try {
            return rec.getName();
        } catch (final Exception e) {
            log.error("Failed to parse entity key for batch action failure: {}", e.getMessage());
            return "-1";
        }
    }

    private APIMessage toFailureMessage(final AdditionalAttributeRecord rec) {
        try {
            return this.jsonMapper.readValue(rec.getValue(), APIMessage.class);
        } catch (final Exception e) {
            log.error("Failed to parse APIMessage for batch action failure: {}", e.getMessage());
            return APIMessage.ErrorMessage.UNEXPECTED.of(e);
        }
    }

}
