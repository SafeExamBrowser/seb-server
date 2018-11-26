/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.WebSecurityConfig;

/** This defines the Spring's OAuth2 ClientDetails for an Administration API client. */
@Lazy
@Component
public final class AdminAPIClientDetails extends BaseClientDetails {

    private static final long serialVersionUID = 4505193832353978832L;

    public AdminAPIClientDetails(
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.webservice.api.admin.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds}") final Integer accessTokenValiditySeconds,
            @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds}") final Integer refreshTokenValiditySeconds) {

        super(
                clientId,
                WebResourceServerConfiguration.ADMIN_API_RESOURCE_ID,
                "read,write",
                "password,refresh_token",
                null);
        super.setClientSecret(clientPasswordEncoder.encode(clientSecret));
        super.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        super.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
    }

}
