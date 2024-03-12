/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_VALUE;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationValue implements GrantEntity {

    public static final String FILTER_ATTR_CONFIGURATION_ID = "configurationId";
    public static final String FILTER_ATTR_CONFIGURATION_ATTRIBUTE_ID = "attributeId";

    @JsonProperty(CONFIGURATION_VALUE.ATTR_ID)
    public final Long id;

    @JsonProperty(CONFIGURATION_VALUE.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID)
    public final Long configurationId;

    @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID)
    public final Long attributeId;

    @JsonProperty(CONFIGURATION_VALUE.ATTR_LIST_INDEX)
    public final Integer listIndex;

    @JsonProperty(CONFIGURATION_VALUE.ATTR_VALUE)
    public final String value;

    @JsonCreator
    public ConfigurationValue(
            @JsonProperty(CONFIGURATION_VALUE.ATTR_ID) final Long id,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID) final Long configurationId,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID) final Long attributeId,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_LIST_INDEX) final Integer listIndex,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_VALUE) final String value) {

        this.id = id;
        this.institutionId = institutionId;
        this.configurationId = configurationId;
        this.attributeId = attributeId;
        this.listIndex = listIndex;
        this.value = value;
    }

    public ConfigurationValue(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        this.configurationId = postParams.getLong(Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID);
        this.attributeId = postParams.getLong(Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID);
        this.listIndex = postParams.getInteger(Domain.CONFIGURATION_VALUE.ATTR_LIST_INDEX);
        this.value = postParams.getString(Domain.CONFIGURATION_VALUE.ATTR_VALUE);
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_VALUE;
    }

    @Override
    public String getName() {
        return getModelId();
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getConfigurationId() {
        return this.configurationId;
    }

    public long getAttributeId() {
        return this.attributeId;
    }

    public int getListIndex() {
        return this.listIndex;
    }

    public String getValue() {
        return this.value;
    }

    public ConfigurationValue copyOf(final Long institutionId, final Long configurationId2) {
        return new ConfigurationValue(
                this.id,
                institutionId,
                configurationId2,
                this.attributeId,
                this.listIndex,
                this.value);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConfigurationValue [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", configurationId=");
        builder.append(this.configurationId);
        builder.append(", attributeId=");
        builder.append(this.attributeId);
        builder.append(", listIndex=");
        builder.append(this.listIndex);
        builder.append(", value=");
        builder.append(this.value);
        builder.append("]");
        return builder.toString();
    }

}
