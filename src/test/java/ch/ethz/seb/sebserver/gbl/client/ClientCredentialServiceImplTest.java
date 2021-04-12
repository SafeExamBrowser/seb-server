/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;

public class ClientCredentialServiceImplTest {

    @Test
    public void testGeneratedClientCredentials() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getRequiredProperty(Cryptor.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY))
                .thenReturn("secret1");

        final Cryptor cryptor = new Cryptor(envMock);
        final String clientName = "simpleClientName";
        final ClientCredentialServiceImpl service = new ClientCredentialServiceImpl(envMock, cryptor);

        final Result<ClientCredentials> clientCredentialsResult = service.generatedClientCredentials();
        assertTrue(clientCredentialsResult.hasValue());
        final ClientCredentials clientCredentials = clientCredentialsResult.get();
        assertNotNull(clientCredentials);
        assertNotNull(clientCredentials.clientId);
        assertNotNull(clientCredentials.secret);
        assertNull(clientCredentials.accessToken);
    }

}
