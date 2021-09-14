/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport.ErrorEntry;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.USER_ACTIVITY_LOG_ENDPOINT)
public class UserActivityLogController extends ReadonlyEntityController<UserActivityLog, UserActivityLog> {

    protected UserActivityLogController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<UserActivityLog, UserActivityLog> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
    }

    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDeleteAll(
            @RequestParam(name = API.PARAM_MODEL_ID_LIST) final List<String> ids,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // check user has SEB Server administrator role
        final SEBServerUser currentUser = this.authorization.getUserService()
                .getCurrentUser();
        if (!currentUser.getUserRoles().contains(UserRole.SEB_SERVER_ADMIN)) {
            throw new PermissionDeniedException(
                    EntityType.USER_ACTIVITY_LOG,
                    PrivilegeType.WRITE,
                    currentUser.getUserInfo());
        }

        if (ids == null || ids.isEmpty()) {
            return EntityProcessingReport.ofEmptyError();
        }

        final Set<EntityKey> sources = ids.stream()
                .map(id -> new EntityKey(id, EntityType.USER_ACTIVITY_LOG))
                .collect(Collectors.toSet());

        final Result<Collection<EntityKey>> delete = this.entityDAO.delete(sources);

        if (delete.hasError()) {
            return new EntityProcessingReport(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Arrays.asList(new ErrorEntry(null, APIMessage.ErrorMessage.UNEXPECTED.of(delete.getError()))),
                    BulkActionType.HARD_DELETE);
        } else {
            return new EntityProcessingReport(
                    sources,
                    delete.get(),
                    Collections.emptyList(),
                    BulkActionType.HARD_DELETE);
        }
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        checkRead(institutionId);
    }

    @Override
    protected Result<UserActivityLog> checkReadAccess(final UserActivityLog entity) {
        return Result.of(entity);
    }

    @Override
    protected boolean hasReadAccess(final UserActivityLog entity) {
        return true;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return UserActivityLogRecordDynamicSqlSupport.userActivityLogRecord;
    }

    private void checkRead(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.READ,
                EntityType.USER_ACTIVITY_LOG,
                institutionId);
    }

}
