/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_SECURITY_KEY_REGISTRY;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppSignatureKeyInfo {

    public static final String ATTR_KEY_CONNECTION_MAPPING = "kcMapping";

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(ATTR_KEY_CONNECTION_MAPPING)
    public final Map<String, Set<Long>> keyConnectionMapping;

    @JsonCreator
    public AppSignatureKeyInfo(
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(ATTR_KEY_CONNECTION_MAPPING) final Map<String, Set<Long>> keyConnectionMapping) {

        this.institutionId = institutionId;
        this.examId = examId;
        this.keyConnectionMapping = Utils.immutableMapOf(keyConnectionMapping);
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Map<String, Set<Long>> getKeyConnectionMapping() {
        return this.keyConnectionMapping;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.examId, this.institutionId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AppSignatureKeyInfo other = (AppSignatureKeyInfo) obj;
        return Objects.equals(this.examId, other.examId) && Objects.equals(this.institutionId, other.institutionId);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AppSignatureKeyInfo [institutionId=");
        builder.append(this.institutionId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", keyConnectionMapping=");
        builder.append(this.keyConnectionMapping);
        builder.append("]");
        return builder.toString();
    }

}
