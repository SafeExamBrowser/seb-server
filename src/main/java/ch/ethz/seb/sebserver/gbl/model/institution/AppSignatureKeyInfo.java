/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_SECURITY_KEY_REGISTRY;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppSignatureKeyInfo implements ModelIdAware {

    public static final String ATTR_NUMBER_OF_CONNECTIONS = "numConnections";
    public static final String ATTR_CONNECTION_IDS = "connectionIds";

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE)
    public final String key;

    @JsonProperty(ATTR_CONNECTION_IDS)
    public final Map<Long, String> connectionIds;

    @JsonCreator
    public AppSignatureKeyInfo(
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE) final String key,
            @JsonProperty(ATTR_CONNECTION_IDS) final Map<Long, String> connectionIds) {

        this.institutionId = institutionId;
        this.examId = examId;
        this.key = key;
        this.connectionIds = Utils.immutableMapOf(connectionIds);
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getExamId() {
        return this.examId;
    }

    @Override
    public String getModelId() {
        return StringUtils.isNoneBlank(this.key) ? this.key : "-1";
    }

    public String getKey() {
        return this.key;
    }

    public Map<Long, String> getConnectionIds() {
        return this.connectionIds;
    }

    @JsonIgnore
    public String getConnectionNames() {
        return StringUtils.join(this.connectionIds.values(), Constants.LIST_SEPARATOR_CHAR);
    }

    @JsonIgnore
    public int getNumberOfConnections() {
        return this.connectionIds.size();
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
        builder.append(", key=");
        builder.append(this.key);
        builder.append(", connectionIds=");
        builder.append(this.connectionIds);
        builder.append("]");
        return builder.toString();
    }

}
