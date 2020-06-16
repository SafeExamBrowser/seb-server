/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

/** Defines a simple data bean holding (encrypted) client credentials */
public final class ClientCredentials {

    /** The client id or client name parameter */
    public final CharSequence clientId;
    /** The client secret parameter */
    public final CharSequence secret;
    /** An client access token if supported */
    public final CharSequence accessToken;

    public ClientCredentials(
            final CharSequence clientId,
            final CharSequence secret,
            final CharSequence accessToken) {

        this.clientId = clientId;
        this.secret = secret;
        this.accessToken = accessToken;
    }

    public ClientCredentials(
            final CharSequence clientId,
            final CharSequence secret) {

        this(clientId, secret, null);
    }

    public boolean hasClientId() {
        return this.clientId != null && this.clientId.length() > 0;
    }

    public boolean hasSecret() {
        return this.secret != null && this.secret.length() > 0;
    }

    public boolean hasAccessToken() {
        return this.accessToken != null && this.accessToken.length() > 0;
    }

    public String clientIdAsString() {
        return hasClientId() ? this.clientId.toString() : null;
    }

    public String secretAsString() {
        return hasSecret() ? this.secret.toString() : null;
    }

    public String accessTokenAsString() {
        return hasAccessToken() ? this.accessToken.toString() : null;
    }
}