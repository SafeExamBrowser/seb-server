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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

public abstract class EntityController<T extends GrantEntity, M extends GrantEntity> {

    protected final AuthorizationGrantService authorizationGrantService;
    protected final BulkActionService bulkActionService;
    protected final EntityDAO<T, M> entityDAO;
    protected final UserActivityLogDAO userActivityLogDAO;
    protected final PaginationService paginationService;

    protected EntityController(
            final AuthorizationGrantService authorizationGrantService,
            final BulkActionService bulkActionService,
            final EntityDAO<T, M> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService) {

        this.authorizationGrantService = authorizationGrantService;
        this.bulkActionService = bulkActionService;
        this.entityDAO = entityDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public T byId(@PathVariable final String id) {
        return this.entityDAO
                .byModelId(id)
                .flatMap(entity -> this.authorizationGrantService.checkGrantOnEntity(
                        entity,
                        PrivilegeType.READ_ONLY))
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

    @RequestMapping(path = "/names", method = RequestMethod.GET)
    public Collection<EntityKeyAndName> getNames(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Entity.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        return getAll(institutionId, active)
                .getOrThrow()
                .stream()
                .filter(entity -> this.authorizationGrantService.hasGrant(entity, PrivilegeType.READ_ONLY))
                .map(Entity::toName)
                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/{id}/hard-delete", method = RequestMethod.DELETE)
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

    @RequestMapping(path = "/create", method = RequestMethod.PUT)
    public T create(@Valid @RequestBody final M modifyData) {
        return this.authorizationGrantService.checkGrantOnEntity(modifyData, PrivilegeType.WRITE)
                .flatMap(entity -> this.entityDAO.save(modifyData))
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.CREATE, entity))
                .flatMap(entity -> notifySave(modifyData, entity))
                .getOrThrow();
    }

    @RequestMapping(path = "/save", method = RequestMethod.POST)
    public T save(@Valid @RequestBody final M modifyData) {
        return this.authorizationGrantService.checkGrantOnEntity(modifyData, PrivilegeType.MODIFY)
                .flatMap(entity -> this.entityDAO.save(modifyData))
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.MODIFY, entity))
                .flatMap(entity -> notifySave(modifyData, entity))
                .getOrThrow();
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

    protected Result<Collection<T>> getAll(final Long institutionId, final Boolean active) {
        return this.entityDAO.all(institutionId);
    }
}
