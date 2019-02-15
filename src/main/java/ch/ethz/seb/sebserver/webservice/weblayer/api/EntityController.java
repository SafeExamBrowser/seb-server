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
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
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

    protected final AuthorizationService authorization;
    protected final BulkActionService bulkActionService;
    protected final EntityDAO<T, M> entityDAO;
    protected final UserActivityLogDAO userActivityLogDAO;
    protected final PaginationService paginationService;
    protected final BeanValidationService beanValidationService;

    protected EntityController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<T, M> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        this.authorization = authorization;
        this.bulkActionService = bulkActionService;
        this.entityDAO = entityDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
        this.beanValidationService = beanValidationService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    // ******************
    // * GET (all)
    // ******************

    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<T> getAll(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        checkReadPrivilege(institutionId);
        final FilterMap filterMap = new FilterMap(allRequestParams)
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                getSQLTableOfEntity(),
                () -> getAll(filterMap)).getOrThrow();
    }

    // ******************
    // * GET (names)
    // ******************

    @RequestMapping(
            path = API.NAMES_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityName> getNames(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        checkReadPrivilege(institutionId);
        final FilterMap filterMap = new FilterMap(allRequestParams)
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return getAll(filterMap)
                .getOrThrow()
                .stream()
                .map(Entity::toName)
                .collect(Collectors.toList());
    }

    // ******************
    // * GET (single)
    // ******************

    @RequestMapping(
            path = "/{modelId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T getBy(@PathVariable final String modelId) {

        return this.entityDAO
                .byModelId(modelId)
                .flatMap(this.authorization::checkReadonly)
                .getOrThrow();
    }

    // ******************
    // * GET (list)
    // ******************

    @RequestMapping(
            path = API.LIST_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
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
                .filter(this.authorization::hasReadonlyGrant)
                .collect(Collectors.toList());
    }

    // ******************
    // * POST (create)
    // ******************

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T create(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // check write privilege for requested institution and concrete entityType
        this.authorization.check(
                PrivilegeType.WRITE,
                this.entityDAO.entityType(),
                institutionId);

        final POSTMapper postMap = new POSTMapper(allRequestParams)
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        final M requestModel = this.createNew(postMap);

        return this.beanValidationService.validateBean(requestModel)
                .flatMap(this.entityDAO::createNew)
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.CREATE, entity))
                .flatMap(entity -> this.notifySaved(requestModel, entity))
                .getOrThrow();
    }

    // ****************
    // * PUT (save)
    // ****************

    @RequestMapping(
            path = "/{modelId}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T savePut(
            @PathVariable final String modelId,
            @Valid @RequestBody final M modifyData) {

        return this.beanValidationService.validateBean(modifyData)
                .flatMap(this.authorization::checkModify)
                .flatMap(m -> this.entityDAO.save(modelId, m))
                .flatMap(e -> this.userActivityLogDAO.log(ActivityType.MODIFY, e))
                .flatMap(e -> notifySaved(modifyData, e))
                .getOrThrow();
    }

    // ******************
    // * PATCH (save)
    // ******************

    // NOTE: not supported yet because of difficulties on conversion of params-map to json object
//    @RequestMapping(
//            path = "/{id}",
//            method = RequestMethod.PATCH,
//            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    public T savePost(
//            @PathVariable final String id,
//            @RequestParam final MultiValueMap<String, String> allRequestParams,
//            @RequestParam(
//                    name = Entity.FILTER_ATTR_INSTITUTION,
//                    required = true,
//                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {
//
//        allRequestParams.putIfAbsent(
//                Domain.ATTR_INSTITUTION_ID,
//                Arrays.asList(String.valueOf(institutionId)));
//        final M requestModel = this.toRequestModel(null, allRequestParams);
//
//        return this.entityDAO.save(id, requestModel)
//                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.MODIFY, entity))
//                .flatMap(entity -> notifySaved(requestModel, entity))
//                .getOrThrow();
//    }

    // ******************
    // * DELETE (delete)
    // ******************

    @RequestMapping(
            path = "/{modelId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDelete(@PathVariable final String modelId) {
        final EntityType entityType = this.entityDAO.entityType();
        final BulkAction bulkAction = new BulkAction(
                Type.HARD_DELETE,
                entityType,
                new EntityKey(modelId, entityType));

        return this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .flatMap(entity -> this.bulkActionService.createReport(bulkAction))
                .getOrThrow();
    }

    protected void checkReadPrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.READ_ONLY,
                this.entityDAO.entityType(),
                institutionId);
    }

    protected Result<Collection<T>> getAll(final FilterMap filterMap) {
        return this.entityDAO.allMatching(
                filterMap,
                this.authorization::hasReadonlyGrant);
    }

    protected Result<T> notifySaved(final M modifyData, final T entity) {
        return Result.of(entity);
    }

    protected abstract M createNew(POSTMapper postParams);

    protected abstract Class<M> modifiedDataType();

    protected abstract SqlTable getSQLTableOfEntity();

}
