/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import ch.ethz.seb.sebserver.gbl.model.*;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.*;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

import static ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService.clientGroupTemplatePageSort;
import static ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService.indicatorTemplatePageSort;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_TEMPLATE_ENDPOINT)
@Tag(name = "ExamTemplate", description = "Exam template administration endpoints.")
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class ExamTemplateController extends EntityController<ExamTemplate, ExamTemplate> {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateController.class);

    private final ExamTemplateDAO examTemplateDAO;
    private final ExamTemplateService examTemplateService;
    private final ProctoringAdminService proctoringServiceSettingsService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConfigurationNodeDAO configurationNodeDAO;

    protected ExamTemplateController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ExamTemplateDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamTemplateService examTemplateService,
            final ProctoringAdminService proctoringServiceSettingsService,
            final ApplicationEventPublisher applicationEventPublisher,
            final ConfigurationNodeDAO configurationNodeDAO) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examTemplateDAO = entityDAO;
        this.examTemplateService = examTemplateService;
        this.proctoringServiceSettingsService = proctoringServiceSettingsService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.configurationNodeDAO = configurationNodeDAO;
    }

    /** Get a single ExamTemplate model with all data assigned to it.
     *
     * @param modelId The model identifier of the ExamTemplate to get
     * @return ExamTemplate model with all additional data applied*/
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ExamTemplate getBy(@PathVariable final String modelId) {

        checkReadPrivilege(this.authorization.getUserService().getCurrentUser().institutionId());

        return this.entityDAO
                .byModelId(modelId)
                .flatMap(this::checkReadAccess)
                .flatMap(examTemplateService::prepareForV3)
                .getOrThrow();
    }

    @Override
    protected Result<Collection<ExamTemplate>> getAll(FilterMap filterMap) {
        return super
                .getAll(filterMap)
                .flatMap(examTemplateService::applyForV3);
    }

    /** Get the institutional default Exam Template with all additional data assigned.
     *
     * @return ExamTemplate with all additional data*/
    @Operation(hidden = true)
    @RequestMapping(
            path = API.EXAM_TEMPLATE_DEFAULT_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate getDefault() {
        return ((ExamTemplateDAO) this.entityDAO)
                .getInstitutionalDefault(this.authorization.getUserService().getCurrentUser().institutionId())
                .flatMap(this::checkReadAccess)
                .flatMap(examTemplateService::prepareForV3)
                .getOrThrow();
    }

    /** Create a whole new Exam Template form given ExamTemplate model.
     *  This is used from the new GUI Exam Template Wizard that creates the Exam Template at the end of the wizard
     *  by sending all ExamTemplate data at once.
     * @param institutionId the institution id of the user
     * @param examTemplate ExamTemplate model with all data for creation
     * @return created ExamTemplate*/
    @Operation(
            operationId = "fullCreateExamTemplate",
            summary = "Creates a new exam template with all sub-templates in one call.",
            description = "Used by the exam template wizard to create the exam template together with its indicator templates, client group templates and exam attributes at once. If no configuration template is assigned, a default one is created.")
    @RequestMapping(
            path = API.EXAM_TEMPLATE_FULL_CREATE,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate createExamTemplate(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestBody final ExamTemplate examTemplate) {

        checkWritePrivilege(institutionId);

        final ExamTemplate newExamTemplate = new ExamTemplate(
                null,
                institutionId,
                examTemplate.name,
                examTemplate.description,
                examTemplate.examType,
                examTemplate.supporter,
                // Note: if no configuration template is assigned, we create a default one
                examTemplate.configTemplateId != null
                        ? examTemplate.configTemplateId
                        : createTemporaryConfigurationTemplate().id,
                examTemplate.institutionalDefault,
                examTemplate.lmsIntegration,
                examTemplate.clientConfigurationId,
                examTemplate.indicatorTemplates,
                examTemplate.clientGroupTemplates,
                examTemplate.examAttributes);
        
        return  beanValidationService
                .validateBean(newExamTemplate)
                .flatMap(examTemplateDAO::createNew)
                .flatMap(t -> examTemplateService.createExamTemplateAdditionalData(t.id, newExamTemplate))
                .flatMap(this::logCreate)

                // get source of truth from DB apply SPS Data and notify created
                .flatMap(r -> examTemplateDAO.byPK(r.id))
                .flatMap(examTemplateService::prepareForV3)
                .flatMap(this::notifyCreated)
                .getOrThrow();
    }

    /** This is used by the new GUI Exam Template Wizard to create a temporary Configuration Template to
     *  fill in the SEB Settings. The Configuration Template is created with a dedicated name prefix. The name
     *  of the Configuration Template will be changed to the name of the Exam Template as soon as the Exam Template
     *  gets stored.
     * @return ConfigurationNode model of the new created Configuration Template*/
    @Operation(
            operationId = "createTemporaryConfigTemplate",
            summary = "Creates a temporary configuration template for a new exam template.",
            description = "Holds the SEB settings during the exam template wizard; it is renamed to the exam template name as soon as the exam template gets stored.")
    @RequestMapping(
            path = API.EXAM_TEMPLATE_CREATE_TEMP_CONFIG_TEMPLATE,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ConfigurationNode createTemporaryConfigurationTemplate() {

        final SEBServerUser currentUser = this.authorization
                .getUserService()
                .getCurrentUser();

        final Long institutionId = currentUser.institutionId();
        checkWritePrivilege(institutionId);

        final ConfigurationNode configurationNode = new ConfigurationNode(
                null,
                institutionId,
                null,
                ExamTemplateService.createNameForTemporaryConfigurationTemplate(),
                null,
                ConfigurationNode.ConfigurationType.TEMPLATE,
                currentUser.uuid(),
                ConfigurationNode.ConfigurationStatus.READY_TO_USE,
                Utils.toDateTimeUTC(Utils.getMillisecondsNow()),
                currentUser.uuid(),
                null);

        return configurationNodeDAO
                .createNew(configurationNode)
                .getOrThrow();
    }

    /** Used to copy an existing ExamTemplate. Creates a full copy of Exam Template as well as Configuration Template
     *
     * @param modelId The model identifier of the existing ExamTemplate that should be copied.
     * @return Copy of existing ExamTemplate within the new ExamTemplate model.*/
    @Operation(
            operationId = "copyExamTemplate",
            summary = "Creates a full copy of the given exam template.",
            description = "Copies the exam template together with its configuration template under a generated copy name.")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.EXAM_TEMPLATE_COPY,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate copyExamTemplate(@PathVariable final String modelId) {

        final SEBServerUser currentUser = this.authorization
                .getUserService()
                .getCurrentUser();
        final Long institutionId = currentUser.institutionId();

        checkWritePrivilege(institutionId);

        final ExamTemplate sourceExamTemplate = getBy(modelId);
        final String newName = examTemplateDAO.getCopyName(sourceExamTemplate);

        // create copy of Configuration Template if available
        Long newConfigTemplateId = null;
        if (sourceExamTemplate.configTemplateId != null) {
            final ConfigCreationInfo copyInfo = new ConfigCreationInfo(
                    sourceExamTemplate.configTemplateId,
                    newName,
                    sourceExamTemplate.description,
                    false,
                    ConfigurationNode.ConfigurationType.TEMPLATE);

            newConfigTemplateId = configurationNodeDAO.createCopy(
                            institutionId,
                            currentUser.uuid(),
                            copyInfo)
                    .getOrThrow()
                    .id;
        }

        ExamTemplate modelForCopy = new ExamTemplate(
                null,
                institutionId,
                newName,
                sourceExamTemplate.description,
                sourceExamTemplate.examType,
                sourceExamTemplate.supporter,
                newConfigTemplateId,
                sourceExamTemplate.institutionalDefault,
                sourceExamTemplate.lmsIntegration,
                sourceExamTemplate.clientConfigurationId,
                sourceExamTemplate.indicatorTemplates,
                sourceExamTemplate.clientGroupTemplates,
                sourceExamTemplate.examAttributes);

        return createExamTemplate(institutionId, modelForCopy);
    }

    @Override
    protected Result<ExamTemplate> notifyCreated(final ExamTemplate entity) {
        return notifyExamTemplateChange(entity, ExamTemplateChangeEvent.ChangeState.CREATED);
    }

    @Override
    protected Result<ExamTemplate> notifySaved(final ExamTemplate entity) {
        return examTemplateService.saveAdditionalData(entity)
                .flatMap(t -> notifyExamTemplateChange(t, ExamTemplateChangeEvent.ChangeState.MODIFIED));
    }

    @Override
    protected Result<Pair<ExamTemplate, EntityProcessingReport>> notifyDeleted(final Pair<ExamTemplate, EntityProcessingReport> pair) {
        notifyExamTemplateChange(pair.a, ExamTemplateChangeEvent.ChangeState.DELETED);
        return super.notifyDeleted(pair);
    }

    // ****************************************************************************
    // **** Indicator Templates

    @Operation(hidden = true)
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<IndicatorTemplate> getIndicatorPage(
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        checkReadPrivilege(institutionId);

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM_TEMPLATE,
                institutionId);

        final ExamTemplate examTemplate = super.entityDAO.byModelId(modelId)
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                examTemplate.indicatorTemplates,
                indicatorTemplatePageSort(sort));
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IndicatorTemplate getIndicatorBy(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM_TEMPLATE,
                institutionId);

        return super.entityDAO
                .byModelId(parentModelId)
                .map(t -> t.indicatorTemplates
                        .stream()
                        .filter(i -> modelId.equals(i.getModelId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException(EntityType.INDICATOR, parentModelId)))
                .getOrThrow();
    }

    @Operation(
            operationId = "createIndicatorTemplate",
            summary = "Creates a new indicator template for the given exam template.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(
                            mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            schema = @Schema(implementation = IndicatorTemplate.class)) }))
    @RequestMapping(
            path = API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IndicatorTemplate createIndicatorTemplate(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);
        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.beanValidationService
                .validateBean(new IndicatorTemplate(
                        null,
                        postMap.getLong(IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID),
                        postMap))
                .map(this::checkIndicatorConsistency)
                .flatMap(this.examTemplateDAO::createNewIndicatorTemplate)
                .flatMap(this.userActivityLogDAO::logCreate)
                .getOrThrow();
    }

    @Operation(
            operationId = "saveIndicatorTemplate",
            summary = "Saves changes to an existing indicator template.")
    @RequestMapping(
            path = API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IndicatorTemplate saveIndicatorPut(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @Valid @RequestBody final IndicatorTemplate modifyData) {

        // check modify privilege for requested institution and concrete entityType
        this.checkModifyPrivilege(institutionId);
        return this.beanValidationService
                .validateBean(modifyData)
                .map(this::checkIndicatorConsistency)
                .flatMap(this.examTemplateDAO::saveIndicatorTemplate)
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    @Operation(
            operationId = "deleteIndicatorTemplate",
            summary = "Deletes the given indicator template of the given exam template.")
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityKey deleteIndicatorTemplate(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);
        return this.examTemplateDAO
                .deleteIndicatorTemplate(parentModelId, modelId)
                .flatMap(this.userActivityLogDAO::logDelete)
                .getOrThrow();
    }

    // **** Indicator Templates
    // ****************************************************************************
    // ****************************************************************************
    // **** Client Group Templates

    @Operation(hidden = true)
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ClientGroupTemplate> getClientGroupTemplatePage(
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkReadPrivilege(institutionId);

        final ExamTemplate examTemplate = this.getBy(modelId);

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                examTemplate.clientGroupTemplates,
                clientGroupTemplatePageSort(sort));
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientGroupTemplate getClientGroupTemplateBy(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);

        final ExamTemplate examTemplate = this.getBy(modelId);
        return examTemplate.clientGroupTemplates
                .stream()
                .filter(i -> modelId.equals(i.getModelId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(EntityType.CLIENT_GROUP, parentModelId));
    }

    @Operation(
            hidden = true,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }))
    @RequestMapping(
            path = API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientGroupTemplate createClientGroupTemplate(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);
        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.beanValidationService
                .validateBean(new ClientGroupTemplate(
                        null,
                        postMap.getLong(ClientGroupTemplate.ATTR_EXAM_TEMPLATE_ID),
                        postMap))
                .map(ExamUtils::checkClientGroupConsistency)
                .flatMap(this.examTemplateService::createNewClientGroupTemplate)
                .flatMap(this.userActivityLogDAO::logCreate)
                .getOrThrow();
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientGroupTemplate saveClientGroupTemplate(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @Valid @RequestBody final ClientGroupTemplate modifyData) {

        // check modify privilege for requested institution and concrete entityType
        this.checkModifyPrivilege(institutionId);
        return this.beanValidationService
                .validateBean(modifyData)
                .map(ExamUtils::checkClientGroupConsistency)
                .flatMap(this.examTemplateService::saveClientGroupTemplate)
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityKey deleteClientGroupTemplate(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);
        return this.examTemplateService
                .deleteClientGroupTemplate(parentModelId, modelId)
                .flatMap(this.userActivityLogDAO::logDelete)
                .getOrThrow();
    }

    // **** Client Group Templates
    // ****************************************************************************
    // ****************************************************************************
    // **** Screen Proctoring

    @Operation(
            operationId = "getExamTemplateScreenProctoringSettings",
            summary = "Gets the screen proctoring settings of the given exam template.")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SCREEN_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScreenProctoringSettings getScreenProctoringSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final Long modelId) {

        checkReadPrivilege(institutionId);
        return this.proctoringServiceSettingsService
                .getScreenProctoringSettings(new EntityKey(modelId, EntityType.EXAM_TEMPLATE))
                .getOrThrow();
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SCREEN_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate saveScreenProctoringSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examId,
            @Valid @RequestBody final ScreenProctoringSettings screenProctoringSettings) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO
                .byPK(examId)
                .flatMap(this.authorization::checkModify)
                .map(exam -> {
                    this.proctoringServiceSettingsService
                            .saveScreenProctoringSettings(
                                    new EntityKey(examId, EntityType.EXAM_TEMPLATE),
                                    screenProctoringSettings)
                            .getOrThrow();
                    return exam;
                })
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    // **** Screen Proctoring
    // ****************************************************************************

    @Override
    protected ExamTemplate createNew(final POSTMapper postParams) {
        final Long institutionId = postParams.getLong(API.PARAM_INSTITUTION_ID);
        return new ExamTemplate(institutionId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamTemplateRecordDynamicSqlSupport.examTemplateRecord;
    }

    private IndicatorTemplate checkIndicatorConsistency(final IndicatorTemplate indicatorTemplate) {
        ExamUtils.checkThresholdConsistency(indicatorTemplate.thresholds);
        return indicatorTemplate;
    }

    private Result<ExamTemplate> notifyExamTemplateChange(
            final ExamTemplate entity,
            final ExamTemplateChangeEvent.ChangeState changeState) {

        try {
            applicationEventPublisher.publishEvent(new ExamTemplateChangeEvent(entity, changeState));
        } catch (final Exception e) {
            log.error("Failed to notify ExamTemplate change: ", e);
        }

        return Result.of(entity);
    }



}
