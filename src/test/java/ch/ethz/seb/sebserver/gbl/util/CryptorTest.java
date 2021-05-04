/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.core.env.Environment;

public class CryptorTest {

    @Test
    public void testEncryptDecrypt() {
        final String clientName = "simpleClientName";
        String encrypted =
                Cryptor.encrypt(clientName, "secret1").getOrThrow().toString();
        String decrypted = Cryptor.decrypt(encrypted, "secret1").getOrThrow().toString();

        assertEquals(clientName, decrypted);

        final String clientSecret = "fbjreij39ru29305ruà££àèLöäöäü65%(/%(ç87";

        encrypted =
                Cryptor.encrypt(clientSecret, "secret1").getOrThrow().toString();
        decrypted = Cryptor.decrypt(encrypted, "secret1").getOrThrow().toString();

        assertEquals(clientSecret, decrypted);
    }

    @Test
    public void testEncryptDecryptService() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getRequiredProperty("sebserver.webservice.internalSecret"))
                .thenReturn("secret1");

        final Cryptor cryptor = new Cryptor(envMock);
        final String clientName = "simpleClientName";

        String encrypted =
                cryptor.encrypt(clientName).getOrThrow().toString();
        String decrypted = cryptor.decrypt(encrypted).getOrThrow().toString();

        assertEquals(clientName, decrypted);

        final String clientSecret = "fbjreij39ru29305ruà££àèLöäöäü65%(/%(ç87";

        encrypted =
                cryptor.encrypt(clientSecret).getOrThrow().toString();
        decrypted = cryptor.decrypt(encrypted).getOrThrow().toString();

        assertEquals(clientSecret, decrypted);
    }

}
