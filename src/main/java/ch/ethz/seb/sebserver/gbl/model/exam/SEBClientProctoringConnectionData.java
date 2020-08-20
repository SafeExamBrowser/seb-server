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

@JsonIgnoreProperties(ignoreUnknown = true)
public class SEBClientProctoringConnectionData {

    public static final String ATTR_SERVER_HOST = "serverHost";
    public static final String ATTR_SERVER_URL = "serverURL";
    public static final String ATTR_ROOM_NAME = "roomName";
    public static final String ATTR_ACCESS_TOKEN = "accessToken";
    public static final String ATTR_CONNECTION_URL = "connectionURL";

    @JsonProperty(ATTR_SERVER_HOST)
    public final String serverHost;

    @JsonProperty(ATTR_SERVER_URL)
    public final String serverURL;

    @JsonProperty(ATTR_ROOM_NAME)
    public final String roomName;

    @JsonProperty(ATTR_ACCESS_TOKEN)
    public final String accessToken;

    @JsonProperty(ATTR_CONNECTION_URL)
    public final String connectionURL;

    @JsonCreator
    public SEBClientProctoringConnectionData(
            @JsonProperty(ATTR_SERVER_HOST) final String serverHost,
            @JsonProperty(ATTR_SERVER_URL) final String serverURL,
            @JsonProperty(ATTR_ROOM_NAME) final String roomName,
            @JsonProperty(ATTR_ACCESS_TOKEN) final String accessToken,
            @JsonProperty(ATTR_CONNECTION_URL) final String connectionURL) {

        this.serverHost = serverHost;
        this.serverURL = serverURL;
        this.roomName = roomName;
        this.accessToken = accessToken;
        this.connectionURL = connectionURL;
    }

    public String getServerHost() {
        return this.serverHost;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getServerURL() {
        return this.serverURL;
    }

    public String getConnectionURL() {
        return this.connectionURL;
    }

    public String getRoomName() {
        return this.roomName;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SEBClientProctoringConnectionData [serverHost=");
        builder.append(this.serverHost);
        builder.append(", serverURL=");
        builder.append(this.serverURL);
        builder.append(", roomName=");
        builder.append(this.roomName);
        builder.append(", accessToken=");
        builder.append(this.accessToken);
        builder.append(", connectionURL=");
        builder.append(this.connectionURL);
        builder.append("]");
        return builder.toString();
    }

}
