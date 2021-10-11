/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.BatchActionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;

@Service
@WebServiceProfile
public class BatchActionServiceImpl implements BatchActionService {

    private static final Logger log = LoggerFactory.getLogger(BatchActionServiceImpl.class);

    private final BatchActionDAO batchActionDAO;
    private final TaskScheduler taskScheduler;
    private final EnumMap<BatchActionType, BatchActionExec> batchExecutions;

    public BatchActionServiceImpl(
            final BatchActionDAO batchActionDAO,
            final Collection<BatchActionExec> batchExecutions,
            final TaskScheduler taskScheduler) {

        this.batchActionDAO = batchActionDAO;
        this.taskScheduler = taskScheduler;

        this.batchExecutions = new EnumMap<>(BatchActionType.class);
        batchExecutions.stream()
                .map(exec -> this.batchExecutions.putIfAbsent(exec.actionType(), exec))
                .findAny()
                .ifPresent(exec -> log.error(
                        "BatchActionExec mismatch. It seems there is already a BatchActionExec for type: {} registered!",
                        exec.actionType()));
    }

    @Override
    public Result<BatchAction> registerNewBatchAction(
            final Long institutionId,
            final BatchActionType actionType,
            final String ids) {

        return Result.tryCatch(() -> {

            final Collection<String> sourceIds = Arrays.asList(StringUtils.split(
                    ids,
                    Constants.LIST_SEPARATOR));

            return this.batchActionDAO
                    .createNew(new BatchAction(null, institutionId, actionType, sourceIds, null, null, null))
                    .map(res -> {
                        processNextBatchAction();
                        return res;
                    })
                    .getOrThrow();
        });
    }

    @Override
    public Result<Collection<BatchAction>> getRunningActions(final Long institutionId) {
        return this.batchActionDAO.allMatching(new FilterMap().putIfAbsent(
                API.PARAM_INSTITUTION_ID,
                String.valueOf(institutionId)))
                .map(results -> results.stream()
                        .filter(action -> StringUtils.isNotBlank(action.processorId) &&
                                !action.processorId.endsWith(BatchActionDAO.FLAG_FINISHED))
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<BatchAction>> getRunningActions(final Long institutionId, final EntityType entityType) {
        return this.batchActionDAO.allMatching(new FilterMap().putIfAbsent(
                API.PARAM_INSTITUTION_ID,
                String.valueOf(institutionId)))
                .map(results -> results.stream()
                        .filter(action -> StringUtils.isNotBlank(action.processorId) &&
                                !action.processorId.endsWith(BatchActionDAO.FLAG_FINISHED))
                        .filter(action -> action.actionType.entityType == entityType)
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<BatchAction>> getFinishedActions(final Long institutionId) {
        return this.batchActionDAO.allMatching(new FilterMap().putIfAbsent(
                API.PARAM_INSTITUTION_ID,
                String.valueOf(institutionId)))
                .map(results -> results.stream()
                        .filter(action -> StringUtils.isNotBlank(action.processorId) &&
                                action.processorId.endsWith(BatchActionDAO.FLAG_FINISHED))
                        .collect(Collectors.toList()));
    }

    private void processNextBatchAction() {
        try {

            this.taskScheduler.schedule(
                    new BatchActionProcess(this.batchActionDAO, this.batchExecutions),
                    Instant.now());

        } catch (final Exception e) {
            log.error("Failed to schedule BatchActionProcess task: ", e);
        }
    }

    private final static class BatchActionProcess implements Runnable {

        private final BatchActionDAO batchActionDAO;
        private final EnumMap<BatchActionType, BatchActionExec> batchExecutions;
        private final String processorId = UUID.randomUUID().toString();

        private BatchAction batchAction;
        private Set<String> processingIds;
        private Set<String> failedIds;

        public BatchActionProcess(
                final BatchActionDAO batchActionDAO,
                final EnumMap<BatchActionType, BatchActionExec> batchExecutions) {

            this.batchActionDAO = batchActionDAO;
            this.batchExecutions = batchExecutions;
        }

        @Override
        public void run() {
            try {

                this.batchAction = this.batchActionDAO.getAndReserveNext(this.processorId)
                        .onErrorDo(error -> {
                            if (error instanceof ResourceNotFoundException) {
                                log.info("No batch pending actions found for processing.");
                                return null;
                            } else {
                                throw new RuntimeException(error);
                            }
                        })
                        .getOrThrow();

                if (this.batchAction == null) {
                    return;
                }

                final BatchActionExec batchActionExec = this.batchExecutions.get(this.batchAction.actionType);

                this.processingIds = new HashSet<>(this.batchAction.sourceIds);
                this.processingIds.removeAll(this.batchAction.successful);
                this.failedIds = new HashSet<>();

                this.processingIds
                        .stream()
                        .forEach(modelId -> {
                            final Result<EntityKey> doSingleAction = batchActionExec.doSingleAction(modelId);
                            if (doSingleAction.hasError()) {
                                log.error(
                                        "Failed to process single entity on batch action. ModelId: {}, action: ",
                                        modelId,
                                        this.batchAction,
                                        doSingleAction.getError());

                                this.failedIds.add(modelId);
                            } else {
                                this.batchActionDAO.updateProgress(null, modelId, this.failedIds);
                            }
                        });

            } catch (final Exception e) {
                log.error("Unexpected error while batch action processing. processorId: {} action: ",
                        this.processorId,
                        this.batchAction);
                log.info("Skip this batch action.");
            }
        }
    }

}
