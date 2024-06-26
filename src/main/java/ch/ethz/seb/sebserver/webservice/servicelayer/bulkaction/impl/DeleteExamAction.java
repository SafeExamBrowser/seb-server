/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
@WebServiceProfile
public class DeleteExamAction implements BatchActionExec {

    private static final Logger log = LoggerFactory.getLogger(DeleteExamAction.class);

    private final ExamDAO examDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ClientGroupDAO clientGroupDAO;
    private final IndicatorDAO indicatorDAO;
    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;

    public DeleteExamAction(
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ClientGroupDAO clientGroupDAO,
            final IndicatorDAO indicatorDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO) {

        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.indicatorDAO = indicatorDAO;
        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
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
    @Transactional
    public Result<EntityKey> doSingleAction(final String modelId, final BatchAction batchAction) {
        return this.examDAO.byModelId(modelId)
                .flatMap( exam -> this.checkWriteAccess(exam, batchAction.ownerId))
                .flatMap(this::checkNoActiveSEBClientConnections)
                .flatMap(this::deleteExamDependencies)
                .flatMap(this::deleteExamWithRefs)
                .flatMap(exam -> logDeleted(exam, batchAction))
                .map(Exam::getEntityKey)
                .onError(TransactionHandler::rollback);
    }

    @Transactional
    public Result<EntityKey> deleteExamInternal(final Exam exam) {
        return deleteExamDependencies(exam)
                .flatMap(this::deleteExamWithRefs)
                .map(Exam::getEntityKey)
                .onError(TransactionHandler::rollback);
    }

    private Result<Exam> deleteExamDependencies(final Exam entity) {
        return this.clientConnectionDAO.deleteAllForExam(entity.id)
                .map(this::logDelete)
                .flatMap(res -> this.examConfigurationMapDAO.deleteAllForExam(entity.id))
                .map(this::logDelete)
                .flatMap(res -> this.clientGroupDAO.deleteAllForExam(entity.id))
                .map(this::logDelete)
                .flatMap(res -> this.indicatorDAO.deleteAllForExam(entity.id))
                .map(this::logDelete)
                .map(res -> entity);
    }

    private Collection<EntityKey> logDelete(final Collection<EntityKey> deletedKeys) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Exam deletion, deleted references: {}", deletedKeys);
            }
        } catch (final Exception e) {
            log.error("Failed to log deletion for: {}", deletedKeys, e);
        }

        return deletedKeys;
    }

    private Result<Exam> deleteExamWithRefs(final Exam entity) {
        final Result<Collection<EntityKey>> delete =
                this.examDAO.delete(new HashSet<>(Arrays.asList(entity.getEntityKey())));
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
