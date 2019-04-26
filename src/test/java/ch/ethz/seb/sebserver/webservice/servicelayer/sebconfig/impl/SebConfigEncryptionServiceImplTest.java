/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;

public class SebConfigEncryptionServiceImplTest {

    @Test
    public void testPlainText() {
        final SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl = sebConfigEncryptionServiceImpl();

        final String config = "<TestConfig></TestConfig>";

        final Result<ByteBuffer> plainText = sebConfigEncryptionServiceImpl.plainText(config);
        assertFalse(plainText.hasError());
        final ByteBuffer cipher = plainText.get();
        assertEquals("plnd<TestConfig></TestConfig>", Utils.toString(cipher));

        final Result<ByteBuffer> decrypt = sebConfigEncryptionServiceImpl.decrypt(cipher, null, null);
        assertFalse(decrypt.hasError());
        assertEquals("<TestConfig></TestConfig>", Utils.toString(decrypt.get()));
    }

    @Test
    public void testPasswordEncryption() {
        final SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl = sebConfigEncryptionServiceImpl();

        final String config = "<TestConfig></TestConfig>";
        final String pwd = "password";

        final Result<ByteBuffer> plainText = sebConfigEncryptionServiceImpl.encryptWithPassword(
                config,
                Strategy.PASSWORD_PWCC,
                pwd);

        assertFalse(plainText.hasError());
        final ByteBuffer cipher = plainText.get();
        assertTrue(Utils.toString(cipher).startsWith(Utils.toString(Strategy.PASSWORD_PWCC.header)));

        final Result<ByteBuffer> decrypt = sebConfigEncryptionServiceImpl.decrypt(cipher, () -> pwd, null);
        assertFalse(decrypt.hasError());
        assertEquals("<TestConfig></TestConfig>", Utils.toString(decrypt.get()));
    }

    private SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl() {
        final JNCryptor jnCryptor = new AES256JNCryptor();
        jnCryptor.setPBKDFIterations(10000);

        final List<SebConfigCryptor> encryptors = Arrays.asList(
                new PasswordEncryptor(jnCryptor));
        return new SebConfigEncryptionServiceImpl(encryptors);
    }

}
