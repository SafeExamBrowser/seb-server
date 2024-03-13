/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Defines a simple data bean holding (encrypted) client credentials */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClientCredentials {

    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_SECRET = "secret";
    public static final String ATTR_ACCESS_TOKEN = "accessToken";

    /** The client id or client name parameter */
    @JsonProperty(ATTR_CLIENT_ID)
    public final CharSequence clientId;
    /** The client secret parameter */
    @JsonProperty(ATTR_SECRET)
    public final CharSequence secret;
    /** An client access token if supported */
    @JsonProperty(ATTR_ACCESS_TOKEN)
    public final CharSequence accessToken;

    @JsonCreator
    public ClientCredentials(
            @JsonProperty(ATTR_CLIENT_ID) final CharSequence clientId,
            @JsonProperty(ATTR_SECRET) final CharSequence secret,
            @JsonProperty(ATTR_ACCESS_TOKEN) final CharSequence accessToken) {

        this.clientId = clientId;
        this.secret = secret;
        this.accessToken = accessToken;
    }

    public ClientCredentials(
            final CharSequence clientId,
            final CharSequence secret) {

        this(clientId, secret, null);
    }

    public CharSequence getClientId() {
        return this.clientId;
    }

    public CharSequence getSecret() {
        return this.secret;
    }

    public CharSequence getAccessToken() {
        return this.accessToken;
    }

    @JsonIgnore
    public boolean hasClientId() {
        return this.clientId != null && this.clientId.length() > 0;
    }

    @JsonIgnore
    public boolean hasSecret() {
        return this.secret != null && this.secret.length() > 0;
    }

    @JsonIgnore
    public boolean hasAccessToken() {
        return this.accessToken != null && this.accessToken.length() > 0;
    }

    @JsonIgnore
    public String clientIdAsString() {
        return hasClientId() ? this.clientId.toString() : null;
    }

    @JsonIgnore
    public String secretAsString() {
        return hasSecret() ? this.secret.toString() : null;
    }

    @JsonIgnore
    public String accessTokenAsString() {
        return hasAccessToken() ? this.accessToken.toString() : null;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientCredentials [clientId=");
        builder.append(this.clientId);
        builder.append("]");
        return builder.toString();
    }

}