/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Data class that represents a entity processing report JSON of the SEB Server API.
 * This report is many used on bulk-action and defines the entity-keys of processing, entity-keys of all entities that
 * has dependencies to the given processing entities and a list of error entries that describes
 * errors if happened. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityProcessingReport {

    public static final String ATTR_SOURCE = "source";
    public static final String ATTR_RESULTS = "results";
    public static final String ATTR_ERRORS = "errors";
    public static final String ATTR_TYPE = "bulkActionType";

    /** A set of entity-keys that are or were processed by a bulk action- or other process with a EntityProcessingReport
     * result. */
    @JsonProperty(value = ATTR_SOURCE, required = true)
    public final Set<EntityKey> source;
    /** A set of entity-keys for all entities that has been detected as dependency to one or more of the source entities
     * during the process */
    @JsonProperty(value = ATTR_RESULTS, required = true)
    public final Set<EntityKey> results;
    /** A set of error entries that defines an error if happened. */
    @JsonProperty(value = ATTR_ERRORS, required = true)
    public final Set<ErrorEntry> errors;
    @JsonProperty(value = ATTR_TYPE, required = true)
    public final BulkActionType bulkActionType;

    @JsonCreator
    public EntityProcessingReport(
            @JsonProperty(value = ATTR_SOURCE, required = true) final Collection<EntityKey> source,
            @JsonProperty(value = ATTR_RESULTS, required = true) final Collection<EntityKey> results,
            @JsonProperty(value = ATTR_ERRORS, required = true) final Collection<ErrorEntry> errors,
            @JsonProperty(value = ATTR_TYPE, required = true) final BulkActionType bulkActionType) {

        this.source = Utils.immutableSetOf(source);
        this.results = Utils.immutableSetOf(results);
        this.errors = Utils.immutableSetOf(errors);
        this.bulkActionType = bulkActionType;
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

    public Set<EntityKey> getResults() {
        return this.results;
    }

    public Set<ErrorEntry> getErrors() {
        return this.errors;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ErrorEntry {

        @JsonProperty(value = "entity_key", required = false)
        public final EntityKey entityKey;
        @JsonProperty(value = "error_message", required = true)
        public final APIMessage errorMessage;

        @JsonCreator
        public ErrorEntry(
                @JsonProperty(value = "entity_key", required = false) final EntityKey entityKey,
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
        builder.append(", results=");
        builder.append(this.results);
        builder.append(", errors=");
        builder.append(this.errors);
        builder.append("]");
        return builder.toString();
    }

    public static EntityProcessingReport ofEmptyError() {
        return new EntityProcessingReport(
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(new ErrorEntry(null, APIMessage.ErrorMessage.RESOURCE_NOT_FOUND.of())),
                null);
    }

}
