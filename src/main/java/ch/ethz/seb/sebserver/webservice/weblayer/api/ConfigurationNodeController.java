/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.CONFIGURATION_NODE_ENDPOINT)
public class ConfigurationNodeController extends EntityController<ConfigurationNode, ConfigurationNode> {

    private final ConfigurationDAO configurationDAO;
    private final SebExamConfigService sebExamConfigService;

    protected ConfigurationNodeController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ConfigurationNodeDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ConfigurationDAO configurationDAO,
            final SebExamConfigService sebExamConfigService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.configurationDAO = configurationDAO;
        this.sebExamConfigService = sebExamConfigService;
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
    public ResponseEntity<StreamingResponseBody> downloadPlainXMLConfig(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkRead);

        final StreamingResponseBody stream = out -> this.sebExamConfigService
                .exportPlainXML(out, institutionId, modelId);

        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

}
