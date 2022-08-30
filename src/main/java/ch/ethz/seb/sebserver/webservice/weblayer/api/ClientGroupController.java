/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientGroupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_CLIENT_GROUP_ENDPOINT)
public class ClientGroupController extends EntityController<ClientGroup, ClientGroup> {

    private final ExamDAO examDao;
    private final ExamSessionService examSessionService;

    protected ClientGroupController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientGroupDAO entityDAO,
            final ExamDAO examDao,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamSessionService examSessionService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDao = examDao;
        this.examSessionService = examSessionService;
    }

    @Override
    protected ClientGroup createNew(final POSTMapper postParams) {
        final Long examId = postParams.getLong(Domain.CLIENT_GROUP.ATTR_EXAM_ID);
        if (examId == null) {
            throw new RuntimeException("Missing exam model id from request parameter map!");
        }
        return new ClientGroup(examId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ClientGroupRecordDynamicSqlSupport.clientGroupRecord;
    }

    @Override
    protected Result<ClientGroup> checkCreateAccess(final ClientGroup entity) {
        if (entity == null) {
            return null;
        }

        return this.examDao.byPK(entity.examId)
                .flatMap(e -> this.authorization.checkWrite(e))
                .map(e -> entity);
    }

    @Override
    protected Result<ClientGroup> validForCreate(final ClientGroup entity) {
        return super.validForCreate(entity)
                .map(ExamAdminService::checkClientGroupConsistency);
    }

    @Override
    protected Result<ClientGroup> validForSave(final ClientGroup entity) {
        return super.validForSave(entity)
                .map(ExamAdminService::checkClientGroupConsistency);
    }

    @Override
    protected GrantEntity toGrantEntity(final ClientGroup entity) {
        if (entity == null) {
            return null;
        }

        return this.examDao
                .examGrantEntityByPK(entity.examId)
                .getOrThrow();
    }

    @Override
    protected EntityType getGrantEntityType() {
        return EntityType.EXAM;
    }

    @Override
    protected Result<ClientGroup> notifyCreated(final ClientGroup entity) {
        flushExamSessionCaches(entity);
        return super.notifyCreated(entity);
    }

    @Override
    protected Result<ClientGroup> notifySaved(final ClientGroup entity) {
        flushExamSessionCaches(entity);
        return super.notifySaved(entity);
    }

    @Override
    protected Result<Pair<ClientGroup, EntityProcessingReport>> notifyDeleted(
            final Pair<ClientGroup, EntityProcessingReport> pair) {

        flushExamSessionCaches(pair.a);
        return super.notifyDeleted(pair);
    }

    private void flushExamSessionCaches(final ClientGroup entity) {
        if (this.examSessionService.isExamRunning(entity.examId)) {
            this.examSessionService.flushCache(this.examSessionService.getRunningExam(entity.examId).getOrThrow());
        }
    }

}
