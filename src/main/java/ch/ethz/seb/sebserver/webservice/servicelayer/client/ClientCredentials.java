/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

public final class ClientCredentials {
    public final String clientId;
    public final String secret;
    public final String accessToken;

    public ClientCredentials(
            final String clientId, 
            final String secret, 
            final String accessToken) {
        
        this.clientId = clientId;
        this.secret = secret;
        this.accessToken = accessToken;
    }
}