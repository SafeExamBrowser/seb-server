/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.model.EntityType;

public interface AuthorizationGrantRule {

    EntityType entityType();

    boolean hasGrant(GrantEntity entity, SEBServerUser user, GrantType grantType);

//    boolean hasReadGrant(GrantEntity entity, SEBServerUser user);
//
//    boolean hasModifyGrant(GrantEntity entity, SEBServerUser user);
//
//    boolean hasWriteGrant(GrantEntity entity, SEBServerUser user);

}
