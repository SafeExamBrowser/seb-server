/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;

public interface Entity extends ModelIdAware {

    public static final String FILTER_ATTR_INSTITUTION = API.PARAM_INSTITUTION_ID;
    public static final String FILTER_ATTR_ACTIVE = "active";
    public static final String FILTER_ATTR_NAME = "name";

    @JsonIgnore
    EntityType entityType();

    @JsonIgnore
    String getName();

    @JsonIgnore
    default EntityKey getEntityKey() {
        return new EntityKey(getModelId(), entityType());
    }

    public static EntityName toName(final Entity entity) {
        return new EntityName(
                entity.entityType(),
                entity.getModelId(),
                entity.getName());
    }

}
