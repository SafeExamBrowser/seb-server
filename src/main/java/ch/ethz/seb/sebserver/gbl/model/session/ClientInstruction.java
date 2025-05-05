/*
 * Copyright (c) 2019 ETH Zürich, IT Services
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

    public static enum ProctoringRoomType {
        COLLECTING_ROOM("proctoring"),
        TOWNHALL("townhall"),
        BREAKOUT_ROOM("breakout");

        public final String roomTypeName;

        ProctoringRoomType(final String roomTypeName) {
            this.roomTypeName = roomTypeName;
        }
    }

    public interface SEB_INSTRUCTION_ATTRIBUTES {
        interface SEB_PROCTORING {
            String SERVICE_TYPE = "service-type";
            String METHOD = "method";

            String JITSI_URL = "jitsiMeetServerURL";
            String JITSI_ROOM = "jitsiMeetRoom";
            String JITSI_ROOM_SUBJECT = "jitsiMeetSubject";
            String JITSI_TOKEN = "jitsiMeetToken";
            String JITSI_RECEIVE_AUDIO = "jitsiMeetReceiveAudio";
            String JITSI_RECEIVE_VIDEO = "jitsiMeetReceiveVideo";
            String JITSI_ALLOW_CHAT = "jitsiFeatureFlagChat";

            String ZOOM_URL = "zoomServerURL";
            String ZOOM_ROOM = "zoomRoom";
            String ZOOM_ROOM_SUBJECT = "zoomSubject";
            String ZOOM_USER_NAME = "zoomUserName";
            String ZOOM_API_KEY = "zoomAPIKey";
            String ZOOM_TOKEN = "zoomToken";
            String ZOOM_SDK_TOKEN = "zoomSDKToken";
            String ZOOM_MEETING_KEY = "zoomMeetingKey";
            String ZOOM_RECEIVE_AUDIO = "zoomReceiveAudio";
            String ZOOM_RECEIVE_VIDEO = "zoomReceiveVideo";
            String ZOOM_ALLOW_CHAT = "zoomFeatureFlagChat";

            String PROCTORING_ROOM_TYPE = "roomType";
        }

        public interface SEB_RECONFIGURE_SETTINGS {
            String JITSI_RECEIVE_AUDIO = "jitsiMeetReceiveAudio";
            String JITSI_RECEIVE_VIDEO = "jitsiMeetReceiveVideo";
            String JITSI_ALLOW_CHAT = "jitsiMeetFeatureFlagChat";
            String JITSI_PIN_USER_ID = "jitsiMeetPinUser";
            String ZOOM_RECEIVE_AUDIO = "zoomReceiveAudio";
            String ZOOM_RECEIVE_VIDEO = "zoomReceiveVideo";
            String ZOOM_ALLOW_CHAT = "zoomFeatureFlagChat";
        }

        public interface SEB_FORCE_LOCK_SCREEN {
            String MESSAGE = "message";
            String IMAGE_URL = "imageURL";
        }

        public interface SEB_SCREEN_PROCTORING {
            String METHOD = "method";
            String SERVICE_TYPE = "service-type";
            String SERVICE_TYPE_NAME = "SCREEN_PROCTORING";
            String URL = "screenProctoringServiceURL";
            String CLIENT_ID = "screenProctoringClientId";
            String CLIENT_SECRET = "screenProctoringClientSecret";
            String GROUP_ID = "screenProctoringGroupId";
            String SESSION_ID = "screenProctoringClientSessionId";
            String SESSION_ENCRYPTION_KEY = "screenProctoringEncryptSecret";
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
        return "ClientInstruction [id=" +
                this.id +
                ", examId=" +
                this.examId +
                ", connectionToken=" +
                this.connectionToken +
                ", type=" +
                this.type +
                ", attributes=" +
                this.attributes +
                "]";
    }

}
