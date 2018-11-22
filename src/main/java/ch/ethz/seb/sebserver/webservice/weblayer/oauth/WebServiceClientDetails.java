/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.WebSecurityConfig;

@Lazy
@Component
public class WebServiceClientDetails implements ClientDetailsService {

    private static final Logger log = LoggerFactory.getLogger(WebServiceClientDetails.class);

    public static final String[] SEB_CLIENT_GRANT_TYPES = new String[] { "client_credentials", "refresh_token" };
    public static final String[] SEB_CLIENT_SCOPES = new String[] { "web-service-api-read", "web-service-api-write" };
    public static final String RESOURCE_ID = "seb-server-seb-client-api";

    @Value("${sebserver.oauth.clients.guiClient.accessTokenValiditySeconds}")
    private Integer guiClientAccessTokenValiditySeconds;
    @Value("${sebserver.oauth.clients.guiClient.refreshTokenValiditySeconds}")
    private Integer guiClientRefreshTokenValiditySeconds;

    private final GuiClientDetails guiClientDetails;
    @Autowired
    @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME)
    private PasswordEncoder clientPasswordEncoder;

    public WebServiceClientDetails(final GuiClientDetails guiClientDetails) {
        this.guiClientDetails = guiClientDetails;
    }

    @Override
    public ClientDetails loadClientByClientId(final String clientId) throws ClientRegistrationException {
        if (clientId == null) {
            throw new ClientRegistrationException("clientId is null");
        }

        if (clientId.equals(this.guiClientDetails.getClientId())) {
            return this.guiClientDetails;
        }

        final ClientDetails forSEBClientAPI = getForSEBClientAPI(clientId);
        if (forSEBClientAPI != null) {
            return forSEBClientAPI;
        }

        log.warn("ClientDetails for clientId: {} not found", clientId);
        throw new ClientRegistrationException("clientId not found");
    }

    private ClientDetails getForSEBClientAPI(final String clientId) {
        // TODO create ClientDetails from matching LMSSetup
        final BaseClientDetails baseClientDetails = new BaseClientDetails(
                clientId,
                RESOURCE_ID,
                "read,write",
                "client_credentials,refresh_token", "");
        baseClientDetails.setClientSecret(this.clientPasswordEncoder.encode("test"));
        return baseClientDetails;
    }

}
