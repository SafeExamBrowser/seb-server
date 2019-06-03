/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

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
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_CONFIGURATION_MAP_ENDPOINT)
public class ExamConfigurationMappingController extends EntityController<ExamConfigurationMap, ExamConfigurationMap> {

    private final ExamDAO examDao;

    protected ExamConfigurationMappingController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<ExamConfigurationMap, ExamConfigurationMap> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamDAO examDao) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDao = examDao;
    }

    @Override
    protected ExamConfigurationMap createNew(final POSTMapper postParams) {
        final Long institutionId = postParams.getLong(API.PARAM_INSTITUTION_ID);
        return new ExamConfigurationMap(institutionId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamConfigurationMapRecordDynamicSqlSupport.examConfigurationMapRecord;
    }

    @Override
    protected EntityType getGrantEntityType() {
        return EntityType.EXAM;
    }

    @Override
    protected GrantEntity toGrantEntity(final ExamConfigurationMap entity) {
        if (entity == null) {
            return null;
        }

        return this.examDao
                .byPK(entity.examId)
                .getOrThrow();
    }

    @Override
    protected Result<ExamConfigurationMap> checkCreateAccess(final ExamConfigurationMap entity) {
        final GrantEntity grantEntity = toGrantEntity(entity);
        this.authorization.checkWrite(grantEntity);
        return Result.of(entity);
    }

    @Override
    protected Result<ExamConfigurationMap> validForCreate(final ExamConfigurationMap entity) {
        return super.validForCreate(entity)
                .map(this::checkPasswordMatch);
    }

    @Override
    protected Result<ExamConfigurationMap> validForSave(final ExamConfigurationMap entity) {
        return super.validForSave(entity)
                .map(this::checkPasswordMatch);
    }

    private ExamConfigurationMap checkPasswordMatch(final ExamConfigurationMap entity) {
        if (entity.hasEncryptionSecret() && !entity.encryptSecret.equals(entity.confirmEncryptSecret)) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.EXAM_CONFIGURATION_MAP.TYPE_NAME,
                            PasswordChange.ATTR_NAME_PASSWORD,
                            "examConfigMapping:confirm_encrypt_secret:password.mismatch")));
        }

        return entity;
    }

}
