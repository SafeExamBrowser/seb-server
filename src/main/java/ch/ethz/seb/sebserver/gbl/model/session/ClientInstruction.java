/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClientInstruction {

    public enum InstructionType {
        SEB_QUIT,
        SEB_PROCTORING,
        SEB_RECONFIGURE_SETTINGS,
        NOTIFICATION_CONFIRM,
        SEB_FORCE_LOCK_SCREEN
    }

    public enum ProctoringInstructionMethod {
        JOIN,
        LEAVE
    }

    public interface SEB_INSTRUCTION_ATTRIBUTES {
        public interface SEB_PROCTORING {
            public static final String SERVICE_TYPE = "service-type";
            public static final String METHOD = "method";

            public static final String JITSI_URL = "jitsiMeetServerURL";
            public static final String JITSI_ROOM = "jitsiMeetRoom";
            public static final String JITSI_ROOM_SUBJECT = "jitsiMeetSubject";
            public static final String JITSI_TOKEN = "jitsiMeetToken";
            public static final String JITSI_RECEIVE_AUDIO = "jitsiMeetReceiveAudio";
            public static final String JITSI_RECEIVE_VIDEO = "jitsiMeetReceiveVideo";
            public static final String JITSI_ALLOW_CHAT = "jitsiFeatureFlagChat";

            public static final String ZOOM_URL = "zoomServerURL";
            public static final String ZOOM_ROOM = "zoomRoom";
            public static final String ZOOM_ROOM_SUBJECT = "zoomSubject";
            public static final String ZOOM_USER_NAME = "zoomUserName";
            public static final String ZOOM_API_KEY = "zoomAPIKey";
            public static final String ZOOM_TOKEN = "zoomToken";
            public static final String ZOOM_SDK_TOKEN = "zoomSDKToken";
            public static final String ZOOM_MEETING_KEY = "zoomMeetingKey";
            public static final String ZOOM_RECEIVE_AUDIO = "zoomReceiveAudio";
            public static final String ZOOM_RECEIVE_VIDEO = "zoomReceiveVideo";
            public static final String ZOOM_ALLOW_CHAT = "zoomFeatureFlagChat";
        }

        public interface SEB_RECONFIGURE_SETTINGS {
            public static final String JITSI_RECEIVE_AUDIO = "jitsiMeetReceiveAudio";
            public static final String JITSI_RECEIVE_VIDEO = "jitsiMeetReceiveVideo";
            public static final String JITSI_ALLOW_CHAT = "jitsiMeetFeatureFlagChat";
            public static final String JITSI_PIN_USER_ID = "jitsiMeetPinUser";
            public static final String ZOOM_RECEIVE_AUDIO = "zoomReceiveAudio";
            public static final String ZOOM_RECEIVE_VIDEO = "zoomReceiveVideo";
            public static final String ZOOM_ALLOW_CHAT = "zoomFeatureFlagChat";
        }

        public interface SEB_FORCE_LOCK_SCREEN {
            public static final String MESSAGE = "message";
            public static final String IMAGE_URL = "imageURL";
        }
    }

    @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_EXAM_ID)
    public final Long examId;

    @NotEmpty
    @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_CONNECTION_TOKEN)
    public final String connectionToken;

    @NotNull
    @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_TYPE)
    public final InstructionType type;

    @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_ATTRIBUTES)
    public final Map<String, String> attributes;

    @JsonCreator
    public ClientInstruction(
            @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_TYPE) final InstructionType type,
            @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(Domain.CLIENT_INSTRUCTION.ATTR_ATTRIBUTES) final Map<String, String> attributes) {

        this.id = id;
        this.connectionToken = connectionToken;
        this.examId = examId;
        this.type = type;
        this.attributes = Utils.immutableMapOf(attributes);
    }

    public Long getId() {
        return this.id;
    }

    public Long getExamId() {
        return this.examId;
    }

    public String getConnectionToken() {
        return this.connectionToken;
    }

    public InstructionType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientInstruction [id=");
        builder.append(this.id);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", connectionToken=");
        builder.append(this.connectionToken);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", attributes=");
        builder.append(this.attributes);
        builder.append("]");
        return builder.toString();
    }

}
