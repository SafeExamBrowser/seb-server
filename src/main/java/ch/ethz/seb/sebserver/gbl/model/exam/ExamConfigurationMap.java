/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_NODE;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM_CONFIGURATION_MAP;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExamConfigurationMap implements GrantEntity {

    public static final String ATTR_CONFIRM_ENCRYPT_SECRET = "confirm_encrypt_secret";

    public static final String FILTER_ATTR_EXAM_ID = "examId";
    public static final String FILTER_ATTR_CONFIG_ID = "configurationNodeId";

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "examConfigurationMap:examId:notNull")
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID)
    public final Long examId;

    @NotNull(message = "examConfigurationMap:configurationNodeId:notNull")
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID)
    public final Long configurationNodeId;

    @JsonProperty(CONFIGURATION_NODE.ATTR_NAME)
    public final String configName;

    @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION)
    public final String configDescription;

    @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS)
    public final ConfigurationStatus configStatus;

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_USER_NAMES)
    public final String userNames;

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET)
    public final CharSequence encryptSecret;

    @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET)
    public final CharSequence confirmEncryptSecret;

    @JsonCreator
    public ExamConfigurationMap(
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID) final Long id,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID) final Long configurationNodeId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_USER_NAMES) final String userNames,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET) final CharSequence encryptSecret,
            @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET) final CharSequence confirmEncryptSecret,

            @JsonProperty(CONFIGURATION_NODE.ATTR_NAME) final String configName,
            @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION) final String configDescription,
            @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS) final ConfigurationStatus configStatus) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.configurationNodeId = configurationNodeId;
        this.userNames = userNames;
        this.encryptSecret = encryptSecret;
        this.confirmEncryptSecret = confirmEncryptSecret;

        this.configName = configName;
        this.configDescription = configDescription;
        this.configStatus = configStatus;
    }

    public ExamConfigurationMap(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        this.examId = postParams.getLong(Domain.EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID);
        this.configurationNodeId = postParams.getLong(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID);
        this.userNames = postParams.getString(Domain.EXAM_CONFIGURATION_MAP.ATTR_USER_NAMES);
        this.encryptSecret = postParams.getCharSequence(Domain.EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET);
        this.confirmEncryptSecret = postParams.getCharSequence(ATTR_CONFIRM_ENCRYPT_SECRET);

        this.configName = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_NAME);
        this.configDescription = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION);
        this.configStatus = postParams.getEnum(Domain.CONFIGURATION_NODE.ATTR_STATUS, ConfigurationStatus.class);
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

    public CharSequence getEncryptSecret() {
        return this.encryptSecret;
    }

    public CharSequence getConfirmEncryptSecret() {
        return this.confirmEncryptSecret;
    }

    @JsonIgnore
    public boolean hasEncryptionSecret() {
        return this.encryptSecret != null && this.encryptSecret.length() > 0;
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getConfigDescription() {
        return this.configDescription;
    }

    public ConfigurationStatus getConfigStatus() {
        return this.configStatus;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ExamConfigurationMap [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", configurationNodeId=");
        builder.append(this.configurationNodeId);
        builder.append(", configName=");
        builder.append(this.configName);
        builder.append(", configDescription=");
        builder.append(this.configDescription);
        builder.append(", configStatus=");
        builder.append(this.configStatus);
        builder.append(", userNames=");
        builder.append(this.userNames);
        builder.append("]");
        return builder.toString();
    }

    public static ExamConfigurationMap createNew(final Exam exam) {
        return new ExamConfigurationMap(null, exam.institutionId, exam.id, null, null, null, null, null, null, null);
    }

}
