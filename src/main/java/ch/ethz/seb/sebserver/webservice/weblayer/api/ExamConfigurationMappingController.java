/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.servlet.http.HttpServletRequest;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_CONFIGURATION_MAP_ENDPOINT)
public class ExamConfigurationMappingController extends EntityController<ExamConfigurationMap, ExamConfigurationMap> {

    private final ExamDAO examDao;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigUpdateService examConfigUpdateService;
    private final ExamSessionService examSessionService;

    protected ExamConfigurationMappingController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<ExamConfigurationMap, ExamConfigurationMap> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamDAO examDao,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigUpdateService examConfigUpdateService,
            final ExamSessionService examSessionService) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDao = examDao;
        this.configurationNodeDAO = configurationNodeDAO;
        this.examConfigUpdateService = examConfigUpdateService;
        this.examSessionService = examSessionService;
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
                .map(this::checkConfigurationState)
                .map(this::checkPasswordMatch)
                .map(this::checkNoActiveClientConnections);
    }

    @Override
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ExamConfigurationMap create(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        // check modify privilege for requested institution and concrete entityType
        this.checkModifyPrivilege(institutionId);
        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        final ExamConfigurationMap requestModel = this.createNew(postMap);
        return this.checkCreateAccess(requestModel)
                .flatMap(this::validForCreate)
                .map(this::checkPasswordMatch)
                .flatMap(entity -> this.examConfigUpdateService.processExamConfigurationMappingChange(
                        entity,
                        this.entityDAO::createNew))
                .flatMap(this::logCreate)
                .flatMap(this::notifyCreated)
                .getOrThrow();
    }

    @Override
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public EntityProcessingReport hardDelete(@PathVariable final String modelId) {

        return this.entityDAO.byModelId(modelId)
                .flatMap(this::checkWriteAccess)
                .flatMap(entity -> this.examConfigUpdateService.processExamConfigurationMappingChange(
                        entity,
                        this::bulkDelete))
                .flatMap(this::notifyDeleted)
                .flatMap(pair -> this.logBulkAction(pair.b))
                .getOrThrow();
    }

    @Override
    protected Result<ExamConfigurationMap> notifyCreated(final ExamConfigurationMap entity) {
        // update the attached configurations state to "In Use"
        return this.configurationNodeDAO.save(new ConfigurationNode(
                entity.configurationNodeId,
                null,
                null,
                null,
                null,
                null,
                null,
                ConfigurationStatus.IN_USE))
                .map(id -> entity);

    }

    @Override
    protected Result<Pair<ExamConfigurationMap, EntityProcessingReport>> notifyDeleted(
            final Pair<ExamConfigurationMap, EntityProcessingReport> pair) {
        // update the attached configurations state to "Ready"
        return this.configurationNodeDAO.save(new ConfigurationNode(
                pair.a.configurationNodeId,
                null,
                null,
                null,
                null,
                null,
                null,
                ConfigurationStatus.READY_TO_USE))
                .map(id -> pair);
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

    private ExamConfigurationMap checkConfigurationState(final ExamConfigurationMap entity) {
        final ConfigurationStatus status;
        if (entity.getConfigStatus() != null) {
            status = entity.getConfigStatus();
        } else {
            status = this.configurationNodeDAO.byPK(entity.configurationNodeId)
                    .getOrThrow()
                    .getStatus();
        }

        if (status != ConfigurationStatus.READY_TO_USE) {
            throw new APIMessageException(ErrorMessage.INTEGRITY_VALIDATION.of(
                    "Illegal SEB Exam Configuration state"));
        }

        return entity;
    }

    private ExamConfigurationMap checkNoActiveClientConnections(final ExamConfigurationMap entity) {
        if (this.examSessionService.hasActiveSEBClientConnections(entity.examId)) {
            throw new APIMessageException(ErrorMessage.INTEGRITY_VALIDATION.of(
                    "The Exam is currently running and has active SEB Client connections"));
        }

        return entity;
    }

}
