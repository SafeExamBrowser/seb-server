/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_VALUE;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationTableValues implements GrantEntity {

    public static final String ATTR_TABLE_VALUES = "tableValues";

    @NotNull
    @JsonProperty(CONFIGURATION_VALUE.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID)
    public final Long configurationId;

    @NotNull
    @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID)
    public final Long attributeId;

    @JsonProperty(ATTR_TABLE_VALUES)
    public final List<TableValue> values;

    @JsonCreator
    public ConfigurationTableValues(
            @JsonProperty(CONFIGURATION_VALUE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID) final Long configurationId,
            @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID) final Long attributeId,
            @JsonProperty(ATTR_TABLE_VALUES) final List<TableValue> values) {

        this.institutionId = institutionId;
        this.configurationId = configurationId;
        this.attributeId = attributeId;
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_VALUE;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getModelId() {
        return null;
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

    public List<TableValue> getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConfigurationTableValues [institutionId=");
        builder.append(this.institutionId);
        builder.append(", configurationId=");
        builder.append(this.configurationId);
        builder.append(", attributeId=");
        builder.append(this.attributeId);
        builder.append(", values=");
        builder.append(this.values);
        builder.append("]");
        return builder.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TableValue {

        @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID)
        public final Long attributeId;
        @JsonProperty(CONFIGURATION_VALUE.ATTR_LIST_INDEX)
        public final Integer listIndex;
        @JsonProperty(CONFIGURATION_VALUE.ATTR_VALUE)
        public final String value;

        public TableValue(
                @JsonProperty(CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID) final Long attributeId,
                @JsonProperty(CONFIGURATION_VALUE.ATTR_LIST_INDEX) final Integer listIndex,
                @JsonProperty(CONFIGURATION_VALUE.ATTR_VALUE) final String value) {

            this.attributeId = attributeId;
            this.listIndex = listIndex;
            this.value = value;
        }

        public static TableValue of(final ConfigurationValue value) {
            return new TableValue(value.attributeId, value.listIndex, value.value);
        }
    }
}
