/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

/** A privilege consisting of a overall grant type, a institutional grant type and a owner grant type.
 *
 * The overallGrantType defines the grant type independent of an institutional relation as well as an owner
 * relation. The institutionalGrantType defines the grant type specific to the institutional relation of an entity.
 * And the ownerGrantType defines the grant type specific to the ownership of an entity
 *
 * For example with a privilege of:
 * overallGrantType = READ_ONLY
 * institutionalGrantType = MODIFY
 * ownerGrantType = WRITE
 *
 * A user with such a privilege is granted to see all type of specified entities independent of institutional relation
 * or ownership, is able to modify all type of specified entities within its own institution and is able to create or
 * delete owned entities. */
public final class Privilege {

    public final GrantType overallGrantType;
    public final GrantType institutionalGrantType;
    public final GrantType ownerGrantType;

    public Privilege(
            final GrantType overallGrantType,
            final GrantType institutionalGrantType,
            final GrantType ownerGrantType) {

        this.overallGrantType = overallGrantType;
        this.institutionalGrantType = institutionalGrantType;
        this.ownerGrantType = ownerGrantType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.institutionalGrantType == null) ? 0 : this.institutionalGrantType.hashCode());
        result = prime * result + ((this.overallGrantType == null) ? 0 : this.overallGrantType.hashCode());
        result = prime * result + ((this.ownerGrantType == null) ? 0 : this.ownerGrantType.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Privilege other = (Privilege) obj;
        if (this.institutionalGrantType != other.institutionalGrantType)
            return false;
        if (this.overallGrantType != other.overallGrantType)
            return false;
        if (this.ownerGrantType != other.ownerGrantType)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Privilege [overallGrantType=" + this.overallGrantType + ", institutionalGrantType="
                + this.institutionalGrantType
                + ", ownerGrantType=" + this.ownerGrantType + "]";
    }

}
