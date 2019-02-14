/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Component
@GuiProfile
public class WebserviceURIService {

    private static final String OAUTH_TOKEN_URI_PATH = "oauth/token"; // TODO to config properties?
    private static final String OAUTH_REVOKE_TOKEN_URI_PATH = "/oauth/revoke-token"; // TODO to config properties?
    private static final String CURRENT_USER_URI_PATH = API.USER_ACCOUNT_ENDPOINT + "/me";

    private final String webserviceServerAddress;
    private final UriComponentsBuilder webserviceURIBuilder;

    public WebserviceURIService(
            @Value("${sebserver.gui.webservice.protocol}") final String webserviceProtocol,
            @Value("${sebserver.gui.webservice.address}") final String webserviceServerAdress,
            @Value("${sebserver.gui.webservice.port}") final String webserviceServerPort,
            @Value("${sebserver.gui.webservice.apipath}") final String webserviceAPIPath) {

        this.webserviceServerAddress = webserviceProtocol + "://" + webserviceServerAdress + ":" + webserviceServerPort;
        this.webserviceURIBuilder = UriComponentsBuilder
                .fromHttpUrl(webserviceProtocol + "://" + webserviceServerAdress)
                .port(webserviceServerPort)
                .path(webserviceAPIPath);
    }

    public String getWebserviceServerAddress() {
        return this.webserviceServerAddress;
    }

    public UriComponentsBuilder getURIBuilder() {
        return this.webserviceURIBuilder.cloneBuilder();
    }

    public String getOAuthTokenURI() {
        return UriComponentsBuilder.fromHttpUrl(this.webserviceServerAddress)
                .path(OAUTH_TOKEN_URI_PATH)
                .toUriString();
    }

    public String getOAuthRevokeTokenURI() {
        return UriComponentsBuilder.fromHttpUrl(this.webserviceServerAddress)
                .path(OAUTH_REVOKE_TOKEN_URI_PATH)
                .toUriString();
    }

    public String getCurrentUserRequestURI() {
        return getURIBuilder()
                .path(CURRENT_USER_URI_PATH)
                .toUriString();
    }
}
