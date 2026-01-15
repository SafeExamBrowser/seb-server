/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

import static java.time.temporal.ChronoUnit.SECONDS;

@Lazy
@Component
public class RegisteredLMSClient {

    public final RegisteredClient client;

    public RegisteredLMSClient(
            final PasswordEncoder clientPasswordEncoder,
            final LmsSetupDAO lmsSetupDAO,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.lms.api.accessTokenValiditySeconds:-1}") final Integer accessTokenValiditySeconds) {


        final String joinIds = StringUtils.join(
                lmsSetupDAO.allIdsFullIntegration().getOrThrow(),
                Constants.LIST_SEPARATOR
        );

        client = RegisteredClient
                .withId(clientId)
                .clientId(clientId)
                .clientSecret(clientPasswordEncoder.encode(clientSecret))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
               // .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientSecretExpiresAt(null)
                .scope(joinIds)
                .tokenSettings(TokenSettings
                        .builder()
                        .accessTokenTimeToLive(Duration.of(accessTokenValiditySeconds, SECONDS))
                        .build())
                .build();
    }

    public String getClientId() {
        return client.getClientId();
    }
    
    public UserDetails getUserDetails() {
        return new User(
                client.getClientId(),
                client.getClientSecret(),
                Collections.emptyList());
    }
}
