/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;

/** This defines the Spring's OAuth2 ClientDetails for an Administration API client. */
@Lazy
@Component
public final class AdminAPIClientDetails extends BaseClientDetails {

    private static final long serialVersionUID = 4505193832353978832L;

    public AdminAPIClientDetails(
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.webservice.api.admin.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds:3600}") final Integer accessTokenValiditySeconds,
            @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds:-1}") final Integer refreshTokenValiditySeconds) {

        super(
                clientId,
                WebserviceResourceConfiguration.ADMIN_API_RESOURCE_ID,
                StringUtils.joinWith(
                        Constants.LIST_SEPARATOR,
                        Constants.OAUTH2_SCOPE_READ,
                        Constants.OAUTH2_SCOPE_WRITE),
                StringUtils.joinWith(
                        Constants.LIST_SEPARATOR,
                        Constants.OAUTH2_GRANT_TYPE_PASSWORD,
                        Constants.OAUTH2_GRANT_TYPE_REFRESH_TOKEN),
                null);
        super.setClientSecret(clientPasswordEncoder.encode(clientSecret));
        super.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        super.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
    }

}
