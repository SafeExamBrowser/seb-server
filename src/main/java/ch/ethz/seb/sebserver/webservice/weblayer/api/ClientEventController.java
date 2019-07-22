/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import javax.validation.Valid;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_EVENT_ENDPOINT)
public class ClientEventController extends EntityController<ClientEvent, ClientEvent> {

    private final ClientConnectionDAO clientConnectionDAO;

    protected ClientEventController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientEventDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ClientConnectionDAO clientConnectionDAO) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.clientConnectionDAO = clientConnectionDAO;
    }

    @Override
    public ClientEvent create(final MultiValueMap<String, String> allRequestParams, final Long institutionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientEvent savePut(@Valid final ClientEvent modifyData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<EntityKey> getDependencies(final String modelId, final BulkActionType bulkActionType) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ClientEvent createNew(final POSTMapper postParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ClientEventRecordDynamicSqlSupport.clientEventRecord;
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        checkRead(institutionId);
    }

    @Override
    protected Result<ClientEvent> checkReadAccess(final ClientEvent entity) {
        return Result.tryCatch(() -> {

            final ClientConnection clientConnection = this.clientConnectionDAO
                    .byPK(entity.connectionId)
                    .getOrThrow();

            checkRead(clientConnection.institutionId);
            return entity;
        });
    }

    @Override
    protected void checkModifyPrivilege(final Long institutionId) {
        throw new PermissionDeniedException(
                EntityType.CLIENT_EVENT,
                PrivilegeType.MODIFY,
                this.authorization.getUserService().getCurrentUser().uuid());
    }

    @Override
    protected Result<ClientEvent> checkModifyAccess(final ClientEvent entity) {
        throw new PermissionDeniedException(
                EntityType.CLIENT_EVENT,
                PrivilegeType.MODIFY,
                this.authorization.getUserService().getCurrentUser().uuid());
    }

    @Override
    protected Result<ClientEvent> checkWriteAccess(final ClientEvent entity) {
        throw new PermissionDeniedException(
                EntityType.CLIENT_EVENT,
                PrivilegeType.WRITE,
                this.authorization.getUserService().getCurrentUser().uuid());
    }

    @Override
    protected Result<ClientEvent> checkCreateAccess(final ClientEvent entity) {
        throw new PermissionDeniedException(
                EntityType.CLIENT_EVENT,
                PrivilegeType.WRITE,
                this.authorization.getUserService().getCurrentUser().uuid());
    }

    private void checkRead(final Long institution) {
        this.authorization.checkRole(
                UserRole.EXAM_SUPPORTER,
                institution,
                EntityType.CLIENT_EVENT);
    }

}
