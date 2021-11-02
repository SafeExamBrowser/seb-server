/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_INDICATOR_ENDPOINT)
public class IndicatorController extends EntityController<Indicator, Indicator> {

    private final ExamDAO examDao;
    private final ExamSessionService examSessionService;

    protected IndicatorController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final IndicatorDAO entityDAO,
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
    protected Indicator createNew(final POSTMapper postParams) {
        final Long examId = postParams.getLong(Domain.INDICATOR.ATTR_EXAM_ID);
        if (examId == null) {
            throw new RuntimeException("Missing exam model id from request parameter map!");
        }
        return new Indicator(examId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return IndicatorRecordDynamicSqlSupport.indicatorRecord;
    }

    @Override
    protected Result<Indicator> checkCreateAccess(final Indicator entity) {
        if (entity == null) {
            return null;
        }

        this.authorization.checkWrite(this.examDao.byPK(entity.examId).getOrThrow());
        return Result.of(entity);
    }

    @Override
    protected Result<Indicator> validForCreate(final Indicator entity) {
        final Result<Indicator> validForCreate = super.validForCreate(entity);
        if (validForCreate.hasError()) {
            return validForCreate;
        }

        return checkThresholdConsistency(entity);
    }

    @Override
    protected Result<Indicator> validForSave(final Indicator entity) {
        final Result<Indicator> validForSave = super.validForSave(entity);
        if (validForSave.hasError()) {
            return validForSave;
        }

        return checkThresholdConsistency(entity);
    }

    @Override
    protected GrantEntity toGrantEntity(final Indicator entity) {
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
    protected Result<Indicator> notifyCreated(final Indicator entity) {
        flushExamSessionCaches(entity);
        return super.notifyCreated(entity);
    }

    @Override
    protected Result<Indicator> notifySaved(final Indicator entity) {
        flushExamSessionCaches(entity);
        return super.notifySaved(entity);
    }

    @Override
    protected Result<Pair<Indicator, EntityProcessingReport>> notifyDeleted(
            final Pair<Indicator, EntityProcessingReport> pair) {

        flushExamSessionCaches(pair.a);
        return super.notifyDeleted(pair);
    }

    private void flushExamSessionCaches(final Indicator entity) {
        if (this.examSessionService.isExamRunning(entity.examId)) {
            this.examSessionService.flushCache(this.examSessionService.getRunningExam(entity.examId).getOrThrow());
        }

    }

    private Result<Indicator> checkThresholdConsistency(final Indicator entity) {
        if (entity != null) {
            final List<Threshold> emptyThresholds = entity.thresholds.stream()
                    .filter(t -> t.getValue() == null)
                    .collect(Collectors.toList());

            if (!emptyThresholds.isEmpty()) {
                throw new APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.EXAM.TYPE_NAME,
                                Domain.EXAM.ATTR_SUPPORTER,
                                "indicator:thresholds:thresholdEmpty")));
            }

            final Set<Double> values = entity.thresholds.stream()
                    .map(t -> t.getValue())
                    .collect(Collectors.toSet());

            if (values.size() != entity.thresholds.size()) {
                throw new APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.EXAM.TYPE_NAME,
                                Domain.EXAM.ATTR_SUPPORTER,
                                "indicator:thresholds:thresholdDuplicate")));
            }
        }
        return Result.of(entity);
    }

}
