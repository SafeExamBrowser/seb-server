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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.BatchActionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;

@Service
@WebServiceProfile
public class BatchActionServiceImpl implements BatchActionService {

    private static final Logger log = LoggerFactory.getLogger(BatchActionServiceImpl.class);

    public static final String BATCH_ACTION_ERROR_ATTR_NAME = "batchActionError";

    private final BatchActionDAO batchActionDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final JSONMapper jsonMapper;
    private final TaskScheduler taskScheduler;
    private final EnumMap<BatchActionType, BatchActionExec> batchExecutions;

    private ScheduledFuture<?> runningBatchProcess = null;

    public BatchActionServiceImpl(
            final BatchActionDAO batchActionDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final JSONMapper jsonMapper,
            final Collection<BatchActionExec> batchExecutions,
            final TaskScheduler taskScheduler) {

        this.batchActionDAO = batchActionDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.jsonMapper = jsonMapper;
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
            final Map<String, String> actionAttributes,
            final String ids) {

        return Result.tryCatch(() -> {

            final BatchActionExec batchActionExec = this.batchExecutions.get(actionType);
            if (batchActionExec == null) {
                throw new IllegalArgumentException(
                        "Batch action execution not found for batch action type: " + actionType);
            }

            final APIMessage consistencyError = batchActionExec.checkConsistency(actionAttributes);
            if (consistencyError != null) {
                throw new APIMessageException(consistencyError);
            }

            final Collection<String> sourceIds = Arrays.asList(StringUtils.split(
                    ids,
                    Constants.LIST_SEPARATOR));

            return this.batchActionDAO
                    .createNew(new BatchAction(
                            null,
                            institutionId,
                            actionType,
                            actionAttributes,
                            sourceIds,
                            null, null, null))
                    .map(res -> {
                        processNextBatchAction();
                        return res;
                    })
                    .getOrThrow();
        });
    }

    @Override
    public Result<BatchAction> getRunningAction(final String actionId) {
        return this.batchActionDAO.byModelId(actionId);
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

    @Scheduled(
            fixedDelayString = "${sebserver.webservice.batchaction.update-interval:60000}",
            initialDelay = 60000)
    private void processing() {
        processNextBatchAction();
    }

    private void processNextBatchAction() {

        if (this.runningBatchProcess != null && !this.runningBatchProcess.isDone()) {
            return;
        }

        try {

            final String processorId = UUID.randomUUID().toString();
            log.debug("Check for pending batch action with processorId: {}", processorId);

            final BatchAction batchAction = this.batchActionDAO
                    .getAndReserveNext(processorId)
                    .onErrorDo(error -> {
                        if (error instanceof ResourceNotFoundException) {
                            log.debug("No batch pending actions found for processing.");
                            return null;
                        } else {
                            throw new RuntimeException(error);
                        }
                    })
                    .getOrThrow();

            if (batchAction == null) {
                log.debug("No pending batch action found...");
                return;
            }

            this.runningBatchProcess = this.taskScheduler.schedule(
                    new BatchActionProcess(
                            new BatchActionHandlerImpl(batchAction),
                            this.batchExecutions.get(batchAction.actionType),
                            batchAction),
                    Instant.now());

        } catch (final Exception e) {
            log.error("Failed to schedule BatchActionProcess task: ", e);
        }
    }

    private final static class BatchActionProcess implements Runnable {

        private final BatchActionHandler batchActionHandler;
        private final BatchActionExec batchActionExec;
        private final BatchAction batchAction;

        private Set<String> processingIds;

        public BatchActionProcess(
                final BatchActionHandler batchActionHandler,
                final BatchActionExec batchActionExec,
                final BatchAction batchAction) {

            this.batchActionHandler = batchActionHandler;
            this.batchActionExec = batchActionExec;
            this.batchAction = batchAction;
        }

        @Override
        public void run() {
            try {

                log.info("Starting or continuing batch action - {}", this.batchAction);

                this.processingIds = new HashSet<>(this.batchAction.sourceIds);
                this.processingIds.removeAll(this.batchAction.successful);

                this.processingIds
                        .stream()
                        .forEach(modelId -> {

                            log.debug("Process batch action type: {}, id: {}", this.batchAction.actionType, modelId);

                            this.batchActionExec
                                    .doSingleAction(modelId, this.batchAction.attributes)
                                    .onError(error -> this.batchActionHandler.handleError(modelId, error))
                                    .onSuccess(entityKey -> this.batchActionHandler.handleSuccess(entityKey));

                        });

                log.info("Finished batch action - {}", this.batchAction);

            } catch (final Exception e) {
                log.error("Unexpected error while batch action processing. processorId: {} action: ",
                        this.batchAction.processorId,
                        this.batchAction);
                log.info("Skip this batch action... new batch action process will be started automatically");
            }
        }
    }

    private interface BatchActionHandler {

        void handleSuccess(final EntityKey entityKey);

        void handleError(final String modelId, final Exception error);
    }

    private final class BatchActionHandlerImpl implements BatchActionHandler {

        public final BatchAction batchAction;

        public BatchActionHandlerImpl(final BatchAction batchAction) {
            this.batchAction = batchAction;
        }

        @Override
        public void handleSuccess(final EntityKey entityKey) {
            BatchActionServiceImpl.this.batchActionDAO
                    .updateProgress(
                            this.batchAction.id,
                            this.batchAction.processorId,
                            entityKey.modelId)
                    .onError(error -> log.error("Failed to save progress: ", error));
        }

        @Override
        public void handleError(final String modelId, final Exception error) {
            log.error(
                    "Failed to process single entity on batch action. ModelId: {}, action: ",
                    modelId,
                    this.batchAction,
                    error);

            APIMessage apiMessage = null;
            if (error instanceof APIMessageException) {
                apiMessage = ((APIMessageException) error).getMainMessage();
            } else {
                apiMessage = APIMessage.ErrorMessage.UNEXPECTED.of(error);
            }

            if (apiMessage != null) {
                // save error message for reporting
                try {
                    BatchActionServiceImpl.this.additionalAttributesDAO.saveAdditionalAttribute(
                            EntityType.BATCH_ACTION,
                            this.batchAction.id,
                            BATCH_ACTION_ERROR_ATTR_NAME,
                            BatchActionServiceImpl.this.jsonMapper.writeValueAsString(apiMessage))
                            .getOrThrow();
                } catch (final Exception e) {
                    log.error("Unexpected error while trying to persist batch action error: {}", apiMessage, error);
                }
            }
        }

    }

}
