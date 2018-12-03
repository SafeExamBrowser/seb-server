/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

/** Defines SEB-Server internal grant types **/
public enum GrantType {
    /** No grant type at all (placeholder) */
    NONE,
    /** The read-only grant type for read access */
    READ_ONLY,
    /** The modify grant type includes read-only type grant plus grant for editing right but without create and delete
     * rights */
    MODIFY,
    /** The write grant type including modify grant type plus creation and deletion rights */
    WRITE;

    public boolean hasImplicit(final GrantType type) {
        if (type == null) {
            return false;
        }

        switch (this) {
            case NONE:
                return false;
            case READ_ONLY:
                return type == READ_ONLY;
            case MODIFY:
                return type == READ_ONLY || type == MODIFY;
            case WRITE:
                return type == READ_ONLY || type == MODIFY || type == WRITE;
            default:
                return false;
        }
    }
}
