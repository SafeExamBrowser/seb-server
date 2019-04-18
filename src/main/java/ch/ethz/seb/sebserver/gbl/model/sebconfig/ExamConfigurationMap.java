/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM_CONFIGURATION_MAP;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExamConfigurationMap implements GrantEntity {

    public static final String FILTER_ATTR_EXAM_ID = "examId";
    public static final String FILTER_ATTR_CONFIG_ID = "configurationNodeId";

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID)
    public final Long examId;

    @NotNull
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID)
    public final Long configurationNodeId;

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_USER_NAMES)
    public final String userNames;

    @JsonCreator
    public ExamConfigurationMap(
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID) final Long id,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID) final Long configurationNodeId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_USER_NAMES) final String userNames) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.configurationNodeId = configurationNodeId;
        this.userNames = userNames;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_CONFIGURATION_MAP;
    }

    @Override
    public String getName() {
        return getModelId();
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

    public Long getId() {
        return this.id;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Long getConfigurationNodeId() {
        return this.configurationNodeId;
    }

    public String getUserNames() {
        return this.userNames;
    }

    @Override
    public String toString() {
        return "ExamConfiguration [id=" + this.id + ", institutionId=" + this.institutionId + ", examId=" + this.examId
                + ", configurationNodeId=" + this.configurationNodeId + ", userNames=" + this.userNames + "]";
    }

}
