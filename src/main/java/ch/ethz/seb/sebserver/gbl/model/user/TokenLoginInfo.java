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
    @JsonProperty
    public final String username;
    @JsonProperty
    public final String userUUID;
    @JsonProperty
    public final String redirect;
    @JsonProperty
    public final OAuth2AccessToken login;

    @JsonCreator
    public TokenLoginInfo(
            @JsonProperty final String username,
            @JsonProperty final String userUUID,
            @JsonProperty final String redirect,
            @JsonProperty final OAuth2AccessToken login) {

        this.username = username;
        this.userUUID = userUUID;
        this.redirect = redirect;
        this.login = login;
    }
}
