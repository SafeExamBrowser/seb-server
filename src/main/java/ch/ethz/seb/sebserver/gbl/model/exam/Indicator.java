/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.Domain.INDICATOR;
import ch.ethz.seb.sebserver.gbl.model.Domain.THRESHOLD;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class Indicator implements GrantEntity {

    public static final String FILTER_ATTR_EXAM = "exam";

    public enum IndicatorType {
        LAST_PING,
        ERROR_COUNT
    }

    @JsonProperty(INDICATOR.ATTR_ID)
    public final Long id;

    @JsonProperty(EXAM.ATTR_INSTITUTION_ID)
    @NotNull
    public final Long institutionId;

    @JsonProperty(INDICATOR.ATTR_EXAM_ID)
    @NotNull
    public final Long examId;

    @JsonProperty(INDICATOR.ATTR_NAME)
    @NotNull
    @Size(min = 3, max = 255, message = "indicator:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(INDICATOR.ATTR_TYPE)
    @NotNull
    public final IndicatorType type;

    @JsonProperty(INDICATOR.ATTR_COLOR)
    public final String defaultColor;

    @JsonProperty(THRESHOLD.REFERENCE_NAME)
    public final List<Threshold> thresholds;

    @JsonCreator
    public Indicator(
            @JsonProperty(INDICATOR.ATTR_ID) final Long id,
            @JsonProperty(EXAM.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM.ATTR_OWNER) final String owner,
            @JsonProperty(INDICATOR.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(INDICATOR.ATTR_NAME) final String name,
            @JsonProperty(INDICATOR.ATTR_TYPE) final IndicatorType type,
            @JsonProperty(INDICATOR.ATTR_COLOR) final String defaultColor,
            @JsonProperty(THRESHOLD.REFERENCE_NAME) final Collection<Threshold> thresholds) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.name = name;
        this.type = type;
        this.defaultColor = defaultColor;
        this.thresholds = Utils.immutableListOf(thresholds);
    }

    public Indicator(final Exam exam, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = exam.institutionId;
        this.examId = exam.id;
        this.name = postParams.getString(Domain.INDICATOR.ATTR_NAME);
        this.type = postParams.getEnum(Domain.INDICATOR.ATTR_TYPE, IndicatorType.class);
        this.defaultColor = postParams.getString(Domain.INDICATOR.ATTR_COLOR);
        this.thresholds = Collections.emptyList();
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

    @Override

    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getOwnerId() {
        return null;
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
        return "Indicator [id=" + this.id + ", examId=" + this.examId + ", name=" + this.name + ", type=" + this.type
                + ", defaultColor="
                + this.defaultColor + ", thresholds=" + this.thresholds + "]";
    }

    public static final class Threshold {

        @JsonProperty(THRESHOLD.ATTR_ID)
        public final Long id;

        @JsonProperty(THRESHOLD.ATTR_INDICATOR_ID)
        @NotNull
        public final Long indicatorId;

        @JsonProperty(THRESHOLD.ATTR_VALUE)
        @NotNull
        public final Double value;

        @JsonProperty(THRESHOLD.ATTR_COLOR)
        public final String color;

        @JsonCreator
        public Threshold(
                @JsonProperty(THRESHOLD.ATTR_ID) final Long id,
                @JsonProperty(THRESHOLD.ATTR_INDICATOR_ID) final Long indicatorId,
                @JsonProperty(THRESHOLD.ATTR_VALUE) final Double value,
                @JsonProperty(THRESHOLD.ATTR_COLOR) final String color) {

            this.id = id;
            this.indicatorId = indicatorId;
            this.value = value;
            this.color = color;
        }

        public Long getId() {
            return this.id;
        }

        public Long getIndicatorId() {
            return this.indicatorId;
        }

        public Double getValue() {
            return this.value;
        }

        public String getColor() {
            return this.color;
        }

        @Override
        public String toString() {
            return "Threshold [id=" + this.id + ", indicatorId=" + this.indicatorId + ", value=" + this.value
                    + ", color=" + this.color
                    + "]";
        }

    }

}
