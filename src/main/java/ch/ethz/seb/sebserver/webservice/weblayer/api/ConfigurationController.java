/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.CONFIGURATION_ENDPOINT)
public class ConfigurationController extends ReadonlyEntityController<Configuration, Configuration> {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationDAO configurationDAO;
    private final ExamConfigUpdateService examConfigUpdateService;

    protected ConfigurationController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ConfigurationDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamConfigUpdateService examConfigUpdateService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.configurationDAO = entityDAO;
        this.examConfigUpdateService = examConfigUpdateService;
    }

    @RequestMapping(
            path = API.CONFIGURATION_SAVE_TO_HISTORY_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration saveToHistory(@PathVariable final Long modelId) {

        return this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkModify)
                .flatMap(this::processSEBExamConfigurationChange)
                .onError(t -> this.examConfigUpdateService.forceReleaseUpdateLocks(modelId))
                .flatMap(this.userActivityLogDAO::logSaveToHistory)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.CONFIGURATION_UNDO_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration undo(@PathVariable final String modelId) {

        return this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkModify)
                .flatMap(config -> this.configurationDAO.undo(config.configurationNodeId))
                .flatMap(this.userActivityLogDAO::logUndo)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.CONFIGURATION_RESTORE_FROM_HISTORY_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Configuration restoreFormHistory(
            @PathVariable final String modelId,
            @RequestParam(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long configurationNodeId) {

        return this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkModify)
                .flatMap(config -> this.configurationDAO.restoreToVersion(configurationNodeId, config.getId()))
                .getOrThrow();
    }

    @Override
    public Collection<EntityKey> getDependencies(final String modelId, final BulkActionType bulkActionType) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ConfigurationRecordDynamicSqlSupport.configurationRecord;
    }

    private Result<Configuration> processSEBExamConfigurationChange(final Configuration config) {
        if (config == null) {
            return Result.ofError(new NullPointerException("Configuration has null reference"));
        }

        return this.examConfigUpdateService.processExamConfigurationChange(config.configurationNodeId)
                .map(ids -> {
                    log.info("Successfully updated SEB Configuration for exams: {}", ids);
                    return config;
                });
    }

}
