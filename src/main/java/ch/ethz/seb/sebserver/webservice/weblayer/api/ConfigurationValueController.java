/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.validation.Valid;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.CONFIGURATION_VALUE_ENDPOINT)
public class ConfigurationValueController extends EntityController<ConfigurationValue, ConfigurationValue> {

    private final ConfigurationDAO configurationDAO;
    private final ConfigurationValueDAO configurationValueDAO;

    protected ConfigurationValueController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ConfigurationValueDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ConfigurationDAO configurationDAO) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.configurationDAO = configurationDAO;
        this.configurationValueDAO = entityDAO;
    }

    @Override
    protected ConfigurationValue createNew(final POSTMapper postParams) {
        final Long institutionId = postParams.getLong(API.PARAM_INSTITUTION_ID);
        return new ConfigurationValue(institutionId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ConfigurationValueRecordDynamicSqlSupport.configurationValueRecord;
    }

    @Override
    public EntityProcessingReport hardDelete(final String modelId) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(
            path = API.CONFIGURATION_TABLE_VALUE_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConfigurationTableValue getTableValueBy(
            @RequestParam(
                    name = Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID,
                    required = true) final Long attributeId,
            @RequestParam(
                    name = Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID,
                    required = true) final Long configurationId) {

        return this.configurationDAO.byPK(configurationId)
                .flatMap(this.authorization::checkRead)
                .flatMap(config -> this.configurationValueDAO.getTableValue(
                        config.institutionId,
                        attributeId,
                        configurationId))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.CONFIGURATION_TABLE_VALUE_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConfigurationTableValue savePut(
            @Valid @RequestBody final ConfigurationTableValue tableValue) {

        return this.configurationDAO.byPK(tableValue.configurationId)
                .flatMap(this.authorization::checkModify)
                .flatMap(config -> this.configurationValueDAO.saveTableValue(tableValue))
                .getOrThrow();
    }

    @Override
    protected GrantEntity toGrantEntity(final ConfigurationValue entity) {
        if (entity == null) {
            return null;
        }

        return this.configurationDAO.byPK(entity.configurationId)
                .getOrThrow();
    }

    @Override
    protected EntityType getGrantEntityType() {
        return EntityType.CONFIGURATION;
    }

}
