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
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_NODE;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationNode implements GrantEntity {

    public static final Long DEFAULT_TEMPLATE_ID = 0L;

    public static final String FILTER_ATTR_TEMPLATE_ID = "templateId";
    public static final String FILTER_ATTR_DESCRIPTION = "description";
    public static final String FILTER_ATTR_TYPE = "type";
    public static final String FILTER_ATTR_STATUS = "status";

    public enum ConfigurationType {
        TEMPLATE,
        EXAM_CONFIG
    }

    public enum ConfigurationStatus {
        CONSTRUCTION,
        READY_TO_USE,
        IN_USE,
        ARCHIVED
    }

    @JsonProperty(CONFIGURATION_NODE.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(CONFIGURATION_NODE.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(CONFIGURATION_NODE.ATTR_TEMPLATE_ID)
    public final Long templateId;

    @NotNull(message = "configurationNode:name:notNull")
    @Size(min = 3, max = 255, message = "configurationNode:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_NODE.ATTR_NAME)
    public final String name;

    @Size(max = 4000, message = "configurationNode:description:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION)
    public final String description;

    @NotNull
    @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE)
    public final ConfigurationType type;

    @JsonProperty(CONFIGURATION_NODE.ATTR_OWNER)
    public final String owner;

    @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS)
    public final ConfigurationStatus status;

    @JsonCreator
    public ConfigurationNode(
            @JsonProperty(CONFIGURATION_NODE.ATTR_ID) final Long id,
            @JsonProperty(CONFIGURATION_NODE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CONFIGURATION_NODE.ATTR_TEMPLATE_ID) final Long templateId,
            @JsonProperty(CONFIGURATION_NODE.ATTR_NAME) final String name,
            @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION) final String description,
            @JsonProperty(CONFIGURATION_NODE.ATTR_TYPE) final ConfigurationType type,
            @JsonProperty(CONFIGURATION_NODE.ATTR_OWNER) final String owner,
            @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS) final ConfigurationStatus status) {

        this.id = id;
        this.institutionId = institutionId;
        this.templateId = (templateId != null) ? templateId : DEFAULT_TEMPLATE_ID;
        this.name = name;
        this.description = description;
        this.type = (type != null) ? type : ConfigurationType.EXAM_CONFIG;
        this.owner = owner;
        this.status = status;
    }

    public ConfigurationNode(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        final Long tplId = postParams.getLong(Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID);
        this.templateId = (tplId != null) ? tplId : DEFAULT_TEMPLATE_ID;
        this.name = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_NAME);
        this.description = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION);
        this.type = postParams.getEnum(
                Domain.CONFIGURATION_NODE.ATTR_TYPE,
                ConfigurationType.class,
                ConfigurationType.EXAM_CONFIG);
        this.owner = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_OWNER);
        this.status = postParams.getEnum(
                Domain.CONFIGURATION_NODE.ATTR_STATUS,
                ConfigurationStatus.class,
                ConfigurationStatus.CONSTRUCTION);
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

    public Long getTemplateId() {
        return this.templateId;
    }

    public ConfigurationStatus getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConfigurationNode [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", templateId=");
        builder.append(this.templateId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", owner=");
        builder.append(this.owner);
        builder.append(", status=");
        builder.append(this.status);
        builder.append("]");
        return builder.toString();
    }

    public static ConfigurationNode createNewExamConfig(final Long institutionId) {
        return new ConfigurationNode(
                null,
                institutionId,
                null,
                null,
                null,
                ConfigurationType.EXAM_CONFIG,
                null,
                ConfigurationStatus.CONSTRUCTION);
    }

    public static ConfigurationNode createNewTemplate(final Long institutionId) {
        return new ConfigurationNode(
                null,
                institutionId,
                null,
                null,
                null,
                ConfigurationType.TEMPLATE,
                null,
                ConfigurationStatus.CONSTRUCTION);
    }

}
