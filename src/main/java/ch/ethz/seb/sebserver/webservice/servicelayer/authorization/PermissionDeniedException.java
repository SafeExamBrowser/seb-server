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
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

public class PermissionDeniedException extends RuntimeException {

    private static final long serialVersionUID = 5333137812363042580L;

    /** The EntityType of the denied permission check */
    public final EntityType entityType;
    /** The PrivilegeType of the denied permission check */
    public final PrivilegeType privilegeType;
    /** The user identifier of the denied permission check */
    public final String userId;

    public PermissionDeniedException(
            final EntityType entityType,
            final PrivilegeType grantType,
            final String userId) {

        super("No grant: " + grantType + " on type: " + entityType + " for user: " + userId);
        this.entityType = entityType;
        this.privilegeType = grantType;
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
        this.privilegeType = grantType;
        this.userId = userId;
    }

}
