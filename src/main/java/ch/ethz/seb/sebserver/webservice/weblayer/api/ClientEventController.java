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
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_EVENT_ENDPOINT)
public class ClientEventController extends ReadonlyEntityController<ClientEvent, ClientEvent> {

    private final ExamDAO examDAO;
    private final ClientEventDAO clientEventDAO;

    protected ClientEventController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientEventDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamDAO examDAO) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDAO = examDAO;
        this.clientEventDAO = entityDAO;
    }

    @RequestMapping(
            path = API.SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<ExtendedClientEvent> getExtendedPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        // at least current user must have base read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams);

        // if current user has no read access for specified entity type within other institution
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorization.hasGrant(PrivilegeType.READ, getGrantEntityType())) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        try {

            return this.paginationService.getPage(
                    pageNumber,
                    pageSize,
                    sort,
                    getSQLTableOfEntity().name(),
                    () -> this.clientEventDAO.allMatchingExtended(filterMap, this::hasReadAccess))
                    .getOrThrow();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Collection<EntityKey> getDependencies(final String modelId, final BulkActionType bulkActionType) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ClientEventRecordDynamicSqlSupport.clientEventRecord;
    }

    @Override
    protected GrantEntity toGrantEntity(final ClientEvent entity) {
        return this.examDAO
                .byClientConnection(entity.connectionId)
                .get();
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
        if (currentUser.institutionId().longValue() != institutionId.longValue()) {
            throw new PermissionDeniedException(
                    EntityType.CLIENT_EVENT,
                    PrivilegeType.READ,
                    currentUser.getUserInfo());
        }
    }

}
