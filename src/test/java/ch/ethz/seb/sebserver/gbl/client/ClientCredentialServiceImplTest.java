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

import java.nio.CharBuffer;

import org.junit.Test;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ClientCredentialServiceImplTest {

    @Test
    public void testGeneratedClientCredentials() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getProperty("sebserver.webservice.internalSecret"))
                .thenReturn("secret1");

        final Cryptor cryptor = new Cryptor(envMock);
        final ClientCredentialServiceImpl service = new ClientCredentialServiceImpl(cryptor);

        final Result<ClientCredentials> clientCredentialsResult = service.generatedClientCredentials();
        assertTrue(clientCredentialsResult.hasValue());
        final ClientCredentials clientCredentials = clientCredentialsResult.get();
        assertNotNull(clientCredentials);
        assertNotNull(clientCredentials.clientId);
        assertNotNull(clientCredentials.secret);
        assertNull(clientCredentials.accessToken);
    }

    @Test
    public void testAccessToken() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getProperty("sebserver.webservice.internalSecret"))
                .thenReturn("secret1");

        final Cryptor cryptor = new Cryptor(envMock);
        final ClientCredentialServiceImpl service = new ClientCredentialServiceImpl(cryptor);

        final ClientCredentials cc = service
                .encryptClientCredentials("", "", "")
                .getOrThrow();

        assertNotNull(cc);
        assertNull(cc.accessToken);
        assertNull(cc.secret);
        assertEquals("", cc.clientId);

        final ClientCredentials cc2 = service
                .encryptClientCredentials("c1", "password", "token")
                .getOrThrow();

        assertNotNull(cc2);
        assertNotNull(cc2.accessToken);
        assertNotNull(cc2.secret);
        assertTrue(cc2.accessToken.length() > "token".length());
        assertTrue(cc2.secret.length() > "password".length());
        assertEquals("c1", cc2.clientId);
    }

    @Test
    public void testClearChars() {
        final CharBuffer charBuffer = Utils.toCharBuffer(Utils.toByteBuffer("password"));
        ClientCredentialServiceImpl.clearChars(charBuffer);
        assertEquals("", String.valueOf(charBuffer));
    }

}
