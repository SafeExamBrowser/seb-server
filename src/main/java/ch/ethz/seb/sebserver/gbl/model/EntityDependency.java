/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityDependency implements Comparable<EntityDependency>, ModelIdAware {

    public static final String ATTR_PARENT = "parent";
    public static final String ATTR_SELF = "self";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_DESCRIPTION = "description";

    @JsonProperty(value = ATTR_PARENT, required = true)
    public final EntityKey parent;
    @JsonProperty(value = ATTR_SELF, required = true)
    public final EntityKey self;
    @JsonProperty(value = ATTR_NAME)
    public final String name;
    @JsonProperty(ATTR_DESCRIPTION)
    public final String description;

    public EntityDependency(
            @JsonProperty(value = ATTR_PARENT, required = true) final EntityKey parent,
            @JsonProperty(value = ATTR_SELF, required = true) final EntityKey self,
            @JsonProperty(value = ATTR_NAME, required = true) final String name,
            @JsonProperty(ATTR_DESCRIPTION) final String description) {

        this.parent = parent;
        this.self = self;
        this.name = name;
        this.description = description;
    }

    public EntityKey getParent() {
        return this.parent;
    }

    public EntityKey getSelf() {
        return this.self;
    }

    @Override
    public String getModelId() {
        return this.self.modelId;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.parent == null) ? 0 : this.parent.hashCode());
        result = prime * result + ((this.self == null) ? 0 : this.self.hashCode());
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
        final EntityDependency other = (EntityDependency) obj;
        if (this.parent == null) {
            if (other.parent != null)
                return false;
        } else if (!this.parent.equals(other.parent))
            return false;
        if (this.self == null) {
            if (other.self != null)
                return false;
        } else if (!this.self.equals(other.self))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EntityDependency [parent=");
        builder.append(this.parent);
        builder.append(", self=");
        builder.append(this.self);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int compareTo(final EntityDependency other) {
        if (other == null) {
            return -1;
        }

        final int compareTo = this.self.entityType.name().compareTo(other.self.entityType.name());
        if (compareTo == 0) {
            return this.self.modelId.compareTo(other.self.modelId);
        } else {
            return compareTo;
        }
    }

}
