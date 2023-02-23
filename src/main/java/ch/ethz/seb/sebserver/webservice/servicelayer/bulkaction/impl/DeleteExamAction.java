/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
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

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Component
@WebServiceProfile
public class DeleteExamAction implements BatchActionExec {

    private final ExamDAO examDAO;
    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ExamSessionService examSessionService;

    public DeleteExamAction(
            final ExamDAO examDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamSessionService examSessionService) {

        this.examDAO = examDAO;
        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
        this.examSessionService = examSessionService;
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
                .flatMap(this::checkWriteAccess)
                .flatMap(this::checkNoActiveSEBClientConnections)
                .flatMap(this::deleteExamWithRefs)
                .flatMap(exam -> logDeleted(exam, batchAction))
                .map(Exam::getEntityKey);
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

    private Result<Exam> checkWriteAccess(final Exam entity) {
        if (entity != null) {
            this.authorization.checkWrite(entity);
        }
        return Result.of(entity);
    }

    private Result<Exam> checkNoActiveSEBClientConnections(final Exam exam) {
        if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
            return Result.ofError(new APIMessageException(
                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                            .of("Exam currently has active SEB Client connections.")));
        }

        return Result.of(exam);
    }

    private Result<Exam> logDeleted(final Exam entity, final BatchAction batchAction) {
        return this.userActivityLogDAO.log(
                batchAction.ownerId,
                UserLogActivityType.DELETE,
                entity,
                "Part of batch action: " + batchAction.processorId);
    }

}
