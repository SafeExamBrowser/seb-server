/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

public interface Entity extends ModelIdAware {

    public static final String ATTR_ID = "id";
    public static final String ATTR_INSTITUTION = "institution";
    public static final String ATTR_ACTIVE = "active";

    EntityType entityType();

    String getName();

    public static EntityKeyAndName toName(final Entity entity) {
        return new EntityKeyAndName(
                entity.entityType(),
                entity.getModelId(),
                entity.getName());
    }

}
