/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_NODE;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;

public final class ConfigCreationInfo implements Entity {

    public static final String ATTR_COPY_WITH_HISTORY = "with-history";

    @NotNull
    @JsonProperty(CONFIGURATION_NODE.ATTR_ID)
    public final Long configurationNodeId;

    @NotNull(message = "configurationNode:name:notNull")
    @Size(min = 3, max = 255, message = "configurationNode:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_NODE.ATTR_NAME)
    public final String name;

    @Size(max = 4000, message = "configurationNode:description:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION)
    public final String description;

    @JsonProperty(ATTR_COPY_WITH_HISTORY)
    public final Boolean withHistory;

    @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE)
    public final ConfigurationType configurationType;

    public ConfigCreationInfo(
            @JsonProperty(CONFIGURATION_NODE.ATTR_ID) final Long configurationNodeId,
            @JsonProperty(CONFIGURATION_NODE.ATTR_NAME) final String name,
            @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION) final String description,
            @JsonProperty(ATTR_COPY_WITH_HISTORY) final Boolean withHistory,
            @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE) final ConfigurationType configurationType) {

        this.configurationNodeId = configurationNodeId;
        this.name = name;
        this.description = description;
        this.withHistory = withHistory;
        this.configurationType = (configurationType != null)
                ? configurationType
                : ConfigurationType.EXAM_CONFIG;
    }

    public Long getConfigurationNodeId() {
        return this.configurationNodeId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Boolean getWithHistory() {
        return this.withHistory;
    }

    public ConfigurationType getConfigurationType() {
        return this.configurationType;
    }

    @Override
    public String getModelId() {
        return (this.configurationNodeId != null)
                ? String.valueOf(this.configurationNodeId)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_NODE;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConfigCopyInfo [configurationNodeId=");
        builder.append(this.configurationNodeId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append(", withHistory=");
        builder.append(this.withHistory);
        builder.append(", configurationType=");
        builder.append(this.configurationType);
        builder.append("]");
        return builder.toString();
    }

}
