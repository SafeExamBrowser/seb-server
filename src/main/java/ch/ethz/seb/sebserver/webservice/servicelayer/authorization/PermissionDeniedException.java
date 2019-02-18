/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;

public class PermissionDeniedException extends RuntimeException {

    private static final long serialVersionUID = 5333137812363042580L;

    public final EntityType entityType;
    public final GrantEntity entity;
    public final PrivilegeType grantType;
    public final String userId;

    public PermissionDeniedException(
            final EntityType entityType,
            final PrivilegeType grantType,
            final String userId) {

        super("No grant: " + grantType + " on type: " + entityType + " for user: " + userId);
        this.entityType = entityType;
        this.entity = null;
        this.grantType = grantType;
        this.userId = userId;
    }

    public PermissionDeniedException(
            final GrantEntity entity,
            final PrivilegeType grantType,
            final String userId) {

        super("No grant: " + grantType +
                " on type: " + entity.entityType() +
                " entity institution: " + entity.getInstitutionId() +
                " entity owner: " + entity.getOwnerId() +
                " for user: " + userId);
        this.entityType = entity.entityType();
        this.entity = entity;
        this.grantType = grantType;
        this.userId = userId;
    }

}
