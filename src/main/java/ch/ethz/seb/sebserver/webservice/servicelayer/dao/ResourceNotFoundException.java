/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;

@ResponseStatus(HttpStatus.NOT_FOUND)
public final class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 8319235723086949618L;

    public final EntityKey entityKey;

    public ResourceNotFoundException(final EntityType entityType, final String modelId) {
        super("Resource " + entityType + " with ID: " + modelId + " not found");
        this.entityKey = new EntityKey(modelId, entityType);
    }

    public ResourceNotFoundException(final EntityType entityType, final String modelId, final Throwable cause) {
        super("Resource " + entityType + " with ID: " + modelId + " not found", cause);
        this.entityKey = new EntityKey(modelId, entityType);
    }

}
