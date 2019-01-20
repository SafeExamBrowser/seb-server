/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;

public abstract class EntityController<T extends GrantEntity> {

    protected final AuthorizationGrantService authorizationGrantService;
    protected final BulkActionService bulkActionService;
    protected final EntityDAO<T> entityDAO;

    protected EntityController(
            final AuthorizationGrantService authorizationGrantService,
            final BulkActionService bulkActionService,
            final EntityDAO<T> entityDAO) {

        this.authorizationGrantService = authorizationGrantService;
        this.bulkActionService = bulkActionService;
        this.entityDAO = entityDAO;
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public T accountInfo(@PathVariable final String id) {
        return this.entityDAO
                .byModelId(id)
                .flatMap(entity -> this.authorizationGrantService.checkGrantOnEntity(
                        entity,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();
    }

    @RequestMapping(path = "/{id}/hard-delete", method = RequestMethod.DELETE)
    public EntityProcessingReport hardDeleteUser(@PathVariable final String id) {
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

}
