/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_CONNECTION_ENDPOINT)
public class ClientConnectionController extends ReadonlyEntityController<ClientConnection, ClientConnection> {

    protected ClientConnectionController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientConnectionDAO clientConnectionDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorization,
                bulkActionService,
                clientConnectionDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
    }

    @Override
    protected Result<Collection<ClientConnection>> getAll(final FilterMap filterMap) {
        final String infoFilter = filterMap.getString(ClientConnection.FILTER_ATTR_INFO);
        if (StringUtils.isNotBlank(infoFilter)) {
            return super.getAll(filterMap)
                    .map(all -> all.stream().filter(c -> c.getInfo() == null || c.getInfo().contains(infoFilter))
                            .collect(Collectors.toList()));
        }

        return super.getAll(filterMap);
    }

    @Override
    public Collection<EntityDependency> getDependencies(
            final String modelId,
            final BulkActionType bulkActionType,
            final boolean addIncludes,
            final List<String> includes) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord;
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        checkRead(institutionId);
    }

    @Override
    protected Result<ClientConnection> checkReadAccess(final ClientConnection entity) {
        return Result.tryCatch(() -> {
            checkRead(entity.institutionId);
            return entity;
        });
    }

    @Override
    protected boolean hasReadAccess(final ClientConnection entity) {
        return checkReadAccess(entity).hasValue();
    }

    private void checkRead(final Long institution) {
        this.authorization.checkRole(
                institution,
                EntityType.CLIENT_EVENT,
                UserRole.EXAM_ADMIN,
                UserRole.EXAM_SUPPORTER);
    }
}
