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
        LAST_PING(Names.LAST_PING, true),
        ERROR_COUNT(Names.ERROR_COUNT, true)

        ;

        public final String name;
        public final boolean integerValue;

        private IndicatorType(final String name, final boolean integerValue) {
            this.name = name;
            this.integerValue = integerValue;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public interface Names {
            public static final String LAST_PING = "LAST_PING";
            public static final String ERROR_COUNT = "ERROR_COUNT";
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

    @JsonProperty(THRESHOLD.REFERENCE_NAME)
    public final List<Threshold> thresholds;

    @JsonCreator
    public Indicator(
            @JsonProperty(INDICATOR.ATTR_ID) final Long id,
            @JsonProperty(INDICATOR.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(INDICATOR.ATTR_NAME) final String name,
            @JsonProperty(INDICATOR.ATTR_TYPE) final IndicatorType type,
            @JsonProperty(INDICATOR.ATTR_COLOR) final String defaultColor,
            @JsonProperty(THRESHOLD.REFERENCE_NAME) final Collection<Threshold> thresholds) {

        this.id = id;
        this.examId = examId;
        this.name = name;
        this.type = type;
        this.defaultColor = defaultColor;
        this.thresholds = Utils.immutableListOf(thresholds);
    }

    public Indicator(final Exam exam, final POSTMapper postParams) {
        this.id = null;
        this.examId = exam.id;
        this.name = postParams.getString(Domain.INDICATOR.ATTR_NAME);
        this.type = postParams.getEnum(Domain.INDICATOR.ATTR_TYPE, IndicatorType.class);
        this.defaultColor = postParams.getString(Domain.INDICATOR.ATTR_COLOR);
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
        builder.append(", thresholds=");
        builder.append(this.thresholds);
        builder.append("]");
        return builder.toString();
    }

    public static Indicator createNew(final Exam exam) {
        return new Indicator(null, exam.id, null, null, null, null);
    }

    public static final class Threshold {

        @JsonProperty(THRESHOLD.ATTR_VALUE)
        @NotNull
        public final Double value;

        @JsonProperty(THRESHOLD.ATTR_COLOR)
        public final String color;

        @JsonCreator
        public Threshold(
                @JsonProperty(THRESHOLD.ATTR_VALUE) final Double value,
                @JsonProperty(THRESHOLD.ATTR_COLOR) final String color) {

            this.value = value;
            this.color = color;
        }

        public Double getValue() {
            return this.value;
        }

        public String getColor() {
            return this.color;
        }

        @Override
        public String toString() {
            return "Threshold [value=" + this.value + ", color=" + this.color + "]";
        }

    }

}
