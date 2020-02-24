/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Data class that represents a entity processing report JSON of the SEB Server API.
 * This report is many used on bulk-action and defines the entity-keys of processing, entity-keys of all entities that
 * has dependencies to the given processing entities and a list of error entries that describes
 * errors if happened. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityProcessingReport {

    /** A set of entity-keys that are or were processed by a bulk action- or other process with a EntityProcessingReport
     * result. */
    @JsonProperty(value = "source", required = true)
    public final Set<EntityKey> source;
    /** A set of entity-keys for all entities that has been detected as dependency to one or more of the source entities
     *  during the process */
    @JsonProperty(value = "dependencies", required = true)
    public final Set<EntityKey> dependencies;
    /** A set of error entries that defines an error if happened. */
    @JsonProperty(value = "errors", required = true)
    public final Set<ErrorEntry> errors;

    @JsonCreator
    public EntityProcessingReport(
            @JsonProperty(value = "source", required = true) final Collection<EntityKey> source,
            @JsonProperty(value = "dependencies", required = true) final Collection<EntityKey> dependencies,
            @JsonProperty(value = "errors", required = true) final Collection<ErrorEntry> errors) {

        this.source = Utils.immutableSetOf(source);
        this.dependencies = Utils.immutableSetOf(dependencies);
        this.errors = Utils.immutableSetOf(errors);
    }

    @JsonIgnore
    public EntityKey getSingleSource() {
        if (!this.source.isEmpty()) {
            return this.source.iterator().next();
        }

        return null;
    }

    public Set<EntityKey> getSource() {
        return this.source;
    }

    public Set<EntityKey> getDependencies() {
        return this.dependencies;
    }

    public Set<ErrorEntry> getErrors() {
        return this.errors;
    }

    public static final class ErrorEntry {

        public final EntityKey entityKey;
        public final APIMessage errorMessage;

        @JsonCreator
        public ErrorEntry(
                @JsonProperty(value = "entity_key", required = true) final EntityKey entityKey,
                @JsonProperty(value = "error_message", required = true) final APIMessage errorMessage) {

            this.entityKey = entityKey;
            this.errorMessage = errorMessage;
        }

        public EntityKey getEntityKey() {
            return this.entityKey;
        }

        public APIMessage getErrorMessage() {
            return this.errorMessage;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EntityProcessingReport [source=");
        builder.append(this.source);
        builder.append(", dependencies=");
        builder.append(this.dependencies);
        builder.append(", errors=");
        builder.append(this.errors);
        builder.append("]");
        return builder.toString();
    }

}
