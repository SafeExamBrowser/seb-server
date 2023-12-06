package ch.ethz.seb.sebserver.gbl.api.authorization;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A key that combines UserRole EntityType identity
 */
public final class RoleTypeKey {

    @JsonProperty("entityType")
    public final EntityType entityType;
    @JsonProperty("userRole")
    public final UserRole userRole;

    @JsonCreator
    public RoleTypeKey(
            @JsonProperty("entityType") final EntityType type,
            @JsonProperty("userRole") final UserRole role) {

        this.entityType = type;
        this.userRole = role;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.userRole == null) ? 0 : this.userRole.hashCode());
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
        final RoleTypeKey other = (RoleTypeKey) obj;
        if (this.userRole != other.userRole)
            return false;
        if (this.entityType != other.entityType)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RoleTypeKey [entityType=" + this.entityType + ", userRole=" + this.userRole + "]";
    }
}
