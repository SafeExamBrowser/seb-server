/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.BATCH_ACTION;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class BatchAction implements Entity {

    @JsonProperty(BATCH_ACTION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_ACTION_TYPE)
    public final BatchActionType actionType;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_SOURCE_IDS)
    public final Collection<String> sourceIds;

    @JsonProperty(BATCH_ACTION.ATTR_SUCCESSFUL)
    public final Collection<String> successful;

    @JsonProperty(BATCH_ACTION.ATTR_LAST_UPDATE)
    private final Long lastUpdate;

    @JsonProperty(BATCH_ACTION.ATTR_PROCESSOR_ID)
    public final String processorId;

    public BatchAction(
            @JsonProperty(BATCH_ACTION.ATTR_ID) final Long id,
            @JsonProperty(BATCH_ACTION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(BATCH_ACTION.ATTR_ACTION_TYPE) final BatchActionType actionType,
            @JsonProperty(BATCH_ACTION.ATTR_SOURCE_IDS) final Collection<String> sourceIds,
            @JsonProperty(BATCH_ACTION.ATTR_SUCCESSFUL) final Collection<String> successful,
            @JsonProperty(BATCH_ACTION.ATTR_LAST_UPDATE) final Long lastUpdate,
            @JsonProperty(BATCH_ACTION.ATTR_PROCESSOR_ID) final String processorId) {

        super();
        this.id = id;
        this.institutionId = institutionId;
        this.actionType = actionType;
        this.sourceIds = Utils.immutableCollectionOf(sourceIds);
        this.successful = Utils.immutableCollectionOf(successful);
        this.lastUpdate = lastUpdate;
        this.processorId = processorId;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.BATCH_ACTION;
    }

    @Override
    public String getName() {
        return this.actionType.name();
    }

    public Long getId() {
        return this.id;
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public BatchActionType getActionType() {
        return this.actionType;
    }

    public Collection<String> getSourceIds() {
        return this.sourceIds;
    }

    public Collection<String> getSuccessful() {
        return this.successful;
    }

    public Long getLastUpdate() {
        return this.lastUpdate;
    }

    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
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
        final BatchAction other = (BatchAction) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BatchAction [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", actionType=");
        builder.append(this.actionType);
        builder.append(", sourceIds=");
        builder.append(this.sourceIds);
        builder.append(", successful=");
        builder.append(this.successful);
        builder.append(", lastUpdate=");
        builder.append(this.lastUpdate);
        builder.append(", processorId=");
        builder.append(this.processorId);
        builder.append("]");
        return builder.toString();
    }

}
