/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api.authorization;

import java.util.Arrays;

/** Defines SEB-Server internal privilege types **/
public enum PrivilegeType {
    /** No privilege type at all (placeholder) */
    NONE(0, 'n'),
    /** Only assigned entity privileges for the specific entity type. This is used as a marker to indicate that
     * the user has no overall entity type privileges but might have assigned entity privileges. */
    ASSIGNED(1, 'a'),
    /** The read privilege type for read access */
    READ( 2, 'r'),
    /** The modify privilege type includes read-only type privilege plus privilege for editing right but without create
     * and delete rights */
    MODIFY(3, 'm'),
    /** The write privilege type including modify privilege type plus creation and deletion rights */
    WRITE(4, 'w');

    public final byte key;
    public final char code;

    PrivilegeType(final int key, final char code) {
        this.key = (byte) key;
        this.code = code;
    }

    public static PrivilegeType byKey(final byte key) {
        return Arrays.stream(PrivilegeType.values()).filter(t -> t.key == key).findFirst().orElse(NONE);
    }

    public static PrivilegeType byCode(final char code) {
        return Arrays.stream(PrivilegeType.values()).filter(t -> t.code == code).findFirst().orElse(NONE);
    }

    /** Use this to check implicit privilege.
     * <p>
     * Implicit in this case means: if the privilegeType is of type PrivilegeType.WRITE,
     * PrivilegeType.MODIFY and PrivilegeType.READ are implicitly included.
     * If the privilegeType is of type PrivilegeType.MODIFY, the PrivilegeType.READ are implicitly included
     * and so on.
     *
     * @param type the PrivilegeType
     * @return true if given PrivilegeType is implicit from this PrivilegeType */
    public boolean hasImplicit(final PrivilegeType type) {
        if (type == null) {
            return false;
        }

        switch (this) {
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
