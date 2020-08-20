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

    private final String servletContextPath;
    private final String webserviceServerAddress;
    private final UriComponentsBuilder webserviceURIBuilder;

    public WebserviceURIService(
            @Value("${sebserver.gui.webservice.protocol}") final String webserviceProtocol,
            @Value("${sebserver.gui.webservice.address}") final String webserviceServerAddress,
            @Value("${sebserver.gui.webservice.port}") final String webserviceServerPort,
            @Value("${server.servlet.context-path}") final String servletContextPath,
            @Value("${sebserver.gui.webservice.apipath}") final String webserviceAPIPath) {

        this.servletContextPath = servletContextPath;
        this.webserviceServerAddress =
                webserviceProtocol + "://" + webserviceServerAddress + ":" + webserviceServerPort;
        this.webserviceURIBuilder = UriComponentsBuilder
                .fromHttpUrl(webserviceProtocol + "://" + webserviceServerAddress)
                .port(webserviceServerPort)
                .path(servletContextPath)
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
                .path(this.servletContextPath)
                .path(API.OAUTH_TOKEN_ENDPOINT)
                .toUriString();
    }

    public String getOAuthRevokeTokenURI() {
        return UriComponentsBuilder.fromHttpUrl(this.webserviceServerAddress)
                .path(this.servletContextPath)
                .path(API.OAUTH_REVOKE_TOKEN_ENDPOINT)
                .toUriString();
    }

    public String getCurrentUserRequestURI() {
        return getURIBuilder()
                .path(API.CURRENT_USER_ENDPOINT)
                .toUriString();
    }

    public String getLoginLogPostURI() {
        return getURIBuilder()
                .path(API.USER_ACCOUNT_ENDPOINT + API.LOGIN_PATH_SEGMENT)
                .toUriString();
    }

    public String getLogoutLogPostURI() {
        return getURIBuilder()
                .path(API.USER_ACCOUNT_ENDPOINT + API.LOGOUT_PATH_SEGMENT)
                .toUriString();
    }
}
