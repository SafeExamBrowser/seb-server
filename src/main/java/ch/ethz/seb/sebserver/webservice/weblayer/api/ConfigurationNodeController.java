/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.OrientationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ViewDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigTemplateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.CONFIGURATION_NODE_ENDPOINT)
public class ConfigurationNodeController extends EntityController<ConfigurationNode, ConfigurationNode> {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationNodeController.class);

    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ConfigurationDAO configurationDAO;
    private final ViewDAO viewDAO;
    private final OrientationDAO orientationDAO;
    private final ExamConfigService sebExamConfigService;
    private final ExamConfigTemplateService sebExamConfigTemplateService;

    protected ConfigurationNodeController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ConfigurationNodeDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ConfigurationDAO configurationDAO,
            final ViewDAO viewDAO,
            final OrientationDAO orientationDAO,
            final ExamConfigService sebExamConfigService,
            final ExamConfigTemplateService sebExamConfigTemplateService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.configurationDAO = configurationDAO;
        this.configurationNodeDAO = entityDAO;
        this.viewDAO = viewDAO;
        this.orientationDAO = orientationDAO;
        this.sebExamConfigService = sebExamConfigService;
        this.sebExamConfigTemplateService = sebExamConfigTemplateService;
    }

    @Override
    protected ConfigurationNode createNew(final POSTMapper postParams) {
        final Long institutionId = postParams.getLong(API.PARAM_INSTITUTION_ID);
        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
        postParams.putIfAbsent(EXAM.ATTR_OWNER, currentUser.uuid());
        return new ConfigurationNode(institutionId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ConfigurationNodeRecordDynamicSqlSupport.configurationNodeRecord;
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.CONFIGURATION_FOLLOWUP_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration getFollowup(@PathVariable final Long modelId) {

        this.entityDAO
                .byPK(modelId)
                .flatMap(this::checkModifyAccess)
                .getOrThrow();

        return this.configurationDAO
                .getFollowupConfiguration(modelId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.CONFIGURATION_COPY_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConfigurationNode copyConfiguration(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @Valid @RequestBody final ConfigCreationInfo copyInfo) {

        this.entityDAO.byPK(copyInfo.configurationNodeId)
                .flatMap(this.authorization::checkWrite);

        final SEBServerUser currentUser = this.authorization
                .getUserService()
                .getCurrentUser();

        return this.configurationNodeDAO.createCopy(
                institutionId,
                currentUser.getUserInfo().uuid,
                copyInfo)
                .map(config -> {
                    if (config.type == ConfigurationType.TEMPLATE) {
                        return this.createTemplate(config);
                    } else {
                        return config;
                    }
                })
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.CONFIGURATION_CONFIG_KEY_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConfigKey getConfigKey(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkRead);

        final String configKey = this.sebExamConfigService
                .generateConfigKey(institutionId, modelId)
                .getOrThrow();

        return new ConfigKey(configKey);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.CONFIGURATION_PLAIN_XML_DOWNLOAD_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadPlainXMLConfig(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletResponse response) throws IOException {

        this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.userActivityLogDAO::logExport);

        final ServletOutputStream outputStream = response.getOutputStream();

        try {
            this.sebExamConfigService.exportPlainXML(
                    outputStream,
                    institutionId,
                    modelId);

            response.setStatus(HttpStatus.OK.value());
        } catch (final Exception e) {
            log.error("Unexpected error while trying to downstream exam config: ", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    @RequestMapping(
            path = API.CONFIGURATION_IMPORT_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration importExamConfig(
            @RequestHeader(name = Domain.CONFIGURATION_NODE.ATTR_NAME, required = false) final String name,
            @RequestHeader(name = Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                    required = false) final String description,
            @RequestHeader(name = Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID, required = false) final String templateId,
            @RequestHeader(name = API.IMPORT_PASSWORD_ATTR_NAME, required = false) final String password,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) throws IOException {

        this.checkModifyPrivilege(institutionId);

        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();

        final ConfigurationNode configurationNode = new ConfigurationNode(
                null,
                institutionId,
                StringUtils.isNotBlank(templateId) ? Long.parseLong(templateId) : null,
                name,
                description,
                ConfigurationType.EXAM_CONFIG,
                currentUser.uuid(),
                ConfigurationStatus.CONSTRUCTION);

        final Configuration followup = this.beanValidationService.validateBean(configurationNode)
                .flatMap(this.entityDAO::createNew)
                .flatMap(this.configurationDAO::getFollowupConfiguration)
                .getOrThrow();

        final Result<Configuration> doImport = doImport(password, request, followup);
        if (doImport.hasError()) {

            // rollback of the new configuration
            this.configurationNodeDAO.delete(new HashSet<>(Arrays.asList(new EntityKey(
                    followup.configurationNodeId,
                    EntityType.CONFIGURATION_NODE))));
        }

        final Configuration config = doImport
                .getOrThrow();

        return this.configurationDAO
                .saveToHistory(config.configurationNodeId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.CONFIGURATION_IMPORT_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration importExamConfigOnExistingConfig(
            @PathVariable final Long modelId,
            @RequestHeader(name = API.IMPORT_PASSWORD_ATTR_NAME, required = false) final String password,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) throws IOException {

        this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkModify);

        final Configuration newConfig = this.configurationDAO
                .saveToHistory(modelId)
                .flatMap(this.configurationDAO::restoreToDefaultValues)
                .getOrThrow();

        final Result<Configuration> doImport = doImport(password, request, newConfig);
        if (doImport.hasError()) {

            // rollback of the existing values
            this.configurationDAO.undo(newConfig.configurationNodeId);

        }

        return doImport
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT + API.TEMPLATE_ATTRIBUTE_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<TemplateAttribute> getTemplateAttributePage(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long parentModelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());

        // if current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, getGrantEntityType())) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final List<TemplateAttribute> attrs = this.sebExamConfigTemplateService
                .getTemplateAttributes(
                        institutionId,
                        parentModelId,
                        sort,
                        filterMap)
                .getOrThrow();

        final int start = (this.paginationService.getPageNumber(pageNumber) - 1) *
                this.paginationService.getPageSize(pageSize);
        int end = start + this.paginationService.getPageSize(pageSize);
        if (attrs.size() < end) {
            end = attrs.size();
        }

        return new Page<>(
                attrs.size() / this.paginationService.getPageSize(pageSize),
                this.paginationService.getPageNumber(pageNumber),
                sort,
                attrs.subList(start, end));
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_ENDPOINT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TemplateAttribute getTemplateAttribute(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long parentModelId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);
        return this.sebExamConfigTemplateService
                .getAttribute(
                        institutionId,
                        parentModelId,
                        modelId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_ENDPOINT
                    + API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_RESET_VALUES,
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TemplateAttribute resetTemplateAttributeValues(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long parentModelId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkModifyPrivilege(institutionId);
        return this.sebExamConfigTemplateService
                .setDefaultValues(
                        institutionId,
                        parentModelId,
                        modelId)
                .flatMap(entity -> this.userActivityLogDAO.log(UserLogActivityType.MODIFY, entity))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_ENDPOINT
                    + API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_ATTACH_DEFAULT_ORIENTATION,
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TemplateAttribute attachDefaultTemplateAttributeOrientation(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long parentModelId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = API.PARAM_VIEW_ID, required = false) final Long viewId) {

        checkModifyPrivilege(institutionId);

        return this.sebExamConfigTemplateService
                .attachDefaultOrientation(
                        institutionId,
                        parentModelId,
                        modelId,
                        viewId)
                .flatMap(entity -> this.userActivityLogDAO.log(UserLogActivityType.MODIFY, entity))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_ENDPOINT
                    + API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.TEMPLATE_ATTRIBUTE_REMOVE_ORIENTATION,
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TemplateAttribute removeTemplateAttributeOrientation(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long parentModelId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkModifyPrivilege(institutionId);
        return this.sebExamConfigTemplateService
                .removeOrientation(
                        institutionId,
                        parentModelId,
                        modelId)
                .flatMap(entity -> this.userActivityLogDAO.log(UserLogActivityType.MODIFY, entity))
                .getOrThrow();
    }

    @Override
    protected Result<ConfigurationNode> validForSave(final ConfigurationNode entity) {
        return super.validForSave(entity)
                .map(e -> {
                    final ConfigurationNode existingNode = this.entityDAO.byPK(entity.id)
                            .getOrThrow();
                    if (existingNode.type != entity.type) {
                        throw new APIConstraintViolationException(
                                "The Type of ConfigurationNode cannot change after creation");
                    }
                    return e;
                });
    }

    @Override
    protected Result<ConfigurationNode> notifyCreated(final ConfigurationNode entity) {
        return super.notifyCreated(entity)
                .map(this::createTemplate);
    }

    private ConfigurationNode createTemplate(final ConfigurationNode node) {
        if (node.type != null && node.type == ConfigurationType.TEMPLATE) {
            // create views and orientations for node
            return this.viewDAO.copyDefaultViewsForTemplate(node)
                    .flatMap(viewMapping -> this.orientationDAO.copyDefaultOrientationsForTemplate(
                            node,
                            viewMapping))
                    .getOrThrow();
        }
        return node;
    }

    private Result<Configuration> doImport(
            final String password,
            final HttpServletRequest request,
            final Configuration configuration) throws IOException {

        final InputStream inputStream = new BufferedInputStream(request.getInputStream());
        try {

            final Configuration result = this.sebExamConfigService.importFromSEBFile(
                    configuration,
                    inputStream,
                    password)
                    .getOrThrow();

            return Result.of(result);

        } catch (final Exception e) {
            // NOTE: It seems that this has to be manually closed on error case
            //       We expected that this is closed by the API but if this manual close is been left
            //       some left-overs will affect strange behavior.
            //       TODO: find a better solution for this
            IOUtils.closeQuietly(inputStream);
            return Result.ofError(e);
        }
    }

}
