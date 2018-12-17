/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.model.EntityType;

/** Defines a authorization grant rule for a specified EntityType.
 *
 * If there is the need for a specialized authorization grant rule for a specified EntityType, just
 * create an implementation of this interface for a specified EntityType as a normal Spring Component
 * and the AuthorizationGrantService will automatically collect it on initialization and use it for
 * the specified EntityType instead of the default implementation. */
public interface AuthorizationGrantRule {

    /** The EntityType of the authorization grant rule implementation.
     * This is used by the AuthorizationGrantService on initialization.
     *
     * @return the authorization grant rule implementation */
    EntityType entityType();

    /** Implements a authorization grant rule check for a given entity, user and grant type.
     *
     * @param entity the GrantEntity instance to check the grant rule on
     * @param user the SEBServerUser instance to check the grant rule on
     * @param grantType the GrantType to check
     * @return true if a given user has a given grant-type on a given entity, false otherwise */
    boolean hasGrant(GrantEntity entity, SEBServerUser user, PrivilegeType grantType);

}
