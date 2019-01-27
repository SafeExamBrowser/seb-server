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
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class EntityProcessingReport {

    @JsonProperty(value = "source", required = true)
    public final Set<EntityKey> source;
    @JsonProperty(value = "dependencies", required = true)
    public final Set<EntityKey> dependencies;
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

    }

    @Override
    public String toString() {
        return "EntityProcessingReport [source=" + this.source + ", dependencies=" + this.dependencies + ", errors="
                + this.errors
                + "]";
    }

}
