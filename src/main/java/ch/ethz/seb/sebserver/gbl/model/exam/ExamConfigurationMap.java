/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_NODE;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM_CONFIGURATION_MAP;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExamConfigurationMap implements GrantEntity {

    private static final String ARR_EXAM_STATUS = "examStatus";

    public static final String ATTR_CONFIRM_ENCRYPT_SECRET = "confirm_encrypt_secret";

    public static final String FILTER_ATTR_EXAM_ID = "examId";
    public static final String FILTER_ATTR_CONFIG_ID = "configurationNodeId";

    @JsonCreator
    public ExamConfigurationMap(
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID) final Long id,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(QuizData.QUIZ_ATTR_NAME) final String examName,
            @JsonProperty(QuizData.QUIZ_ATTR_DESCRIPTION) final String examDescription,
            @JsonProperty(QuizData.QUIZ_ATTR_START_TIME) final DateTime examStartTime,
            @JsonProperty(EXAM.ATTR_TYPE) final ExamType examType,
            @JsonProperty(ARR_EXAM_STATUS) final ExamStatus examStatus,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID) final Long configurationNodeId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CLIENT_GROUP_ID) final Long clientGroupId,
            @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET) final CharSequence encryptSecret,
            @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET) final CharSequence confirmEncryptSecret,
            @JsonProperty(CONFIGURATION_NODE.ATTR_NAME) final String configName,
            @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION) final String configDescription,
            @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS) final ConfigurationStatus configStatus) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.examName = examName;
        this.examDescription = examDescription;
        this.examStartTime = examStartTime;
        this.examType = examType;
        this.examStatus = examStatus;
        this.configurationNodeId = configurationNodeId;
        this.clientGroupId = clientGroupId;
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

        this.examName = postParams.getString(QuizData.QUIZ_ATTR_NAME);
        this.examDescription = postParams.getString(QuizData.QUIZ_ATTR_DESCRIPTION);
        this.examStartTime = postParams.getDateTime(QuizData.QUIZ_ATTR_START_TIME);
        this.examType = postParams.getEnum(EXAM.ATTR_TYPE, ExamType.class);
        this.examStatus = postParams.getEnum(ARR_EXAM_STATUS, ExamStatus.class);

        this.configurationNodeId = postParams.getLong(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID);
        this.clientGroupId = postParams.getLong(Domain.EXAM_CONFIGURATION_MAP.ATTR_CLIENT_GROUP_ID);
        this.encryptSecret = postParams.getCharSequence(Domain.EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET);
        this.confirmEncryptSecret = postParams.getCharSequence(ATTR_CONFIRM_ENCRYPT_SECRET);

        this.configName = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_NAME);
        this.configDescription = postParams.getString(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION);
        this.configStatus = postParams.getEnum(Domain.CONFIGURATION_NODE.ATTR_STATUS, ConfigurationStatus.class);
    }

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "examConfigurationMap:examId:notNull")
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(QuizData.QUIZ_ATTR_NAME)
    public final String examName;

    @JsonProperty(QuizData.QUIZ_ATTR_DESCRIPTION)
    public final String examDescription;

    @JsonProperty(QuizData.QUIZ_ATTR_START_TIME)
    public final DateTime examStartTime;

    @JsonProperty(EXAM.ATTR_TYPE)
    public final ExamType examType;

    @JsonProperty(ARR_EXAM_STATUS)
    public final ExamStatus examStatus;

    @NotNull(message = "examConfigurationMap:configurationNodeId:notNull")
    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID)
    public final Long configurationNodeId;

    @JsonProperty(CONFIGURATION_NODE.ATTR_NAME)
    public final String configName;

    @JsonProperty(CONFIGURATION_NODE.ATTR_DESCRIPTION)
    public final String configDescription;

    @JsonProperty(CONFIGURATION_NODE.ATTR_STATUS)
    public final ConfigurationStatus configStatus;

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_CLIENT_GROUP_ID)
    public final Long clientGroupId;

    @JsonProperty(EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET)
    public final CharSequence encryptSecret;

    @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET)
    public final CharSequence confirmEncryptSecret;

    public ExamConfigurationMap(
            final Long institutionId,
            final Long examId,
            final Long configurationNodeId,
            final Long clientGroupId) {

        this.id = null;
        this.institutionId = institutionId;
        this.examId = examId;
        this.examName = null;
        this.examDescription = null;
        this.examStartTime = null;
        this.examType = null;
        this.examStatus = null;
        this.configurationNodeId = configurationNodeId;
        this.clientGroupId = clientGroupId;
        this.encryptSecret = null;
        this.confirmEncryptSecret = null;

        this.configName = null;
        this.configDescription = null;
        this.configStatus = null;
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

    public String getExamName() {
        return this.examName;
    }

    public String getExamDescription() {
        return this.examDescription;
    }

    public DateTime getExamStartTime() {
        return this.examStartTime;
    }

    public ExamType getExamType() {
        return this.examType;
    }

    public ExamStatus getExamStatus() {
        return this.examStatus;
    }

    public Long getConfigurationNodeId() {
        return this.configurationNodeId;
    }

    public Long getClientGroupId() {
        return this.clientGroupId;
    }

    @JsonIgnore
    public CharSequence getEncryptSecret() {
        return this.encryptSecret;
    }

    @JsonIgnore
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
    public Entity printSecureCopy() {
        return new ExamConfigurationMap(
                this.id,
                this.institutionId,
                this.examId,
                this.examName,
                this.examDescription,
                this.examStartTime,
                this.examType,
                this.examStatus,
                this.configurationNodeId,
                this.clientGroupId,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                this.configName,
                this.configDescription,
                this.configStatus);
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
        builder.append(", examName=");
        builder.append(this.examName);
        builder.append(", examDescription=");
        builder.append(this.examDescription);
        builder.append(", examStartTime=");
        builder.append(this.examStartTime);
        builder.append(", examType=");
        builder.append(this.examType);
        builder.append(", configurationNodeId=");
        builder.append(this.configurationNodeId);
        builder.append(", configName=");
        builder.append(this.configName);
        builder.append(", configDescription=");
        builder.append(this.configDescription);
        builder.append(", configStatus=");
        builder.append(this.configStatus);
        builder.append(", clientGroupId=");
        builder.append(this.clientGroupId);
        builder.append(", encryptSecret=");
        builder.append(this.encryptSecret);
        builder.append(", confirmEncryptSecret=");
        builder.append(this.confirmEncryptSecret);
        builder.append("]");
        return builder.toString();
    }

    public static ExamConfigurationMap createNew(final Exam exam) {
        return new ExamConfigurationMap(
                null, exam.institutionId, exam.id, exam.name, exam.getDescription(), exam.startTime, exam.type,
                exam.status,
                null, null, null, null, null, null, null);
    }

}
