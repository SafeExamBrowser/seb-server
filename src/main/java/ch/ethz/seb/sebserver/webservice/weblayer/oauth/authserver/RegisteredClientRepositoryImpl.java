/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class RegisteredClientRepositoryImpl implements RegisteredClientRepository {

    private static final Logger log = LoggerFactory.getLogger(RegisteredClientRepositoryImpl.class);

    private final ConnectionConfigurationService sebConnectionConfigurationService;
    private final RegisteredGuiClient registeredGuiClient;
    private final RegisteredLMSClient registeredLMSClient;

    public RegisteredClientRepositoryImpl(
            final ConnectionConfigurationService sebConnectionConfigurationService,
            final RegisteredGuiClient registeredGuiClient, 
            final RegisteredLMSClient registeredLMSClient) {
        
        this.sebConnectionConfigurationService = sebConnectionConfigurationService;
        this.registeredGuiClient = registeredGuiClient;
        this.registeredLMSClient = registeredLMSClient;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Not supported here");
    }

    @Override
    public RegisteredClient findById(String id) {
        return findByClientId(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        System.out.println("******************** findByClientId for: " + clientId);
        if (clientId == null) {
            return null;
        }

        // check if it is valid GUI client
        if (clientId.equals(this.registeredGuiClient.getClientId())) {
            return this.registeredGuiClient.client;
        }
        
        // check if it is valid LMS client
        if (clientId.equals(this.registeredLMSClient.getClientId())) {
            return this.registeredLMSClient.getRegisteredClient();
        }

        // check if it is valid SEB client
        return this.sebConnectionConfigurationService
                .getClientConfigDetails(clientId)
                .onError(error -> log.warn("Active client not found: {} cause: {}", clientId, error.getMessage()))
                .getOr(null);
    }
}
