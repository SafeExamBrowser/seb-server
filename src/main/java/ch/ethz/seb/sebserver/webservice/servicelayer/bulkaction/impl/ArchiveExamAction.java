/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;

@Lazy
@Component
@WebServiceProfile
public class ArchiveExamAction implements BatchActionExec {

    private final ExamDAO examDAO;
    private final ExamAdminService examAdminService;
    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;

    public ArchiveExamAction(
            final ExamDAO examDAO,
            final ExamAdminService examAdminService,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO) {

        this.examDAO = examDAO;
        this.examAdminService = examAdminService;
        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @Override
    public BatchActionType actionType() {
        return BatchActionType.ARCHIVE_EXAM;
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
                .flatMap(this.examAdminService::archiveExam)
                .flatMap(exam -> logArchived(exam, batchAction))
                .map(Exam::getEntityKey);
    }

    private Result<Exam> checkWriteAccess(final Exam entity) {
        if (entity != null) {
            this.authorization.checkWrite(entity);
        }
        return Result.of(entity);
    }

    private Result<Exam> logArchived(final Exam entity, final BatchAction batchAction) {
        return this.userActivityLogDAO.log(
                batchAction.ownerId,
                UserLogActivityType.ARCHIVE,
                entity,
                "Part of batch action: " + batchAction.processorId);
    }
}
