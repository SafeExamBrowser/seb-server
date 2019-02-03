/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.core.env.Environment;

public class InternalEncryptionServiceTest {

    @Test
    public void testEncryptSimpleSecret() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getRequiredProperty(InternalEncryptionService.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY))
                .thenReturn("secret1");

        final InternalEncryptionService service = new InternalEncryptionService(envMock);
        final String encrypt = service.encrypt("text1");
        final String decrypt = service.decrypt(encrypt);
        assertEquals("text1", decrypt);
    }

}
