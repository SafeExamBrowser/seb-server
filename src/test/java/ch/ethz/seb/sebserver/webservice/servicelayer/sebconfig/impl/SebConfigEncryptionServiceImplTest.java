/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SebConfigEncryptionServiceImpl.EncryptionContext;

public class SebConfigEncryptionServiceImplTest {

    @Test
    public void testPlainText() throws IOException {
        final SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl = sebConfigEncryptionServiceImpl();

        final String config = "<TestConfig></TestConfig>";

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        sebConfigEncryptionServiceImpl
                .streamEncrypted(
                        out,
                        IOUtils.toInputStream(config, "UTF-8"),
                        EncryptionContext.contextOfPlainText());

        final byte[] plainWithHeader = out.toByteArray();
        assertNotNull(plainWithHeader);
        assertEquals("plnd<TestConfig></TestConfig>", Utils.toString(plainWithHeader));

        final ByteArrayOutputStream out2 = new ByteArrayOutputStream(512);
        sebConfigEncryptionServiceImpl.streamDecrypted(
                out2,
                new ByteArrayInputStream(plainWithHeader),
                null,
                null);

        final byte[] byteArray2 = out2.toByteArray();
        assertNotNull(byteArray2);

        final String decryptedConfig = new String(byteArray2, "UTF-8");
        assertEquals(config, decryptedConfig);
    }

    @Test
    public void testPasswordEncryption() throws IOException {
        final SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl = sebConfigEncryptionServiceImpl();

        final String config = "<TestConfig></TestConfig>";
        final String pwd = "password";

        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        sebConfigEncryptionServiceImpl.streamEncrypted(
                out,
                IOUtils.toInputStream(config, "UTF-8"),
                EncryptionContext.contextOf(
                        Strategy.PASSWORD_PWCC,
                        pwd));

        final byte[] byteArray = out.toByteArray();

        //assertFalse(plainText.hasError());
        final ByteBuffer cipher = ByteBuffer.wrap(byteArray);
        assertTrue(Utils.toString(cipher).startsWith(Utils.toString(Strategy.PASSWORD_PWCC.header)));

        final ByteArrayOutputStream out2 = new ByteArrayOutputStream(512);
        sebConfigEncryptionServiceImpl.streamDecrypted(
                out2,
                new ByteArrayInputStream(byteArray),
                () -> pwd,
                null);

        final byte[] byteArray2 = out2.toByteArray();
        assertNotNull(byteArray2);

        final String decryptedConfig = new String(byteArray2, "UTF-8");
        assertEquals(config, decryptedConfig);
    }

    private SebConfigEncryptionServiceImpl sebConfigEncryptionServiceImpl() {
        final List<SebConfigCryptor> encryptors = Arrays.asList(
                new PasswordEncryptor(),
                new NoneEncryptor());
        return new SebConfigEncryptionServiceImpl(encryptors);
    }

}
