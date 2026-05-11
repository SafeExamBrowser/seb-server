/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.api.API;
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
public class RegisteredGuiClient {
    
        public final RegisteredClient client;

        public RegisteredGuiClient(
            final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.webservice.api.admin.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds:3600}") final Integer accessTokenValiditySeconds,
            @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds:86400}") final Integer refreshTokenValiditySeconds) {

            final Duration refreshTokenValDuration = Duration.of(refreshTokenValiditySeconds, SECONDS);

            client = RegisteredClient
                    .withId(clientId)
                    .clientId(clientId)
                    .clientSecret(clientPasswordEncoder.encode(clientSecret))
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientSecretExpiresAt(null)
                    .scope(API.READ_SCOPE_NAME)
                    .scope(API.WRITE_SCOPE_NAME)
                    .scope(API.WEB_API_SCOPE_NAME)
                    .tokenSettings(TokenSettings
                            .builder()
                            .accessTokenTimeToLive(Duration.of(accessTokenValiditySeconds, SECONDS))
                            .refreshTokenTimeToLive(refreshTokenValDuration)
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
