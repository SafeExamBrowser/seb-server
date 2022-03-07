/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

public abstract class ReadonlyEntityController<T extends Entity, M extends Entity> extends EntityController<T, M> {

    private static final String ONLY_READ_ACCESS = "Only read requests available for this entity";

    protected ReadonlyEntityController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<T, M> entityDAO,
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
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T savePut(@Valid @RequestBody final T modifyData) {
        throw new AccessDeniedException(ONLY_READ_ACCESS);
    }

    @Override
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public T create(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        throw new AccessDeniedException(ONLY_READ_ACCESS);
    }

    @Override
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDelete(
            @PathVariable final String modelId,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes) {
        throw new AccessDeniedException(ONLY_READ_ACCESS);
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

        throw new AccessDeniedException(ONLY_READ_ACCESS);
    }

    @Override
    protected M createNew(final POSTMapper postParams) {
        throw new AccessDeniedException(ONLY_READ_ACCESS);
    }

    @Override
    protected void checkModifyPrivilege(final Long institutionId) {
        throw new PermissionDeniedException(
                getGrantEntityType(),
                PrivilegeType.MODIFY,
                this.authorization.getUserService().getCurrentUser().getUserInfo());
    }

    @Override
    protected Result<T> checkModifyAccess(final T entity) {
        throw new PermissionDeniedException(
                getGrantEntityType(),
                PrivilegeType.MODIFY,
                this.authorization.getUserService().getCurrentUser().getUserInfo());
    }

    @Override
    protected Result<T> checkWriteAccess(final T entity) {
        throw new PermissionDeniedException(
                getGrantEntityType(),
                PrivilegeType.WRITE,
                this.authorization.getUserService().getCurrentUser().getUserInfo());
    }

    @Override
    protected Result<M> checkCreateAccess(final M entity) {
        throw new PermissionDeniedException(
                getGrantEntityType(),
                PrivilegeType.WRITE,
                this.authorization.getUserService().getCurrentUser().getUserInfo());
    }

}
