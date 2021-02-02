/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.CONFIGURATION_ATTRIBUTE_ENDPOINT)
public class ConfigurationAttributeController extends EntityController<ConfigurationAttribute, ConfigurationAttribute> {

    protected ConfigurationAttributeController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ConfigurationAttributeDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
    }

    @Override
    @RequestMapping(
            path = API.LIST_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ConfigurationAttribute> getForIds(
            @RequestParam(name = API.PARAM_MODEL_ID_LIST, required = false) final String modelIds) {

        if (StringUtils.isNotBlank(modelIds)) {
            return super.getForIds(modelIds);
        }

        return new ArrayList<>(this.entityDAO
                .allMatching(new FilterMap())
                .getOrThrow());
    }

    @Override
    protected ConfigurationAttribute createNew(final POSTMapper postParams) {
        return new ConfigurationAttribute(postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ConfigurationAttributeRecordDynamicSqlSupport.configurationAttributeRecord;
    }

    @Override
    protected Result<ConfigurationAttribute> checkCreateAccess(final ConfigurationAttribute entity) {
        return Result.of(entity); // Skips the entity based grant check
    }

    @Override
    protected GrantEntity toGrantEntity(final ConfigurationAttribute entity) {
        return null; // Skips the entity based grant check
    }

}
