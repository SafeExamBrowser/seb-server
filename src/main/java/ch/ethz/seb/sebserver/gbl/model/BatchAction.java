/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.BATCH_ACTION;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class BatchAction implements GrantEntity {

    public static final String ATTR_FAILURES = "failures";
    public static final String FINISHED_FLAG = "_FINISHED";
    public static final String ACTION_ATTRIBUT_TARGET_STATE = "batchActionTargetState";

    private static final Set<String> ACTION_ATTRIBUTES = new HashSet<>(Arrays.asList(
            ACTION_ATTRIBUT_TARGET_STATE));

    @JsonProperty(BATCH_ACTION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_OWNER)
    public final String ownerId;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_ACTION_TYPE)
    public final BatchActionType actionType;

    @JsonProperty(BATCH_ACTION.ATTR_ATTRIBUTES)
    public final Map<String, String> attributes;

    @NotNull
    @JsonProperty(BATCH_ACTION.ATTR_SOURCE_IDS)
    public final Collection<String> sourceIds;

    @JsonProperty(BATCH_ACTION.ATTR_SUCCESSFUL)
    public final Collection<String> successful;

    @JsonProperty(BATCH_ACTION.ATTR_LAST_UPDATE)
    private final Long lastUpdate;

    @JsonProperty(BATCH_ACTION.ATTR_PROCESSOR_ID)
    public final String processorId;

    @JsonProperty(ATTR_FAILURES)
    public final Map<String, APIMessage> failures;

    public BatchAction(
            @JsonProperty(BATCH_ACTION.ATTR_ID) final Long id,
            @JsonProperty(BATCH_ACTION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(BATCH_ACTION.ATTR_OWNER) final String ownerId,
            @JsonProperty(BATCH_ACTION.ATTR_ACTION_TYPE) final BatchActionType actionType,
            @JsonProperty(BATCH_ACTION.ATTR_ATTRIBUTES) final Map<String, String> attributes,
            @JsonProperty(BATCH_ACTION.ATTR_SOURCE_IDS) final Collection<String> sourceIds,
            @JsonProperty(BATCH_ACTION.ATTR_SUCCESSFUL) final Collection<String> successful,
            @JsonProperty(BATCH_ACTION.ATTR_LAST_UPDATE) final Long lastUpdate,
            @JsonProperty(BATCH_ACTION.ATTR_PROCESSOR_ID) final String processorId,
            @JsonProperty(ATTR_FAILURES) final Map<String, APIMessage> failures) {

        super();
        this.id = id;
        this.institutionId = institutionId;
        this.ownerId = ownerId;
        this.actionType = actionType;
        this.attributes = Utils.immutableMapOf(attributes);
        this.sourceIds = Utils.immutableCollectionOf(sourceIds);
        this.successful = Utils.immutableCollectionOf(successful);
        this.lastUpdate = lastUpdate;
        this.processorId = processorId;
        this.failures = Utils.immutableMapOf(failures);
    }

    public BatchAction(final String modelId, final String ownerId, final POSTMapper postMap) {

        super();
        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.institutionId = postMap.getLong(BATCH_ACTION.ATTR_INSTITUTION_ID);
        this.ownerId = ownerId;
        this.actionType = postMap.getEnum(BATCH_ACTION.ATTR_ACTION_TYPE, BatchActionType.class);
        this.attributes = postMap.getSubMap(ACTION_ATTRIBUTES);
        this.sourceIds = Utils.immutableListOf(StringUtils.split(
                postMap.getString(BATCH_ACTION.ATTR_SOURCE_IDS),
                Constants.LIST_SEPARATOR));
        this.successful = Collections.emptyList();
        this.lastUpdate = null;
        this.processorId = null;
        this.failures = Collections.emptyMap();
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

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return this.ownerId;
    }

    public BatchActionType getActionType() {
        return this.actionType;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
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

    public Map<String, APIMessage> getFailures() {
        return this.failures;
    }

    @JsonIgnore
    public int getProgress() {
        return 100 / this.sourceIds.size() * (this.successful.size() + this.failures.size());
    }

    @JsonIgnore
    public boolean isFinished() {
        return this.processorId != null && this.processorId.contains(FINISHED_FLAG);
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
        builder.append(", attributes=");
        builder.append(this.attributes);
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
