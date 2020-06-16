/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentialServiceImpl;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import org.junit.Test;
import org.springframework.core.env.Environment;

public class ClientCredentialServiceTest {

//    @Test
//    public void testEncryptSimpleSecret() {
//        final Environment envMock = mock(Environment.class);
//        when(envMock.getProperty(ClientCredentialServiceImpl.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY))
//                .thenReturn("somePW");
//
//        final ClientCredentialService service = new ClientCredentialServiceImpl(envMock);
//        final CharSequence encrypt = service.encrypt("test");
//        assertEquals("", encrypt.toString());
//    }

    @Test
    public void testEncryptDecryptClientCredentials() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getRequiredProperty(Cryptor.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY))
                .thenReturn("secret1");

        Cryptor cryptor = new Cryptor(envMock);

        final String clientName = "simpleClientName";

        final ClientCredentialServiceImpl service = new ClientCredentialServiceImpl(envMock, cryptor);
        String encrypted =
                cryptor.encrypt(clientName, "secret1").toString();
        String decrypted = cryptor.decrypt(encrypted, "secret1").toString();

        assertEquals(clientName, decrypted);

        final String clientSecret = "fbjreij39ru29305ruà££àèLöäöäü65%(/%(ç87";

        encrypted =
                cryptor.encrypt(clientSecret, "secret1").toString();
        decrypted = cryptor.decrypt(encrypted, "secret1").toString();

        assertEquals(clientSecret, decrypted);
    }

}
