/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;

@Lazy
@Component
public class DeleteExamAction implements BatchActionExec {

    private static final Logger log = LoggerFactory.getLogger(DeleteExamAction.class);

    private final ExamDAO examDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ClientGroupDAO clientGroupDAO;
    private final IndicatorDAO indicatorDAO;
    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DeleteExamAction(
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ClientGroupDAO clientGroupDAO,
            final IndicatorDAO indicatorDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final ApplicationEventPublisher applicationEventPublisher) {

        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.configurationNodeDAO = configurationNodeDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.indicatorDAO = indicatorDAO;
        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public BatchActionType actionType() {
        return BatchActionType.DELETE_EXAM;
    }

    @Override
    public APIMessage checkConsistency(final Map<String, String> actionAttributes) {
        // no additional check here
        return null;
    }

    @Override
    public Result<EntityKey> doSingleAction(final String modelId, final BatchAction batchAction) {
        return this.examDAO.byModelId(modelId)
                .flatMap( exam -> this.checkWriteAccess(exam, batchAction.ownerId))
                .flatMap(this::checkNoActiveSEBClientConnections)
                .flatMap(this::notifyDeletion)
                .flatMap(this::deleteExamDependencies)
                .flatMap(this::deleteExamWithRefs)
                .flatMap(exam -> logDeleted(exam, batchAction))
                .map(Exam::getEntityKey);
    }

    public Result<EntityKey> deleteExamInternal(final Exam exam) {
        return notifyDeletion(exam)
                .flatMap(this::deleteExamDependencies)
                .flatMap(this::deleteExamWithRefs)
                .map(Exam::getEntityKey);
    }

    public Result<EntityKey> scheduledDeleteExamInternal(final Exam exam) {

        log.info("Delete Exam called from ScheduledDelete, Exam --> {}", exam.externalId);

        return notifyScheduledDeletion(exam)
                .flatMap(this::deleteExamDependencies)
                .flatMap(this::deleteExamWithRefs)
                .map(Exam::getEntityKey);
    }

    private Result<Exam> notifyDeletion(final Exam exam) {
        return Result.tryCatch(() -> {
            this.applicationEventPublisher.publishEvent(
                    new ExamDeletionEvent(Collections.singletonList(exam.id), false));
            return exam;
        });
    }

    private Result<Exam> notifyScheduledDeletion(final Exam exam) {
        return Result.tryCatch(() -> {
            this.applicationEventPublisher.publishEvent(
                    new ExamDeletionEvent(Collections.singletonList(exam.id), true));
            return exam;
        });
    }

    private Result<Exam> deleteExamDependencies(final Exam entity) {
        return this.clientConnectionDAO.deleteAllForExam(entity.id)
                .flatMap(res -> deleteExamConfigMappingAndExamConfigs(entity))
                .flatMap(res -> this.clientGroupDAO.deleteAllForExam(entity.id))
                .flatMap(res -> this.indicatorDAO.deleteAllForExam(entity.id))
                .map(res -> entity);
    }

    private Result<Collection<EntityKey>> deleteExamConfigMappingAndExamConfigs(final Exam entity) {
        return examConfigurationMapDAO
                .allOfExam(entity.id)
                .map(all -> {
                    final Collection<EntityKey> result = this.examConfigurationMapDAO
                            .deleteAllForExam(entity.id)
                            .getOrThrow();

                    // delete dangling Exam Configs
                    all.forEach(cMap -> {
                        try {
                            final Collection<Long> used = examConfigurationMapDAO
                                    .getExamIdsForConfigNodeId(cMap.configurationNodeId)
                                    .getOrThrow();

                            if (used != null && used.isEmpty()) {

                                if (log.isDebugEnabled()) {
                                    log.debug("Delete Exam Configuration {} due to Exam deletion: {}", cMap.configurationNodeId, entity.getModelId());
                                }

                                // not used anymore so delete it
                                final Set<EntityKey> keys = Stream
                                        .of(new EntityKey(cMap.configurationNodeId, EntityType.CONFIGURATION_NODE))
                                        .collect(Collectors.toSet());

                                configurationNodeDAO.delete(keys).getOrThrow();
                            }
                        } catch (Exception e) {
                            log.error("Failed to delete dangling ConfigurationNode: {} cause: {}", cMap.configurationNodeId, e.getMessage() );
                        }
                    });

                    return result;
                });
    }

    private Result<Exam> deleteExamWithRefs(final Exam entity) {
        final Result<Collection<EntityKey>> delete =
                this.examDAO.delete(new HashSet<>(Collections.singletonList(entity.getEntityKey())));
        if (delete.hasError()) {
            return Result.ofError(delete.getError());
        } else {
            return Result.of(entity);
        }
    }

    private Result<Exam> checkWriteAccess(final Exam entity, final String ownerId) {
        if (entity != null) {
            this.authorization.checkWrite(entity);
        }
        return Result.of(entity);
    }

    private Result<Exam> checkNoActiveSEBClientConnections(final Exam exam) {
        if (exam.status != Exam.ExamStatus.RUNNING) {
            return Result.of(exam);
        }

        final Integer active = this.clientConnectionDAO
                .getAllActiveConnectionTokens(exam.id)
                .map(Collection::size)
                .onError(error -> log.warn("Failed to get active access tokens for exam: {}", error.getMessage()))
                .getOr(1);

        if (active == null || active == 0) {
            return Result.of(exam);
        }

        return Result.ofError(new APIMessageException(
                APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                        .of("Exam currently has active SEB Client connections.")));
    }

    private Result<Exam> logDeleted(final Exam entity, final BatchAction batchAction) {
        return this.userActivityLogDAO.log(
                batchAction.ownerId,
                UserLogActivityType.DELETE,
                entity,
                "Part of batch action: " + batchAction.processorId);
    }

}
