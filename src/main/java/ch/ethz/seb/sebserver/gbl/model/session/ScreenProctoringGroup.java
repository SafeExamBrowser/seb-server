/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScreenProctoringGroup {

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_ID)
    public final Long id;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_UUID)
    public final String uuid;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_NAME)
    public final String name;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_SIZE)
    public final Integer size;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_DATA)
    public final String additionalData;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_COLLECTING_STRATEGY)
    public final CollectingStrategy collectingStrategy;

    @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_SEB_GROUP_ID)
    public final Long sebGroupId;

    @JsonCreator
    public ScreenProctoringGroup(
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_ID) final Long id,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_UUID) final String uuid,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_NAME) final String name,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_SIZE) final Integer size,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_DATA) final String additionalData,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_COLLECTING_STRATEGY) final CollectingStrategy collectingStrategy,
            @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_SEB_GROUP_ID) final Long sebGroupId) {

        this.id = id;
        this.examId = examId;
        this.uuid = uuid;
        this.name = name;
        this.size = size;
        this.additionalData = additionalData;
        this.collectingStrategy = collectingStrategy;
        this.sebGroupId = sebGroupId;
    }

    public Long getId() {
        return this.id;
    }

    public Long getExamId() {
        return this.examId;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public Integer getSize() {
        return this.size;
    }

    public String getAdditionalData() {
        return this.additionalData;
    }

    public CollectingStrategy getCollectingStrategy() {
        return collectingStrategy;
    }

    public Long getSebGroupId() {
        return sebGroupId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ScreenProctoringGroup other = (ScreenProctoringGroup) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "ScreenProctoringGroup{" +
                "id=" + id +
                ", examId=" + examId +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", additionalData='" + additionalData + '\'' +
                ", collectingStrategy=" + collectingStrategy +
                ", sebGroupId=" + sebGroupId +
                '}';
    }
}
