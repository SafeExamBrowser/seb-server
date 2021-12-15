/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Indicator implements Entity {

    public static final String FILTER_ATTR_EXAM_ID = "examId";

    public enum IndicatorType {
        NONE(0, "UNKNOWN", false, false, false, false, false),
        LAST_PING(1, Names.LAST_PING, false, true, true, false, false),
        ERROR_COUNT(2, Names.ERROR_COUNT, false, true, false, true, false),
        WARN_COUNT(3, Names.WARN_COUNT, false, true, false, true, false),
        INFO_COUNT(4, Names.INFO_COUNT, false, true, false, true, false),
        BATTERY_STATUS(5, Names.BATTERY_STATUS, true, true, true, true, true),
        WLAN_STATUS(6, Names.WLAN_STATUS, true, true, true, true, true);

        public final int id;
        public final String name;
        public final boolean inverse;
        public final boolean integerValue;
        public final boolean showOnlyInActiveState;
        public final boolean tags;
        public final boolean tagsReadonly;

        IndicatorType(
                final int id,
                final String name,
                final boolean inverse,
                final boolean integerValue,
                final boolean showOnlyInActiveState,
                final boolean tags,
                final boolean tagsReadonly) {

            this.id = id;
            this.name = name;
            this.inverse = inverse;
            this.integerValue = integerValue;
            this.showOnlyInActiveState = showOnlyInActiveState;
            this.tags = tags;
            this.tagsReadonly = tagsReadonly;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public interface Names {
            String LAST_PING = "LAST_PING";
            String ERROR_COUNT = "ERROR_COUNT";
            String WARN_COUNT = "WARN_COUNT";
            String INFO_COUNT = "INFO_COUNT";
            String BATTERY_STATUS = "BATTERY_STATUS";
            String WLAN_STATUS = "WLAN_STATUS";
        }
    }

    @JsonProperty(INDICATOR.ATTR_ID)
    public final Long id;

    @JsonProperty(INDICATOR.ATTR_EXAM_ID)
    @NotNull
    public final Long examId;

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
    public Indicator(
            @JsonProperty(INDICATOR.ATTR_ID) final Long id,
            @JsonProperty(INDICATOR.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(INDICATOR.ATTR_NAME) final String name,
            @JsonProperty(INDICATOR.ATTR_TYPE) final IndicatorType type,
            @JsonProperty(INDICATOR.ATTR_COLOR) final String defaultColor,
            @JsonProperty(INDICATOR.ATTR_ICON) final String defaultIcon,
            @JsonProperty(INDICATOR.ATTR_TAGS) final String tags,
            @JsonProperty(THRESHOLD.REFERENCE_NAME) final Collection<Threshold> thresholds) {

        this.id = id;
        this.examId = examId;
        this.name = name;
        this.type = type;
        this.defaultColor = defaultColor;
        this.defaultIcon = defaultIcon;
        this.tags = tags;
        this.thresholds = Utils.immutableListOf(thresholds);
    }

    public Indicator(final Long examId, final POSTMapper postParams) {
        this.id = null;
        this.examId = examId;
        this.name = postParams.getString(Domain.INDICATOR.ATTR_NAME);
        this.type = postParams.getEnum(Domain.INDICATOR.ATTR_TYPE, IndicatorType.class);
        this.defaultColor = postParams.getString(Domain.INDICATOR.ATTR_COLOR);
        this.defaultIcon = postParams.getString(Domain.INDICATOR.ATTR_ICON);
        this.tags = postParams.getString(Domain.INDICATOR.ATTR_TAGS);
        this.thresholds = postParams.getThresholds();
    }

    /** This initialize an indicator for an exam template */
    public Indicator(final Long id, final Long examTemplateId, final POSTMapper postParams) {
        this.id = id;
        this.examId = examTemplateId;
        this.name = postParams.getString(Domain.INDICATOR.ATTR_NAME);
        this.type = postParams.getEnum(Domain.INDICATOR.ATTR_TYPE, IndicatorType.class);
        this.defaultColor = postParams.getString(Domain.INDICATOR.ATTR_COLOR);
        this.defaultIcon = postParams.getString(Domain.INDICATOR.ATTR_ICON);
        this.tags = postParams.getString(Domain.INDICATOR.ATTR_TAGS);
        this.thresholds = postParams.getThresholds();
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

    public Long getExamId() {
        return this.examId;
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
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Indicator [id=");
        builder.append(this.id);
        builder.append(", examId=");
        builder.append(this.examId);
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

    public static Indicator createNew(final String examId) {
        try {
            return new Indicator(null, Long.parseLong(examId), null, null, null, null, null, null);
        } catch (final Exception e) {
            return new Indicator(null, null, null, null, null, null, null, null);
        }
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

    public static final class Threshold implements Comparable<Threshold> {

        @JsonProperty(THRESHOLD.ATTR_VALUE)
        @NotNull
        public final Double value;

        @JsonProperty(THRESHOLD.ATTR_COLOR)
        public final String color;

        @JsonProperty(THRESHOLD.ATTR_ICON)
        public final String icon;

        @JsonCreator
        public Threshold(
                @JsonProperty(THRESHOLD.ATTR_VALUE) final Double value,
                @JsonProperty(THRESHOLD.ATTR_COLOR) final String color,
                @JsonProperty(THRESHOLD.ATTR_ICON) final String icon) {

            this.value = value;
            this.color = color;
            this.icon = icon;
        }

        public Double getValue() {
            return this.value;
        }

        public String getColor() {
            return this.color;
        }

        public String getIcon() {
            return this.icon;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Threshold [value=");
            builder.append(this.value);
            builder.append(", color=");
            builder.append(this.color);
            builder.append(", icon=");
            builder.append(this.icon);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public int compareTo(final Threshold o) {
            return Double.compare(this.value, (o != null) ? o.value : -1);
        }

    }

}
