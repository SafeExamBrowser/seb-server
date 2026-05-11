package ch.ethz.seb.sebserver.gbl.api.authorization;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A key that combines UserRole EntityType identity
 */
public record RoleTypeKey (
        @JsonProperty("entityType") EntityType entityType,
        @JsonProperty("userRole") UserRole userRole
) { }