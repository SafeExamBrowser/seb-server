/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

public abstract class EntityController<T extends GrantEntity, M extends GrantEntity> {

    protected final AuthorizationGrantService authorizationGrantService;
    protected final BulkActionService bulkActionService;
    protected final EntityDAO<T, M> entityDAO;
    protected final UserActivityLogDAO userActivityLogDAO;
    protected final PaginationService paginationService;
    protected final BeanValidationService beanValidationService;

    protected EntityController(
            final AuthorizationGrantService authorizationGrantService,
            final BulkActionService bulkActionService,
            final EntityDAO<T, M> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        this.authorizationGrantService = authorizationGrantService;
        this.bulkActionService = bulkActionService;
        this.entityDAO = entityDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
        this.beanValidationService = beanValidationService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorizationGrantService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<T> getAll(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String sortBy,
            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final Page.SortOrder sortOrder,
            @RequestParam final Map<String, String> allRequestParams) {

        checkReadPrivilege(institutionId);
        final FilterMap filterMap = new FilterMap(allRequestParams);
        allRequestParams.putIfAbsent(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(institutionId));

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sortBy,
                sortOrder,
                getSQLTableOfEntity(),
                () -> getAll(filterMap)).getOrThrow();
    }

    @RequestMapping(path = "/names", method = RequestMethod.GET)
    public Collection<EntityKeyAndName> getNames(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam final Map<String, String> allRequestParams) {

        checkReadPrivilege(institutionId);
        final FilterMap filterMap = new FilterMap(allRequestParams);
        allRequestParams.putIfAbsent(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(institutionId));

        return getAll(filterMap)
                .getOrThrow()
                .stream()
                .map(Entity::toName)
                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public T getBy(@PathVariable final String id) {

        return this.entityDAO
                .byModelId(id)
                .flatMap(entity -> this.authorizationGrantService.checkGrantOnEntity(
                        entity,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();
    }

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T create(
            @RequestParam final Map<String, String> allRequestParams,
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.INSTITUTION,
                PrivilegeType.WRITE);

        allRequestParams.putIfAbsent(Domain.ATTR_INSTITUTION_ID, String.valueOf(institutionId));
        final M modifyData = this.beanValidationService.validateNewBean(
                allRequestParams,
                modifiedDataType());

        final M _modifyData = beforeSave(modifyData);

        return this.checkIsNew(modifyData)
                .flatMap(entity -> this.entityDAO.save(_modifyData))
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.CREATE, entity))
                .flatMap(entity -> notifySave(_modifyData, entity))
                .getOrThrow();
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T savePut(@Valid @RequestBody final M modifyData) {

        final M _modifyData = beforeSave(modifyData);

        return this.authorizationGrantService.checkGrantOnEntity(_modifyData, PrivilegeType.MODIFY)
                .flatMap(entity -> this.entityDAO.save(_modifyData))
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.MODIFY, entity))
                .flatMap(entity -> notifySave(_modifyData, entity))
                .getOrThrow();
    }

    protected M beforeSave(final M modifyData) {
        return modifyData;
    }

    @RequestMapping(
            path = "/{id}",
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T savePost(
            @PathVariable final String id,
            @RequestParam final Map<String, String> allRequestParams,
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        final T model = this.entityDAO
                .byModelId(id)
                .flatMap(entity -> this.authorizationGrantService.checkGrantOnEntity(
                        entity,
                        PrivilegeType.MODIFY))
                .getOrThrow();

        allRequestParams.putIfAbsent(Domain.ATTR_INSTITUTION_ID, String.valueOf(institutionId));
        final M modifyData = this.beanValidationService.validateModifiedBean(
                model,
                allRequestParams,
                modifiedDataType());

        final M _modifyData = beforeSave(modifyData);

        return this.entityDAO.save(_modifyData)
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.MODIFY, entity))
                .flatMap(entity -> notifySave(_modifyData, entity))
                .getOrThrow();
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
    public EntityProcessingReport hardDelete(@PathVariable final String id) {
        final EntityType entityType = this.entityDAO.entityType();
        final BulkAction bulkAction = new BulkAction(
                Type.HARD_DELETE,
                entityType,
                new EntityKey(id, entityType));

        return this.entityDAO.byModelId(id)
                .flatMap(entity -> this.authorizationGrantService.checkGrantOnEntity(
                        entity,
                        PrivilegeType.WRITE))
                .flatMap(entity -> this.bulkActionService.createReport(bulkAction))
                .getOrThrow();
    }

    @RequestMapping(path = "/in", method = RequestMethod.GET)
    public Collection<T> getForIds(@RequestParam(name = "ids", required = true) final String ids) {
        return Result.tryCatch(() -> {
            return Arrays.asList(StringUtils.split(ids, Constants.LIST_SEPARATOR_CHAR))
                    .stream()
                    .map(modelId -> new EntityKey(modelId, this.entityDAO.entityType()))
                    .collect(Collectors.toList());
        })
                .flatMap(this.entityDAO::loadEntities)
                .getOrThrow()
                .stream()
                .filter(entity -> this.authorizationGrantService.hasGrant(entity, PrivilegeType.READ_ONLY))
                .collect(Collectors.toList());
    }

    protected Result<T> notifySave(final M modifyData, final T entity) {
        return Result.of(entity);
    }

    protected void checkReadPrivilege(final Long institutionId) {
        this.authorizationGrantService.checkPrivilege(
                this.entityDAO.entityType(),
                PrivilegeType.READ_ONLY,
                institutionId);
    }

    protected Result<Collection<T>> getAll(final FilterMap filterMap) {
        final Predicate<T> grantFilter = this.authorizationGrantService.getGrantFilter(
                this.entityDAO.entityType(),
                PrivilegeType.READ_ONLY);

        return this.entityDAO.allMatching(filterMap, grantFilter);
    }

    protected abstract Class<M> modifiedDataType();

    protected abstract SqlTable getSQLTableOfEntity();

    private Result<M> checkIsNew(final M entity) {
        if (entity.getModelId() == null) {
            return Result.of(entity);
        } else {
            return Result
                    .ofError(new IllegalAPIArgumentException("Request model has already an identifier but should not"));
        }
    }
}
