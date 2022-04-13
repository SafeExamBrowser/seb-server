/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.INDICATOR;
import ch.ethz.seb.sebserver.gbl.model.Domain.THRESHOLD;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IndicatorTemplate implements Entity {

    public static final String ATTR_EXAM_TEMPLATE_ID = "examTemplateId";

    @JsonProperty(INDICATOR.ATTR_ID)
    public final Long id;

    @JsonProperty(ATTR_EXAM_TEMPLATE_ID)
    public final Long examTemplateId;

    @JsonProperty(INDICATOR.ATTR_NAME)
    @NotNull(message = "indicator:name:notNull")
    @Size(min = 3, max = 255, message = "indicator:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(INDICATOR.ATTR_TYPE)
    @NotNull(message = "indicator:type:notNull")
    public final IndicatorType type;

    @JsonProperty(INDICATOR.ATTR_COLOR)
    public final String defaultColor;

    @JsonProperty(INDICATOR.ATTR_ICON)
    public final String defaultIcon;

    @JsonProperty(INDICATOR.ATTR_TAGS)
    public final String tags;

    @JsonProperty(THRESHOLD.REFERENCE_NAME)
    public final List<Threshold> thresholds;

    @JsonCreator
    public IndicatorTemplate(
            @JsonProperty(INDICATOR.ATTR_ID) final Long id,
            @JsonProperty(ATTR_EXAM_TEMPLATE_ID) final Long examTemplateId,
            @JsonProperty(INDICATOR.ATTR_NAME) final String name,
            @JsonProperty(INDICATOR.ATTR_TYPE) final IndicatorType type,
            @JsonProperty(INDICATOR.ATTR_COLOR) final String defaultColor,
            @JsonProperty(INDICATOR.ATTR_ICON) final String defaultIcon,
            @JsonProperty(INDICATOR.ATTR_TAGS) final String tags,
            @JsonProperty(THRESHOLD.REFERENCE_NAME) final Collection<Threshold> thresholds) {

        this.id = id;
        this.examTemplateId = examTemplateId;
        this.name = name;
        this.type = type;
        this.defaultColor = defaultColor;
        this.defaultIcon = defaultIcon;
        this.tags = tags;
        this.thresholds = Utils.immutableListOf(thresholds);
    }

    /** This initialize an indicator for an exam template */
    public IndicatorTemplate(final Long id, final Long examTemplateId, final POSTMapper postParams) {
        this.id = id;
        this.examTemplateId = examTemplateId;
        this.name = postParams.getString(Domain.INDICATOR.ATTR_NAME);
        this.type = postParams.getEnum(Domain.INDICATOR.ATTR_TYPE, IndicatorType.class);
        this.defaultColor = postParams.getString(Domain.INDICATOR.ATTR_COLOR);
        this.defaultIcon = postParams.getString(Domain.INDICATOR.ATTR_ICON);
        this.tags = postParams.getString(Domain.INDICATOR.ATTR_TAGS);
        this.thresholds = postParams.getThresholds();
    }

    public IndicatorTemplate(final Long id, final IndicatorTemplate other) {
        this.id = id;
        this.examTemplateId = other.examTemplateId;
        this.name = other.name;
        this.type = other.type;
        this.defaultColor = other.defaultColor;
        this.defaultIcon = other.defaultIcon;
        this.tags = other.tags;
        this.thresholds = Utils.immutableListOf(other.thresholds);
    }

    @Override
    public String getModelId() {
        return (this.id == null) ? null : String.valueOf(this.id);
    }

    @Override
    public EntityType entityType() {
        return EntityType.INDICATOR;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Long getId() {
        return this.id;
    }

    public Long getExamTemplateId() {
        return this.examTemplateId;
    }

    public IndicatorType getType() {
        return this.type;
    }

    public String getDefaultColor() {
        return this.defaultColor;
    }

    public String getDefaultIcon() {
        return this.defaultIcon;
    }

    public String getTags() {
        return this.tags;
    }

    public Collection<Threshold> getThresholds() {
        return this.thresholds;
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
        final IndicatorTemplate other = (IndicatorTemplate) obj;
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
        builder.append("Indicator [id=");
        builder.append(this.id);
        builder.append(", examTemplateId=");
        builder.append(this.examTemplateId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", defaultColor=");
        builder.append(this.defaultColor);
        builder.append(", defaultIcon=");
        builder.append(this.defaultIcon);
        builder.append(", tags=");
        builder.append(this.tags);
        builder.append(", thresholds=");
        builder.append(this.thresholds);
        builder.append("]");
        return builder.toString();
    }

    public static String getDisplayValue(final IndicatorType indicatorType, final Double value) {
        if (value == null || indicatorType == null) {
            return StringUtils.EMPTY;
        }
        if (indicatorType.integerValue) {
            return String.valueOf(value.intValue());
        } else {
            return String.valueOf(value.floatValue());
        }
    }

}
