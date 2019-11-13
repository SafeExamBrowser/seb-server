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
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

/** Abstract Entity-Controller that defines generic Entity rest API endpoints that are supported
 * by all entity types that has activation feature and can be activated or deactivated.
 *
 * @param <T> The concrete Entity domain-model type used on all GET, PUT
 * @param <M> The concrete Entity domain-model type used for POST methods (new) */
public abstract class ActivatableEntityController<T extends GrantEntity, M extends GrantEntity>
        extends EntityController<T, M> {

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
    }

    // TODO use also the getAll method
    @RequestMapping(
            path = API.ACTIVE_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<T> allActive(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap()
                .putIfAbsent(Entity.FILTER_ATTR_ACTIVE, "true")
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                getSQLTableOfEntity().name(),
                () -> getAll(filterMap)).getOrThrow();
    }

    @RequestMapping(
            path = API.INACTIVE_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<T> allInactive(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap()
                .putIfAbsent(Entity.FILTER_ATTR_ACTIVE, "false")
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                getSQLTableOfEntity().name(),
                () -> getAll(filterMap)).getOrThrow();
    }

    @RequestMapping(
            path = API.PATH_VAR_ACTIVE,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public EntityProcessingReport activate(@PathVariable final String modelId) {
        return setActive(modelId, true)
                .getOrThrow();
    }

    @RequestMapping(
            value = API.PATH_VAR_INACTIVE,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public EntityProcessingReport deactivate(@PathVariable final String modelId) {
        return setActive(modelId, false)
                .getOrThrow();
    }

    private Result<EntityProcessingReport> setActive(final String modelId, final boolean active) {
        final EntityType entityType = this.entityDAO.entityType();

        return this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .flatMap(this::validForActivation)
                .flatMap(entity -> {
                    final Result<EntityProcessingReport> createReport =
                            this.bulkActionService.createReport(new BulkAction(
                                    (active) ? BulkActionType.ACTIVATE : BulkActionType.DEACTIVATE,
                                    entityType,
                                    new EntityName(modelId, entityType, entity.getName())));
                    this.notifySaved(entity);
                    return createReport;
                });
    }

    protected Result<T> validForActivation(final T entity) {
        return Result.of(entity);
    }

}
