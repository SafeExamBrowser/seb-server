/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.*;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.*;
import org.apache.commons.lang3.StringUtils;
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
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamTemplateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_TEMPLATE_ENDPOINT)
public class ExamTemplateController extends EntityController<ExamTemplate, ExamTemplate> {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateController.class);

    private final ExamTemplateDAO examTemplateDAO;
    private final ProctoringAdminService proctoringServiceSettingsService;
    private final ExamConfigurationValueService examConfigurationValueService;
    private final ApplicationEventPublisher applicationEventPublisher;

    protected ExamTemplateController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ExamTemplateDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ProctoringAdminService proctoringServiceSettingsService,
            final ExamConfigurationValueService examConfigurationValueService,
            final ApplicationEventPublisher applicationEventPublisher) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examTemplateDAO = entityDAO;
        this.proctoringServiceSettingsService = proctoringServiceSettingsService;
        this.examConfigurationValueService = examConfigurationValueService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RequestMapping(
            path = API.EXAM_TEMPLATE_DEFAULT_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate getDefault() {
        final Long institutionId = super.authorization
                .getUserService()
                .getCurrentUser()
                .institutionId();

        return ((ExamTemplateDAO) this.entityDAO)
                .getInstitutionalDefault(institutionId)
                .flatMap(this::checkReadAccess)
                .getOrThrow();
    }

    @Override
    protected Result<ExamTemplate> validForCreate(final ExamTemplate entity) {
        return super.validForCreate(entity)
                .map(this::applyQuitPasswordIfNeeded);
    }

    @Override
    protected Result<ExamTemplate> validForSave(final ExamTemplate entity) {
        return super.validForSave(entity)
                .map(this::applyQuitPasswordIfNeeded);
    }

    @Override
    protected Result<ExamTemplate> notifyCreated(final ExamTemplate entity) {
        return notifyExamTemplateChange(entity, ExamTemplateChangeEvent.ChangeState.CREATED);
    }

    @Override
    protected Result<ExamTemplate> notifySaved(final ExamTemplate entity) {
        return notifyExamTemplateChange(entity, ExamTemplateChangeEvent.ChangeState.MODIFIED);
    }

    @Override
    protected Result<Pair<ExamTemplate, EntityProcessingReport>> notifyDeleted(final Pair<ExamTemplate, EntityProcessingReport> pair) {
        notifyExamTemplateChange(pair.a, ExamTemplateChangeEvent.ChangeState.DELETED);
        return super.notifyDeleted(pair);
    }

    private ExamTemplate applyQuitPasswordIfNeeded(final ExamTemplate entity) {
        if (entity.configTemplateId != null) {
            try {
                final String quitPassword = this.examConfigurationValueService
                        .getQuitPasswordFromConfigTemplate(entity.configTemplateId);
                final HashMap<String, String> attributes = new HashMap<>(entity.examAttributes);
                attributes.put(ExamTemplate.ATTR_QUIT_PASSWORD, quitPassword);
                return new ExamTemplate(
                        entity.id,
                        entity.institutionId,
                        entity.name,
                        entity.description,
                        entity.examType,
                        entity.supporter,
                        entity.configTemplateId,
                        entity.institutionalDefault,
                        entity.lmsIntegration,
                        entity.clientConfigurationId,
                        entity.indicatorTemplates,
                        entity.clientGroupTemplates,
                        attributes
                );
            } catch (final Exception e) {
                log.error("Failed to apply quit password to Exam Template.", e);
            }
        }
        return entity;
    }

    // ****************************************************************************
    // **** Indicator Templates

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
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

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ClientGroupTemplate> getClientGroupTemplatePage(
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

        final ExamTemplate examTemplate = super.entityDAO
                .byModelId(modelId)
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                examTemplate.clientGroupTemplates,
                clientGroupTemplatePageSort(sort));
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientGroupTemplate getClientGroupTemplateBy(
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
                .map(t -> t.clientGroupTemplates
                        .stream()
                        .filter(i -> modelId.equals(i.getModelId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException(EntityType.CLIENT_GROUP, parentModelId)))
                .getOrThrow();
    }

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
                .flatMap(this.examTemplateDAO::createNewClientGroupTemplate)
                .flatMap(this.userActivityLogDAO::logCreate)
                .getOrThrow();
    }

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
                .flatMap(this.examTemplateDAO::saveClientGroupTemplate)
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

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
        return this.examTemplateDAO
                .deleteClientGroupTemplate(parentModelId, modelId)
                .flatMap(this.userActivityLogDAO::logDelete)
                .getOrThrow();
    }

    // **** Client Group Templates
    // ****************************************************************************
    // ****************************************************************************
    // **** Proctoring

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProctoringServiceSettings getProctoringServiceSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final Long modelId) {

        checkReadPrivilege(institutionId);
        return this.proctoringServiceSettingsService
                .getProctoringSettings(new EntityKey(modelId, EntityType.EXAM_TEMPLATE))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ExamTemplate saveProctoringServiceSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examId,
            @Valid @RequestBody final ProctoringServiceSettings proctoringServiceSettings) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO
                .byPK(examId)
                .flatMap(this.authorization::checkModify)
                .flatMap(examTemplate -> testAndSaveProctoringSettings(examId, examTemplate, proctoringServiceSettings))
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    private Result<ExamTemplate> testAndSaveProctoringSettings(
            final Long examId,
            final ExamTemplate examTemplate,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return this.proctoringServiceSettingsService
                .testProctoringSettings(proctoringServiceSettings)
                .flatMap(test -> this.proctoringServiceSettingsService
                        .saveProctoringServiceSettings(
                                new EntityKey(examId, EntityType.EXAM_TEMPLATE),
                                proctoringServiceSettings))
                .map(settings -> examTemplate);
    }

    // **** Proctoring
    // ****************************************************************************
    // ****************************************************************************
    // **** Screen Proctoring

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

    static Function<Collection<IndicatorTemplate>, List<IndicatorTemplate>> indicatorTemplatePageSort(
            final String sort) {

        final String sortBy = PageSortOrder.decode(sort);
        return indicators -> {
            final List<IndicatorTemplate> list = new ArrayList<>(indicators);
            if (StringUtils.isBlank(sort)) {
                return list;
            }

            if (sortBy.equals(Indicator.FILTER_ATTR_NAME)) {
                list.sort(Comparator.comparing(indicator -> indicator.name));
            }

            if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
                Collections.reverse(list);
            }
            return list;
        };
    }

    static Function<Collection<ClientGroupTemplate>, List<ClientGroupTemplate>> clientGroupTemplatePageSort(
            final String sort) {

        final String sortBy = PageSortOrder.decode(sort);
        return clientGroups -> {
            final List<ClientGroupTemplate> list = new ArrayList<>(clientGroups);
            if (StringUtils.isBlank(sort)) {
                return list;
            }

            if (sortBy.equals(Indicator.FILTER_ATTR_NAME)) {
                list.sort(Comparator.comparing(indicator -> indicator.name));
            }

            if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
                Collections.reverse(list);
            }
            return list;
        };
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
