/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.util.Result;

public class ClientHttpRequestFactoryServiceTest {

    @Mock
    Environment environment;
    @Mock
    ClientCredentialService clientCredentialService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetClientHttpRequestFactory() {

        final ClientHttpRequestFactoryService clientHttpRequestFactoryService = new ClientHttpRequestFactoryService(
                this.environment,
                this.clientCredentialService,
                1, 1, 1);

        final ProxyData proxyData = new ProxyData("testPoxy", 8000, new ClientCredentials("test", "test"));

        Mockito.when(this.environment.getActiveProfiles()).thenReturn(new String[] { "dev-gui", "test" });
        Mockito.when(this.clientCredentialService.getPlainClientSecret(Mockito.any())).thenReturn(Result.of("test"));

        Result<ClientHttpRequestFactory> clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory();

        assertNotNull(clientHttpRequestFactory);
        assertFalse(clientHttpRequestFactory.hasError());
        ClientHttpRequestFactory instance = clientHttpRequestFactory.get();
        assertTrue(instance instanceof HttpComponentsClientHttpRequestFactory);

        clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory(proxyData);

        assertNotNull(clientHttpRequestFactory);
        assertFalse(clientHttpRequestFactory.hasError());
        instance = clientHttpRequestFactory.get();
        assertTrue(instance instanceof HttpComponentsClientHttpRequestFactory);

        Mockito.when(this.environment.getActiveProfiles()).thenReturn(new String[] { "prod-gui", "prod-ws" });

        clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory();

        assertNotNull(clientHttpRequestFactory);
        assertFalse(clientHttpRequestFactory.hasError());
        instance = clientHttpRequestFactory.get();
        assertTrue(instance instanceof HttpComponentsClientHttpRequestFactory);

        clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory(proxyData);

        assertNotNull(clientHttpRequestFactory);
        assertFalse(clientHttpRequestFactory.hasError());
        instance = clientHttpRequestFactory.get();
        assertTrue(instance instanceof HttpComponentsClientHttpRequestFactory);
    }

}
