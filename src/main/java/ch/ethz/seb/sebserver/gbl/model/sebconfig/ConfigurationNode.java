/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_NODE;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationNode implements GrantEntity, Activatable {

    public static final String FILTER_ATTR_DESCRIPTION = "description";
    public static final String FILTER_ATTR_TYPE = "type";
    public static final String FILTER_ATTR_TEMPLATE = "template";

    public enum ConfigurationType {
        TEMPLATE,
        EXAM_CONFIG
    }

    @JsonProperty(CONFIGURATION_NODE.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(CONFIGURATION_NODE.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "configurationNode:name:notNull")
    @Size(min = 3, max = 255, message = "configurationNode:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_NODE.ATTR_NAME)
    public final String name;

    @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION)
    public final String description;

    @NotNull
    @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE)
    public final ConfigurationType type;

    @JsonProperty(CONFIGURATION_NODE.ATTR_TEMPLATE)
    public final String templateName;

    @JsonProperty(CONFIGURATION_NODE.ATTR_OWNER)
    public final String owner;

    /** Indicates whether this Configuration is active or not */
    @JsonProperty(CONFIGURATION_NODE.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public ConfigurationNode(
            @JsonProperty(CONFIGURATION_NODE.ATTR_ID) final Long id,
            @JsonProperty(CONFIGURATION_NODE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CONFIGURATION_NODE.ATTR_NAME) final String name,
            @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION) final String description,
            @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE) final ConfigurationType type,
            @JsonProperty(CONFIGURATION_NODE.ATTR_TEMPLATE) final String templateName,
            @JsonProperty(CONFIGURATION_NODE.ATTR_OWNER) final String owner,
            @JsonProperty(CONFIGURATION_NODE.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.templateName = templateName;
        this.owner = owner;
        this.active = active;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_NODE;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return this.owner;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public ConfigurationType getType() {
        return this.type;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public String toString() {
        return "ConfigurationNode [id=" + this.id + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", description="
                + this.description + ", type=" + this.type + ", templateName=" + this.templateName + ", owner="
                + this.owner + ", active="
                + this.active + "]";
    }

}
