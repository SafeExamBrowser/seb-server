/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProctoringRoomConnection {

    public static final String ATTR_CONNECTION_TOKEN = "connectionToken";
    public static final String ATTR_SERVER_HOST = "serverHost";
    public static final String ATTR_SERVER_URL = "serverURL";
    public static final String ATTR_ROOM_NAME = "roomName";
    public static final String ATTR_SUBJECT = "subject";
    public static final String ATTR_ACCESS_TOKEN = "accessToken";
    public static final String ATTR_SDK_TOKEN = "sdkToken";
    public static final String ATTR_CONNECTION_URL = "connectionURL";
    public static final String ATTR_USER_NAME = "userName";
    public static final String ATTR_ROOM_KEY = "roomKey";
    public static final String ATTR_API_KEY = "apiKey";
    public static final String ATTR_MEETING_ID = "meetingId";

    @JsonProperty(ProctoringServiceSettings.ATTR_SERVER_TYPE)
    public final ProctoringServerType proctoringServerType;

    @JsonProperty(ATTR_CONNECTION_TOKEN)
    public final String connectionToken;

    @JsonProperty(ATTR_SERVER_HOST)
    public final String serverHost;

    @JsonProperty(ATTR_SERVER_URL)
    public final String serverURL;

    @JsonProperty(ATTR_ROOM_NAME)
    public final String roomName;

    @JsonProperty(ATTR_SUBJECT)
    public final String subject;

    @JsonProperty(ATTR_ACCESS_TOKEN)
    public final CharSequence accessToken;

    @JsonProperty(ATTR_SDK_TOKEN)
    public final CharSequence sdkToken;

    @JsonProperty(ATTR_ROOM_KEY)
    public final CharSequence roomKey;

    @JsonProperty(ATTR_API_KEY)
    public final CharSequence apiKey;

    @JsonProperty(ATTR_MEETING_ID)
    public final String meetingId;

    @JsonProperty(ATTR_USER_NAME)
    public final String userName;

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_ROOM_DATA)
    public final String additionalRoomData;

    @JsonCreator
    public ProctoringRoomConnection(
            @JsonProperty(ProctoringServiceSettings.ATTR_SERVER_TYPE) final ProctoringServerType proctoringServerType,
            @JsonProperty(ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(ATTR_SERVER_HOST) final String serverHost,
            @JsonProperty(ATTR_SERVER_URL) final String serverURL,
            @JsonProperty(ATTR_ROOM_NAME) final String roomName,
            @JsonProperty(ATTR_SUBJECT) final String subject,
            @JsonProperty(ATTR_ACCESS_TOKEN) final CharSequence accessToken,
            @JsonProperty(ATTR_SDK_TOKEN) final CharSequence sdkToken,
            @JsonProperty(ATTR_ROOM_KEY) final CharSequence roomKey,
            @JsonProperty(ATTR_API_KEY) final CharSequence apiKey,
            @JsonProperty(ATTR_MEETING_ID) final String meetingId,
            @JsonProperty(ATTR_USER_NAME) final String userName,
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_ROOM_DATA) final String additionalRoomData) {

        this.proctoringServerType = proctoringServerType;
        this.connectionToken = connectionToken;
        this.serverHost = serverHost;
        this.serverURL = serverURL;
        this.roomName = roomName;
        this.subject = subject;
        this.accessToken = accessToken;
        this.sdkToken = sdkToken;
        this.roomKey = roomKey;
        this.apiKey = apiKey;
        this.meetingId = meetingId;
        this.userName = userName;
        this.additionalRoomData = additionalRoomData;
    }

    public ProctoringServerType getProctoringServerType() {
        return this.proctoringServerType;
    }

    public String getConnectionToken() {
        return this.connectionToken;
    }

    public String getServerHost() {
        return this.serverHost;
    }

    public CharSequence getAccessToken() {
        return this.accessToken;
    }

    public CharSequence getSdkToken() {
        return this.sdkToken;
    }

    public CharSequence getRoomKey() {
        return this.roomKey;
    }

    public CharSequence getApiKey() {
        return this.apiKey;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getServerURL() {
        return this.serverURL;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getMeetingId() {
        return this.meetingId;
    }

    public String getAdditionalRoomData() {
        return this.additionalRoomData;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SEBProctoringConnectionData [proctoringServerType=");
        builder.append(this.proctoringServerType);
        builder.append(", serverHost=");
        builder.append(this.serverHost);
        builder.append(", serverURL=");
        builder.append(this.serverURL);
        builder.append(", roomName=");
        builder.append(this.roomName);
        builder.append(", subject=");
        builder.append(this.subject);
        builder.append("]");
        return builder.toString();
    }

}
