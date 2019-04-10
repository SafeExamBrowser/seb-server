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
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_ATTRIBUTE;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationAttribute implements Entity {

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_ID)
    public final Long id;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID)
    public final Long parentId;

    @NotNull(message = "configurationAttribute:name:notNull")
    @Size(min = 3, max = 255, message = "configurationAttribute:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_NAME)
    public final String name;

    @NotNull(message = "configurationAttribute:type:notNull")
    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_TYPE)
    public final AttributeType type;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES)
    public final String resources;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES)
    public final String dependencies;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE)
    public final String defaultValue;

    @JsonCreator
    public ConfigurationAttribute(
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_ID) final Long id,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID) final Long parentId,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String name,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_TYPE) final AttributeType type,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES) final String resources,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES) final String dependencies,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE) final String defaultValue) {

        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.type = type;
        this.resources = resources;
        this.dependencies = dependencies;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_ATTRIBUTE;
    }

    public Long getId() {
        return this.id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public AttributeType getType() {
        return this.type;
    }

    public String getResources() {
        return this.resources;
    }

    public String getDependencies() {
        return this.dependencies;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String toString() {
        return "ConfigurationAttribute [id=" + this.id + ", parentId=" + this.parentId + ", name=" + this.name
                + ", type=" + this.type
                + ", resources=" + this.resources + ", dependencies=" + this.dependencies + ", defaultValue="
                + this.defaultValue
                + "]";
    }

}
