/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.OrientationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.ORIENTATION_ENDPOINT)
public class OrientationController extends EntityController<Orientation, Orientation> {

    private final ConfigurationAttributeDAO configurationAttributeDAO;

    protected OrientationController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final OrientationDAO entityDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.configurationAttributeDAO = configurationAttributeDAO;
    }

    @Override
    protected Orientation createNew(final POSTMapper postParams) {
        final Long attributeId = postParams.getLong(Domain.ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID);

        return this.configurationAttributeDAO
                .byPK(attributeId)
                .map(attr -> new Orientation(attr, postParams))
                .getOrThrow();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return OrientationRecordDynamicSqlSupport.orientationRecord;
    }

    @Override
    protected Result<Orientation> checkCreateAccess(final Orientation entity) {
        // Skips the entity based grant check
        return Result.of(entity);
    }

    @Override
    protected GrantEntity toGrantEntity(final Orientation entity) {
        // Skips the entity based grant check
        return null;
    }

}
