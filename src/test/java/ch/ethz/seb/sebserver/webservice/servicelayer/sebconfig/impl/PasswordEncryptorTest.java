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
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream;
import org.cryptonode.jncryptor.JNCryptor;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SEBConfigEncryptionServiceImpl.EncryptionContext;

public class PasswordEncryptorTest {

    @Test
    public void conversionTest() throws IOException {
        final String text = "ojnjbiboijnlkncokdnvoiwjife";
        final byte[] byteArray = Utils.toByteArray(text);
        final byte[] otherByteArray = new byte[byteArray.length];
        final InputStream inputStream = IOUtils.toInputStream(text, "UTF-8");
        inputStream.read(otherByteArray);

        assertTrue(Arrays.equals(byteArray, otherByteArray));
    }

    @Test
    public void testUsingPassword() throws Exception {

        final String config = "<TestConfig></TestConfig>";
        final byte[] plaintext = Utils.toByteArray(config);

        final String password = "Testing1234";

        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final AES256JNCryptorOutputStream cryptorStream = new AES256JNCryptorOutputStream(
                byteStream, password.toCharArray());
        cryptorStream.write(plaintext);
        cryptorStream.close();

        final byte[] encrypted = byteStream.toByteArray();

        final JNCryptor cryptor = new AES256JNCryptor();

        final byte[] result = cryptor.decryptData(encrypted, password.toCharArray());
        assertArrayEquals(plaintext, result);
    }

    @Test
    public void test2() throws IOException {
        final JNCryptor cryptor = new AES256JNCryptor();
        final PasswordEncryptor encryptor = new PasswordEncryptor(cryptor);

        final String config = "<TestConfig></TestConfig>";
        final String pwd = "password";
        final ByteArrayOutputStream out = new ByteArrayOutputStream(512);

        final SEBConfigEncryptionContext context = EncryptionContext.contextOf(
                Strategy.PASSWORD_PWCC,
                pwd);

        encryptor.encrypt(
                out,
                IOUtils.toInputStream(config, "UTF-8"),
                context);

        final byte[] byteArray = out.toByteArray();

        final ByteArrayOutputStream out2 = new ByteArrayOutputStream(512);
        encryptor.decrypt(
                out2,
                new ByteArrayInputStream(byteArray),
                context);

        final byte[] byteArray2 = out2.toByteArray();
        assertNotNull(byteArray2);

        final String decryptedConfig = new String(byteArray2, "UTF-8");
        assertEquals(config, decryptedConfig);
    }

}
