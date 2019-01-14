/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import javax.validation.constraints.NotNull;

public class EntityKey {

    public final String entityId;
    public final EntityType entityType;
    public final boolean isIdPK;

    public EntityKey(
            @NotNull final Long entityId,
            @NotNull final EntityType entityType) {

        this.entityId = String.valueOf(entityId);
        this.entityType = entityType;
        this.isIdPK = true;
    }

    public EntityKey(
            @NotNull final String entityId,
            @NotNull final EntityType entityType,
            final boolean isIdPK) {

        this.entityId = entityId;
        this.entityType = entityType;
        this.isIdPK = isIdPK;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.entityId == null) ? 0 : this.entityId.hashCode());
        result = prime * result + ((this.entityType == null) ? 0 : this.entityType.hashCode());
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
        final EntityKey other = (EntityKey) obj;
        if (this.entityId == null) {
            if (other.entityId != null)
                return false;
        } else if (!this.entityId.equals(other.entityId))
            return false;
        if (this.entityType != other.entityType)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EntityKey [entityId=" + this.entityId + ", entityType=" + this.entityType + "]";
    }

}
