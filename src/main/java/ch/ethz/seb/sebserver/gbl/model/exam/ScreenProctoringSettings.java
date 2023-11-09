/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScreenProctoringSettings {

    public static final String ATTR_ENABLE_SCREEN_PROCTORING = "enableScreenProctoring";
    public static final String ATTR_SPS_SERVICE_URL = "spsServiceURL";
    public static final String ATTR_COLLECTING_STRATEGY = "spsCollectingStrategy";
    public static final String ATTR_COLLECTING_GROUP_SIZE = "collectingGroupSize";

    public static final String ATTR_SPS_API_KEY = "spsAPIKey";
    public static final String ATTR_SPS_API_SECRET = "spsAPISecret";

    public static final String ATTR_SPS_ACCOUNT_ID = "spsAccountId";
    public static final String ATTR_SPS_ACCOUNT_PASSWORD = "spsAccountPassword";

    public static final String ATTR_SPS_BUNDLED = "bundled";

    @JsonProperty(Domain.EXAM.ATTR_ID)
    public final Long examId;

    @JsonProperty(ATTR_ENABLE_SCREEN_PROCTORING)
    public final Boolean enableScreenProctoring;

    @JsonProperty(ATTR_SPS_SERVICE_URL)
    @URL(message = "screenProctoringSettings:spsServiceURL:invalidURL")
    public final String spsServiceURL;

    @JsonProperty(ATTR_SPS_API_KEY)
    public final String spsAPIKey;

    @JsonProperty(ATTR_SPS_API_SECRET)
    public final CharSequence spsAPISecret;

    @JsonProperty(ATTR_SPS_ACCOUNT_ID)
    public final String spsAccountId;

    @JsonProperty(ATTR_SPS_ACCOUNT_PASSWORD)
    public final CharSequence spsAccountPassword;

    @JsonProperty(ATTR_COLLECTING_STRATEGY)
    public final CollectingStrategy collectingStrategy;

    @JsonProperty(ATTR_COLLECTING_GROUP_SIZE)
    public final Integer collectingGroupSize;

    @JsonProperty(ATTR_SPS_BUNDLED)
    public final boolean bundled;

    @JsonCreator
    public ScreenProctoringSettings(
            @JsonProperty(Domain.EXAM.ATTR_ID) final Long examId,
            @JsonProperty(ATTR_ENABLE_SCREEN_PROCTORING) final Boolean enableScreenProctoring,
            @JsonProperty(ATTR_SPS_SERVICE_URL) final String spsServiceURL,
            @JsonProperty(ATTR_SPS_API_KEY) final String spsAPIKey,
            @JsonProperty(ATTR_SPS_API_SECRET) final CharSequence spsAPISecret,
            @JsonProperty(ATTR_SPS_ACCOUNT_ID) final String spsAccountId,
            @JsonProperty(ATTR_SPS_ACCOUNT_PASSWORD) final CharSequence spsAccountPassword,
            @JsonProperty(ATTR_COLLECTING_STRATEGY) final CollectingStrategy collectingStrategy,
            @JsonProperty(ATTR_COLLECTING_GROUP_SIZE) final Integer collectingGroupSize,
            @JsonProperty(ATTR_SPS_BUNDLED) final boolean bundled) {

        this.examId = examId;
        this.enableScreenProctoring = enableScreenProctoring;
        this.spsServiceURL = spsServiceURL;
        this.spsAPIKey = spsAPIKey;
        this.spsAPISecret = spsAPISecret;
        this.spsAccountId = spsAccountId;
        this.spsAccountPassword = spsAccountPassword;
        this.collectingStrategy = collectingStrategy;
        this.collectingGroupSize = collectingGroupSize;
        this.bundled = bundled;
    }

    public ScreenProctoringSettings(
            final Long examId,
            final Boolean enableScreenProctoring,
            final String spsServiceURL,
            final String spsAPIKey,
            final CharSequence spsAPISecret,
            final String spsAccountId,
            final CharSequence spsAccountPassword,
            final CollectingStrategy collectingStrategy,
            final Integer collectingGroupSize) {

        this.examId = examId;
        this.enableScreenProctoring = enableScreenProctoring;
        this.spsServiceURL = spsServiceURL;
        this.spsAPIKey = spsAPIKey;
        this.spsAPISecret = spsAPISecret;
        this.spsAccountId = spsAccountId;
        this.spsAccountPassword = spsAccountPassword;
        this.collectingStrategy = collectingStrategy;
        this.collectingGroupSize = collectingGroupSize;
        this.bundled = false;
    }

    public ScreenProctoringSettings(final Exam exam) {
        if (exam == null) {
            throw new IllegalStateException("Exam has null reference");
        }
        if (!exam.additionalAttributesIncluded()) {
            throw new IllegalStateException("Exam has no additional attributes");
        }

        this.examId = exam.id;
        this.enableScreenProctoring = BooleanUtils.toBooleanObject(exam.additionalAttributes.getOrDefault(
                ATTR_ENABLE_SCREEN_PROCTORING,
                Constants.FALSE_STRING));
        this.spsServiceURL = exam.additionalAttributes.get(ATTR_SPS_SERVICE_URL);
        this.spsAPIKey = exam.additionalAttributes.get(ATTR_SPS_API_KEY);
        this.spsAPISecret = exam.additionalAttributes.get(ATTR_SPS_API_SECRET);
        this.spsAccountId = exam.additionalAttributes.get(ATTR_SPS_ACCOUNT_ID);
        this.spsAccountPassword = exam.additionalAttributes.get(ATTR_SPS_ACCOUNT_PASSWORD);
        this.collectingStrategy = CollectingStrategy.valueOf(exam.additionalAttributes.getOrDefault(
                ATTR_COLLECTING_STRATEGY,
                CollectingStrategy.EXAM.name()));
        this.collectingGroupSize = Integer.parseInt(exam.additionalAttributes.getOrDefault(
                ATTR_COLLECTING_GROUP_SIZE,
                "-1"));
        this.bundled = false;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Boolean getEnableScreenProctoring() {
        return this.enableScreenProctoring;
    }

    public String getSpsServiceURL() {
        return this.spsServiceURL;
    }

    public String getSpsAPIKey() {
        return this.spsAPIKey;
    }

    public CharSequence getSpsAPISecret() {
        return this.spsAPISecret;
    }

    public String getSpsAccountId() {
        return this.spsAccountId;
    }

    public CharSequence getSpsAccountPassword() {
        return this.spsAccountPassword;
    }

    public CollectingStrategy getCollectingStrategy() {
        return this.collectingStrategy;
    }

    public Integer getCollectingGroupSize() {
        return this.collectingGroupSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.examId);
    }

    public boolean isBundled() {
        return this.bundled;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ScreenProctoringSettings other = (ScreenProctoringSettings) obj;
        return Objects.equals(this.examId, other.examId);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ScreenProctoringSettings [examId=");
        builder.append(this.examId);
        builder.append(", enableScreenProctoring=");
        builder.append(this.enableScreenProctoring);
        builder.append(", spsServiceURL=");
        builder.append(this.spsServiceURL);
        builder.append(", spsAPIKey=");
        builder.append(this.spsAPIKey);
        builder.append(", spsAPISecret=");
        builder.append(this.spsAPISecret);
        builder.append(", spsAccountId=");
        builder.append(this.spsAccountId);
        builder.append(", spsAccountPassword=");
        builder.append(this.spsAccountPassword);
        builder.append(", collectingStrategy=");
        builder.append(this.collectingStrategy);
        builder.append(", collectingGroupSize=");
        builder.append(this.collectingGroupSize);
        builder.append("]");
        return builder.toString();
    }

}
