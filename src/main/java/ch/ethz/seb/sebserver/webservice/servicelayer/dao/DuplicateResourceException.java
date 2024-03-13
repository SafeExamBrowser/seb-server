/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;

public class DuplicateResourceException extends RuntimeException {

    private static final long serialVersionUID = 2935680103812281185L;

    /** The entity key of the resource that was requested */
    public final EntityKey entityKey;

    public DuplicateResourceException(final EntityType entityType, final String modelId) {
        super("Resource " + entityType + " with ID: " + modelId + " already exists");
        this.entityKey = new EntityKey(modelId, entityType);
    }

    public DuplicateResourceException(final EntityType entityType, final String modelId, final Throwable cause) {
        super("Resource " + entityType + " with ID: " + modelId + " not found", cause);
        this.entityKey = new EntityKey(modelId, entityType);
    }

}
