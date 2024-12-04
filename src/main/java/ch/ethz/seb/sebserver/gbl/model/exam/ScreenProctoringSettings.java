/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import javax.validation.constraints.NotEmpty;
import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScreenProctoringSettings implements SPSAPIAccessData, Entity {

    public static final String ATTR_ENABLE_SCREEN_PROCTORING = "enableScreenProctoring";
    public static final String ATTR_SPS_SERVICE_URL = "spsServiceURL";

    public static final String ATTR_SPS_API_KEY = "spsAPIKey";
    public static final String ATTR_SPS_API_SECRET = "spsAPISecret";

    public static final String ATTR_SPS_ACCOUNT_ID = "spsAccountId";
    public static final String ATTR_SPS_ACCOUNT_PASSWORD = "spsAccountPassword";

    public static final String ATT_SPS_DELETION_TIME = "spsDeletionTime";
    public static final String ATTR_COLLECTING_STRATEGY = "spsCollectingStrategy";
    public static final String ATTR_COLLECTING_GROUP_NAME = "spsCollectingGroupName";
    public static final String ATTR_COLLECTING_GROUP_SIZE = "spsCollectingGroupSize";
    public static final String ATT_SEB_GROUPS_SELECTION = "spsSEBGroupsSelection";

    public static final String ATTR_SPS_BUNDLED = "bundled";
    public static final String ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME = "SCREEN_PROCTORING_SETTINGS";

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

    @JsonProperty(ATT_SPS_DELETION_TIME)
    public final DateTime deletionTime;

    @JsonProperty(ATTR_COLLECTING_STRATEGY)
    public final CollectingStrategy collectingStrategy;

    @JsonProperty(ATTR_COLLECTING_GROUP_NAME)
    @NotEmpty(message = "screenProctoringSettings:spsCollectingGroupName:notNull")
    public final String collectingGroupName;

    @JsonProperty(ATTR_COLLECTING_GROUP_SIZE)
    public final Integer collectingGroupSize;

    @JsonProperty(ATT_SEB_GROUPS_SELECTION)
    public final String sebGroupsSelection;

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
            @JsonProperty(ATT_SPS_DELETION_TIME) final DateTime deletionTime,
            @JsonProperty(ATTR_COLLECTING_STRATEGY) final CollectingStrategy collectingStrategy,
            @JsonProperty(ATTR_COLLECTING_GROUP_NAME) final String collectingGroupName,
            @JsonProperty(ATTR_COLLECTING_GROUP_SIZE) final Integer collectingGroupSize,
            @JsonProperty(ATT_SEB_GROUPS_SELECTION) final String sebGroupsSelection,
            @JsonProperty(ATTR_SPS_BUNDLED) final boolean bundled) {

        this.examId = examId;
        this.enableScreenProctoring = enableScreenProctoring;
        this.spsServiceURL = spsServiceURL;
        this.spsAPIKey = spsAPIKey;
        this.spsAPISecret = spsAPISecret;
        this.spsAccountId = spsAccountId;
        this.spsAccountPassword = spsAccountPassword;
        this.deletionTime = deletionTime;
        this.collectingStrategy = collectingStrategy;
        this.collectingGroupName = collectingGroupName;
        this.collectingGroupSize = collectingGroupSize;
        this.sebGroupsSelection = sebGroupsSelection;
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
            final DateTime deletionTime,
            final CollectingStrategy collectingStrategy,
            final String collectingGroupName,
            final Integer collectingGroupSize,
            final String sebGroupsSelection) {

        this.examId = examId;
        this.enableScreenProctoring = enableScreenProctoring;
        this.spsServiceURL = spsServiceURL;
        this.spsAPIKey = spsAPIKey;
        this.spsAPISecret = spsAPISecret;
        this.spsAccountId = spsAccountId;
        this.spsAccountPassword = spsAccountPassword;
        this.deletionTime = deletionTime;
        this.collectingStrategy = collectingStrategy;
        this.collectingGroupName = collectingGroupName;
        this.collectingGroupSize = collectingGroupSize;
        this.sebGroupsSelection = sebGroupsSelection;
        this.bundled = false;
    }

    @Override
    public String getModelId() {
        return (this.examId != null) ? String.valueOf(this.examId) : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_PROCTOR_DATA;
    }

    @Override
    public String getName() {
        return this.spsServiceURL;
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

    public DateTime getDeletionTime() {
        return deletionTime;
    }

    public CollectingStrategy getCollectingStrategy() {
        return this.collectingStrategy;
    }

    public Integer getCollectingGroupSize() {
        return this.collectingGroupSize;
    }

    public String getCollectingGroupName() {
        return collectingGroupName;
    }

    public String getSebGroupsSelection() {
        return sebGroupsSelection;
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
        return "ScreenProctoringSettings{" +
                "examId=" + examId +
                ", enableScreenProctoring=" + enableScreenProctoring +
                ", spsServiceURL='" + spsServiceURL + '\'' +
                ", spsAPIKey='" + spsAPIKey + '\'' +
                ", spsAPISecret=" + spsAPISecret +
                ", spsAccountId='" + spsAccountId + '\'' +
                ", spsAccountPassword=" + spsAccountPassword +
                ", deletionTime=" + deletionTime +
                ", collectingStrategy=" + collectingStrategy +
                ", collectingGroupName='" + collectingGroupName + '\'' +
                ", collectingGroupSize=" + collectingGroupSize +
                ", sebGroupsSelection='" + sebGroupsSelection + '\'' +
                ", bundled=" + bundled +
                '}';
    }
}
