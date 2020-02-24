/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.Comparator;
import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION;
import ch.ethz.seb.sebserver.gbl.model.Entity;

public final class TemplateAttribute implements Entity {

    public static final String ATTR_CONFIG_ATTRIBUTE = "configAttribute";
    public static final String ATTR_ORIENTATION = "orientation";

    public static final String FILTER_ATTR_VIEW = "view";
    public static final String FILTER_ATTR_GROUP = "group";
    public static final String FILTER_ATTR_TYPE = "type";

    @NotNull
    @JsonProperty(CONFIGURATION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(CONFIGURATION.ATTR_CONFIGURATION_NODE_ID)
    public final Long templateId;

    @NotNull
    @JsonProperty(ATTR_CONFIG_ATTRIBUTE)
    private final ConfigurationAttribute configAttribute;

    @JsonProperty(ATTR_ORIENTATION)
    private final Orientation orientation;

    public TemplateAttribute(
            @JsonProperty(CONFIGURATION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CONFIGURATION.ATTR_CONFIGURATION_NODE_ID) final Long templateId,
            @JsonProperty(ATTR_CONFIG_ATTRIBUTE) final ConfigurationAttribute configAttribute,
            @JsonProperty(ATTR_ORIENTATION) final Orientation orientation) {

        this.institutionId = institutionId;
        this.templateId = templateId;
        this.configAttribute = configAttribute;
        this.orientation = orientation;
    }

    @Override
    public String getModelId() {
        return this.configAttribute != null
                ? String.valueOf(this.configAttribute.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_NODE;
    }

    @Override
    public String getName() {
        return this.configAttribute.name;
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public ConfigurationAttribute getConfigAttribute() {
        return this.configAttribute;
    }

    public Orientation getOrientation() {
        return this.orientation;
    }

    @JsonIgnore
    public String getViewModelId() {
        if (this.orientation == null || this.orientation.viewId == null) {
            return null;
        }

        return String.valueOf(this.orientation.viewId);
    }

    @JsonIgnore
    public String getGroupId() {
        if (this.orientation == null) {
            return null;
        }

        return this.orientation.groupId;
    }

    @JsonIgnore
    public boolean isNameLike(final String name) {
        if (StringUtils.isBlank(name)) {
            return true;
        }
        return this.configAttribute.name.contains(name);
    }

    @JsonIgnore
    public boolean isInView(final Long viewId) {
        if (viewId == null) {
            return true;
        }
        return this.orientation != null && this.orientation.viewId.equals(viewId);
    }

    @JsonIgnore
    public boolean isGroupLike(final String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return true;
        }
        return this.orientation != null
                && this.orientation.groupId != null
                && this.orientation.groupId.contains(groupId);
    }

    @JsonIgnore
    public boolean hasType(final EnumSet<AttributeType> types) {
        if (types == null || types.isEmpty()) {
            return true;
        }

        return types.contains(this.configAttribute.type);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("TemplateAttribute [institutionId=");
        builder.append(this.institutionId);
        builder.append(", templateId=");
        builder.append(this.templateId);
        builder.append(", configAttribute=");
        builder.append(this.configAttribute);
        builder.append(", orientation=");
        builder.append(this.orientation);
        builder.append("]");
        return builder.toString();
    }

    public static Comparator<TemplateAttribute> nameComparator(final boolean descending) {
        return (attr1, attr2) -> attr1.configAttribute.name.compareToIgnoreCase(
                attr2.configAttribute.name) * ((descending) ? -1 : 1);
    }

    public static Comparator<TemplateAttribute> typeComparator(final boolean descending) {
        return (attr1, attr2) -> attr1.configAttribute.type.name().compareToIgnoreCase(
                attr2.configAttribute.type.name()) * ((descending) ? -1 : 1);
    }

    public static Comparator<TemplateAttribute> groupComparator(final boolean descending) {
        return (attr1, attr2) -> {
            final Orientation o1 = attr1.getOrientation();
            final Orientation o2 = attr2.getOrientation();
            final String name1 = (o1 != null && o1.getGroupId() != null)
                    ? o1.getGroupId()
                    : Constants.EMPTY_NOTE;
            final String name2 = (o2 != null && o2.getGroupId() != null)
                    ? o2.getGroupId()
                    : Constants.EMPTY_NOTE;
            return name1.compareToIgnoreCase(name2) * ((descending) ? -1 : 1);
        };
    }

}
