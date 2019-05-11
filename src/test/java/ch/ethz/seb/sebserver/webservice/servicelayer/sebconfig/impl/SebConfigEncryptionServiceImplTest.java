/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;

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
    public void testPasswordEncryption() throws IOException {
        final SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl = sebConfigEncryptionServiceImpl();

        final String config = "<TestConfig></TestConfig>";
        final String pwd = "password";

        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        sebConfigEncryptionServiceImpl.streamEncryption(
                out,
                IOUtils.toInputStream(config, "UTF-8"),
                Strategy.PASSWORD_PWCC,
                pwd);

        final byte[] byteArray = out.toByteArray();

        //assertFalse(plainText.hasError());
        final ByteBuffer cipher = ByteBuffer.wrap(byteArray);
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
