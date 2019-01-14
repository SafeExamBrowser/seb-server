/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityType;

/** Defines the possible user roles of SEB Server users. */
public enum UserRole implements Entity {
    SEB_SERVER_ADMIN,
    INSTITUTIONAL_ADMIN,
    EXAM_ADMIN,
    EXAM_SUPPORTER;

    @Override
    public EntityType entityType() {
        return EntityType.USER_ROLE;
    }

    @Override
    public String getModelId() {
        return name();
    }

    @Override
    public String getName() {
        return name();
    }
}
