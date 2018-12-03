/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.model.Entity;

/** Interface of an entity that needs a grant for access.
 * Such an entity usually has an institution association and optionally an owner association */
public interface GrantEntity extends Entity {

    /** The institution association of a GrantEntity. This is the data-base identifier (PK)
     * of the institution entity associated within this entity.
     *
     * @return The institution association of a GrantEntity */
    Long institutionId();

    /** The institution association of a GrantEntity. This is the UUID of the owner-user
     *
     * @return The institution association of a GrantEntity */
    String ownerUUID();

}
