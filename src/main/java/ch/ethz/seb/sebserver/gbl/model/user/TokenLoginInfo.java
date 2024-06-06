/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class TokenLoginInfo {
    @JsonProperty("username")
    public final String username;
    @JsonProperty("userUUID")
    public final String userUUID;
    @JsonProperty("redirect")
    public final LoginForward login_forward;
    @JsonProperty("login")
    public final OAuth2AccessToken login;

    @JsonCreator
    public TokenLoginInfo(
            @JsonProperty("username") final String username,
            @JsonProperty("userUUID") final String userUUID,
            @JsonProperty("redirect") final LoginForward login_forward,
            @JsonProperty("login") final OAuth2AccessToken login) {

        this.username = username;
        this.userUUID = userUUID;
        this.login_forward = login_forward;
        this.login = login;
    }

}
