/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.util.Result;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.ValidProctoringSettings;

@JsonIgnoreProperties(ignoreUnknown = true)
@ValidProctoringSettings
public class ProctoringServiceSettings implements Entity {


    public enum ProctoringServerType {
        JITSI_MEET,
        ZOOM
    }

    public enum ProctoringFeature {
        TOWN_HALL,
        ONE_TO_ONE,
        BROADCAST,
        ENABLE_CHAT,
    }

    public static final String ATTR_ENABLE_PROCTORING = "enableProctoring";
    public static final String ATTR_SERVER_TYPE = "serverType";
    public static final String ATTR_SERVER_URL = "serverURL";

    // Jitsi access (former also Zoom)
    public static final String ATTR_APP_KEY = "appKey";
    public static final String ATTR_APP_SECRET = "appSecret";

    // Zoom Access
    public static final String ATTR_ACCOUNT_ID = "accountId";
    public static final String ATTR_ACCOUNT_CLIENT_ID = "clientId";
    public static final String ATTR_ACCOUNT_CLIENT_SECRET = "clientSecret";
    public static final String ATTR_SDK_KEY = "sdkKey";
    public static final String ATTR_SDK_SECRET = "sdkSecret";

    public static final String ATTR_COLLECTING_ROOM_SIZE = "collectingRoomSize";
    public static final String ATTR_ENABLED_FEATURES = "enabledFeatures";
    public static final String ATTR_COLLECT_ALL_ROOM_NAME = "collectAllRoomName";
    public static final String ATTR_SERVICE_IN_USE = "serviceInUse";
    public static final String ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM = "useZoomAppClientForCollectingRoom";
    
    public static final String ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME = "LIVE_PROCTORING_SETTINGS";

    @JsonProperty(Domain.EXAM.ATTR_ID)
    public final Long examId;

    @JsonProperty(ATTR_ENABLE_PROCTORING)
    public final Boolean enableProctoring;

    @JsonProperty(ATTR_SERVER_TYPE)
    public final ProctoringServerType serverType;

    @JsonProperty(ATTR_SERVER_URL)
    @URL(message = "proctoringSettings:serverURL:invalidURL")
    public final String serverURL;

    @JsonProperty(ATTR_APP_KEY)
    public final String appKey;

    @JsonProperty(ATTR_APP_SECRET)
    public final CharSequence appSecret;

    @JsonProperty(ATTR_ACCOUNT_ID)
    public final String accountId;

    @JsonProperty(ATTR_ACCOUNT_CLIENT_ID)
    public final String clientId;

    @JsonProperty(ATTR_ACCOUNT_CLIENT_SECRET)
    public final CharSequence clientSecret;

    @JsonProperty(ATTR_SDK_KEY)
    public final String sdkKey;

    @JsonProperty(ATTR_SDK_SECRET)
    public final CharSequence sdkSecret;

    @JsonProperty(ATTR_COLLECTING_ROOM_SIZE)
    public final Integer collectingRoomSize;

    @JsonProperty(ATTR_ENABLED_FEATURES)
    public final EnumSet<ProctoringFeature> enabledFeatures;

    @JsonProperty(ATTR_SERVICE_IN_USE)
    public final Boolean serviceInUse;

    @JsonProperty(ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM)
    public final Boolean useZoomAppClientForCollectingRoom;

    @JsonCreator
    public ProctoringServiceSettings(
            @JsonProperty(Domain.EXAM.ATTR_ID) final Long examId,
            @JsonProperty(ATTR_ENABLE_PROCTORING) final Boolean enableProctoring,
            @JsonProperty(ATTR_SERVER_TYPE) final ProctoringServerType serverType,
            @JsonProperty(ATTR_SERVER_URL) final String serverURL,
            @JsonProperty(ATTR_COLLECTING_ROOM_SIZE) final Integer collectingRoomSize,
            @JsonProperty(ATTR_ENABLED_FEATURES) final EnumSet<ProctoringFeature> enabledFeatures,
            @JsonProperty(ATTR_SERVICE_IN_USE) final Boolean serviceInUse,
            @JsonProperty(ATTR_APP_KEY) final String appKey,
            @JsonProperty(ATTR_APP_SECRET) final CharSequence appSecret,
            @JsonProperty(ATTR_ACCOUNT_ID) final String accountId,
            @JsonProperty(ATTR_ACCOUNT_CLIENT_ID) final String clientId,
            @JsonProperty(ATTR_ACCOUNT_CLIENT_SECRET) final CharSequence clientSecret,
            @JsonProperty(ATTR_SDK_KEY) final String sdkKey,
            @JsonProperty(ATTR_SDK_SECRET) final CharSequence sdkSecret,
            @JsonProperty(ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM) final Boolean useZoomAppClientForCollectingRoom) {

        this.examId = examId;
        this.enableProctoring = BooleanUtils.isTrue(enableProctoring);
        this.serverType = (serverType != null) ? serverType : ProctoringServerType.ZOOM;
        this.serverURL = serverURL;
        this.collectingRoomSize = (collectingRoomSize != null) ? collectingRoomSize : 20;
        this.enabledFeatures = enabledFeatures != null ? enabledFeatures : EnumSet.allOf(ProctoringFeature.class);
        this.serviceInUse = serviceInUse;
        this.appKey = StringUtils.trim(appKey);
        this.appSecret = appSecret;
        this.accountId = StringUtils.trim(accountId);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.sdkKey = StringUtils.trim(sdkKey);
        this.sdkSecret = sdkSecret;
        this.useZoomAppClientForCollectingRoom = BooleanUtils.toBoolean(useZoomAppClientForCollectingRoom);
    }

    public ProctoringServiceSettings(final Long examId) {
        this.examId = examId;
        this.enableProctoring = false;
        this.serverType = ProctoringServerType.ZOOM;
        this.serverURL = null;
        this.collectingRoomSize = 20;
        this.enabledFeatures = EnumSet.allOf(ProctoringFeature.class);
        this.serviceInUse = false;
        this.appKey = null;
        this.appSecret = null;
        this.accountId = null;
        this.clientId = null;
        this.clientSecret = null;
        this.sdkKey = null;
        this.sdkSecret = null;
        this.useZoomAppClientForCollectingRoom = false;
    }

    public ProctoringServiceSettings(final Long examId, final ProctoringServiceSettings copyOf) {
        this.examId = examId;
        this.enableProctoring = copyOf.enableProctoring;
        this.serverType = copyOf.serverType;
        this.serverURL = copyOf.serverURL;
        this.collectingRoomSize = copyOf.collectingRoomSize;
        this.enabledFeatures = copyOf.enabledFeatures;
        this.serviceInUse = false;
        this.appKey = copyOf.appKey;
        this.appSecret = copyOf.appSecret;
        this.accountId = copyOf.accountId;
        this.clientId = copyOf.clientId;
        this.clientSecret = copyOf.clientSecret;
        this.sdkKey = copyOf.sdkKey;
        this.sdkSecret = copyOf.sdkSecret;
        this.useZoomAppClientForCollectingRoom = copyOf.useZoomAppClientForCollectingRoom;
    }

    public ProctoringServiceSettings(final Exam exam) {
        if (exam == null) {
            throw new IllegalStateException("Exam has null reference");
        }
        if (!exam.additionalAttributesIncluded()) {
            throw new IllegalStateException("Exam has no additional attributes");
        }

        this.examId = exam.id;
        this.enableProctoring = BooleanUtils.toBooleanObject(exam.additionalAttributes.getOrDefault(
                ATTR_ENABLE_PROCTORING,
                Constants.FALSE_STRING));
        this.serverType = ProctoringServerType.valueOf(exam.additionalAttributes.getOrDefault(
                ATTR_SERVER_TYPE,
                ProctoringServerType.ZOOM.name()));
        this.serverURL = exam.additionalAttributes.get(ATTR_SERVER_URL);
        this.collectingRoomSize = Integer.parseInt(exam.additionalAttributes.getOrDefault(
                ATTR_COLLECTING_ROOM_SIZE,
                "-1"));
        this.enabledFeatures = getEnabledFeatures(exam.additionalAttributes);
        this.serviceInUse = this.enableProctoring;
        this.appKey = exam.additionalAttributes.get(ATTR_APP_KEY);
        this.appSecret = exam.additionalAttributes.get(ATTR_APP_SECRET);
        this.accountId = exam.additionalAttributes.get(ATTR_ACCOUNT_ID);
        this.clientId = exam.additionalAttributes.get(ATTR_ACCOUNT_CLIENT_ID);
        this.clientSecret = exam.additionalAttributes.get(ATTR_ACCOUNT_CLIENT_SECRET);
        this.sdkKey = exam.additionalAttributes.get(ATTR_SDK_KEY);
        this.sdkSecret = exam.additionalAttributes.get(ATTR_SDK_SECRET);
        this.useZoomAppClientForCollectingRoom = BooleanUtils.toBooleanObject(exam.additionalAttributes.getOrDefault(
                ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM,
                Constants.FALSE_STRING));
    }

    private EnumSet<ProctoringFeature> getEnabledFeatures(final Map<String, String> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLED_FEATURES)) {
            try {
                final String value = mapping.get(ProctoringServiceSettings.ATTR_ENABLED_FEATURES);
                return StringUtils.isNotBlank(value)
                        ? EnumSet.copyOf(Arrays.asList(StringUtils.split(value, Constants.LIST_SEPARATOR))
                                .stream()
                                .map(str -> {
                                    try {
                                        return ProctoringFeature.valueOf(str);
                                    } catch (final Exception e) {
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                        : EnumSet.noneOf(ProctoringFeature.class);
            } catch (final Exception e) {
                return EnumSet.allOf(ProctoringFeature.class);
            }
        } else {
            return EnumSet.allOf(ProctoringFeature.class);
        }
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
        return this.serverType.name() + " " + this.serverURL;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Boolean getEnableProctoring() {
        return this.enableProctoring;
    }

    public ProctoringServerType getServerType() {
        return this.serverType;
    }

    public String getServerURL() {
        return this.serverURL;
    }

    public Integer getCollectingRoomSize() {
        return this.collectingRoomSize;
    }

    public EnumSet<ProctoringFeature> getEnabledFeatures() {
        return this.enabledFeatures;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public CharSequence getAppSecret() {
        return this.appSecret;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public CharSequence getClientSecret() {
        return this.clientSecret;
    }

    public String getSdkKey() {
        return this.sdkKey;
    }

    public CharSequence getSdkSecret() {
        return this.sdkSecret;
    }

    public Boolean getServiceInUse() {
        return this.serviceInUse;
    }

    public Boolean getUseZoomAppClientForCollectingRoom() {
        return this.useZoomAppClientForCollectingRoom;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.examId == null) ? 0 : this.examId.hashCode());
        result = prime * result + ((this.serverType == null) ? 0 : this.serverType.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ProctoringServiceSettings other = (ProctoringServiceSettings) obj;
        if (this.examId == null) {
            if (other.examId != null)
                return false;
        } else if (!this.examId.equals(other.examId))
            return false;
        if (this.serverType != other.serverType)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ProctoringServiceSettings [examId=");
        builder.append(this.examId);
        builder.append(", enableProctoring=");
        builder.append(this.enableProctoring);
        builder.append(", serverType=");
        builder.append(this.serverType);
        builder.append(", serverURL=");
        builder.append(this.serverURL);
        builder.append(", appKey=");
        builder.append(this.appKey);
        builder.append(", accountId=");
        builder.append(this.accountId);
        builder.append(", clientId=");
        builder.append(this.clientId);
        builder.append(", sdkKey=");
        builder.append(this.sdkKey);
        builder.append(", collectingRoomSize=");
        builder.append(this.collectingRoomSize);
        builder.append(", enabledFeatures=");
        builder.append(this.enabledFeatures);
        builder.append(", serviceInUse=");
        builder.append(this.serviceInUse);
        builder.append(", useZoomAppClientForCollectingRoom=");
        builder.append(this.useZoomAppClientForCollectingRoom);
        builder.append("]");
        return builder.toString();
    }

}
