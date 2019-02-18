/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

public abstract class ActivatableEntityController<T extends GrantEntity, M extends GrantEntity>
        extends EntityController<T, M> {

    private final ActivatableEntityDAO<T, M> activatableEntityDAO;

    public ActivatableEntityController(
            final AuthorizationService authorizationGrantService,
            final BulkActionService bulkActionService,
            final ActivatableEntityDAO<T, M> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorizationGrantService,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
        this.activatableEntityDAO = entityDAO;
    }

    @RequestMapping(
            path = API.ACTIVE_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<T> allActive(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkReadPrivilege(institutionId);
        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                UserRecordDynamicSqlSupport.userRecord,
                () -> this.activatableEntityDAO.all(institutionId, true)).getOrThrow();
    }

    @RequestMapping(
            path = API.INACTIVE_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<T> allInactive(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkReadPrivilege(institutionId);
        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                UserRecordDynamicSqlSupport.userRecord,
                () -> this.activatableEntityDAO.all(institutionId, false)).getOrThrow();
    }

    @RequestMapping(
            path = API.PATH_VAR_ACTIVE,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport activate(@PathVariable final String modelId) {
        return setActive(modelId, true)
                .getOrThrow();
    }

    @RequestMapping(
            value = API.PATH_VAR_INACTIVE,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport deactivate(@PathVariable final String modelId) {
        return setActive(modelId, false)
                .getOrThrow();
    }

    private Result<EntityProcessingReport> setActive(final String id, final boolean active) {
        final EntityType entityType = this.entityDAO.entityType();
        final BulkAction bulkAction = new BulkAction(
                (active) ? BulkActionType.ACTIVATE : BulkActionType.DEACTIVATE,
                entityType,
                new EntityKey(id, entityType));

        return this.entityDAO.byModelId(id)
                .flatMap(this.authorization::checkWrite)
                .flatMap(entity -> this.bulkActionService.createReport(bulkAction));
    }

}
