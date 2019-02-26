/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;

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

    public static List<UserRole> publicRolesForUser(final UserInfo user) {
        final EnumSet<UserRole> roles = user.getUserRoles();
        if (roles.contains(SEB_SERVER_ADMIN)) {
            return Arrays.asList(UserRole.values());
        } else if (roles.contains(INSTITUTIONAL_ADMIN)) {
            return Arrays.asList(INSTITUTIONAL_ADMIN, EXAM_ADMIN, EXAM_SUPPORTER);
        } else if (roles.contains(EXAM_ADMIN)) {
            return Arrays.asList(EXAM_ADMIN, EXAM_SUPPORTER);
        } else if (roles.contains(EXAM_SUPPORTER)) {
            return Arrays.asList(EXAM_SUPPORTER);
        } else {
            return Collections.emptyList();
        }
    }
}
