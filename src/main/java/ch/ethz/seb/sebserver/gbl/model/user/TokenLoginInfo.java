/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class TokenLoginInfo {
    @JsonProperty("username")
    public final String username;
    @JsonProperty("userUUID")
    public final String userUUID;
    @JsonProperty("redirect")
    public final EntityKey redirect;
    @JsonProperty("login")
    public final OAuth2AccessToken login;

    @JsonCreator
    public TokenLoginInfo(
            @JsonProperty("username") final String username,
            @JsonProperty("userUUID") final String userUUID,
            @JsonProperty("redirect") final EntityKey redirect,
            @JsonProperty("login") final OAuth2AccessToken login) {

        this.username = username;
        this.userUUID = userUUID;
        this.redirect = redirect;
        this.login = login;
    }
}
