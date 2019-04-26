/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api.authorization;

/** Defines SEB-Server internal privilege types **/
public enum PrivilegeType {
    /** No privilege type at all (placeholder) */
    NONE,
    /** The read privilege type for read access */
    READ,
    /** The modify privilege type includes read-only type privilege plus privilege for editing right but without create
     * and delete
     * rights */
    MODIFY,
    /** The write privilege type including modify privilege type plus creation and deletion rights */
    WRITE;

    /** Use this to check implicit privilege.
     *
     * Implicit in this case means: if the privilegeType is of type PrivilegeType.WRITE,
     * PrivilegeType.MODIFY and PrivilegeType.READ are implicitly included.
     * If the privilegeType is of type PrivilegeType.MODIFY, the PrivilegeType.READ are implicitly included
     * and so on.
     *
     * @param type the PrivilegeType
     * @return true if given PrivilegeType is implicit form this PrivilegeType */
    public boolean hasImplicit(final PrivilegeType type) {
        if (type == null) {
            return false;
        }

        switch (this) {
            case NONE:
                return false;
            case READ:
                return type == READ;
            case MODIFY:
                return type == READ || type == MODIFY;
            case WRITE:
                return type == READ || type == MODIFY || type == WRITE;
            default:
                return false;
        }
    }
}
