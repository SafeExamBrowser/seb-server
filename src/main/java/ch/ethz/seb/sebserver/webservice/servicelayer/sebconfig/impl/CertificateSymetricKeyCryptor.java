/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

@Lazy
@Component
@WebServiceProfile
public class CertificateSymetricKeyCryptor implements SEBConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(CertificateSymetricKeyCryptor.class);

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PUBLIC_KEY_HASH_SYMMETRIC_KEY);
    private static final int PUBLIC_KEY_HASH_SIZE = 20;
    private static final int ENCRYPTION_KEY_LENGTH = 32;
    private static final int KEY_LENGTH_SIZE = 4;

    private final PasswordEncryptor passwordEncryptor;
    private final PasswordDecryptor passwordDecryptor;

    public CertificateSymetricKeyCryptor(
            final PasswordEncryptor passwordEncryptor,
            final PasswordDecryptor passwordDecryptor) {

        this.passwordEncryptor = passwordEncryptor;
        this.passwordDecryptor = passwordDecryptor;
    }

    @Override
    public Set<Strategy> strategies() {
        return STRATEGIES;
    }

    @Override
    public void encrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous certificate encryption");
        }

        try {

            final Certificate certificate = context.getCertificate();
            final byte[] publicKeyHash = generatePublicKeyHash(certificate);
            final byte[] symetricKey = generateSymetricKey();
            final CharSequence symetricKeyBase64 = Base64.getEncoder().encodeToString(symetricKey);
            final byte[] generateParameter = generateParameter(certificate, publicKeyHash, symetricKey);

            output.write(generateParameter, 0, generateParameter.length);
            this.passwordEncryptor.encrypt(output, input, symetricKeyBase64);

        } catch (final Exception e) {
            log.error("Error while trying to stream and encrypt data: ", e);
        }
    }

    @Override
    public void decrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        // TODO Auto-generated method stub
        try {
            final Certificate certificate = context.getCertificate();
            final byte[] publicKeyHash = parsePublicKeyHash(input);

        } catch (final Exception e) {
            log.error("Error while trying to stream and decrypt data: ", e);
        }

    }

    private byte[] generatePublicKeyHash(final Certificate cert) throws NoSuchAlgorithmException {
        final byte[] publicKey = cert.getPublicKey().getEncoded();
        final MessageDigest md = MessageDigest.getInstance(Constants.SHA_1);
        final byte[] hash = md.digest(publicKey);
        return hash;
    }

    private byte[] generateSymetricKey() throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(Constants.AES);
        keyGenerator.init(ENCRYPTION_KEY_LENGTH);
        return keyGenerator.generateKey().getEncoded();
    }

    @SuppressWarnings("resource")
    private byte[] generateParameter(
            final Certificate cert,
            final byte[] publicKeyHash,
            final byte[] symetricKey) throws Exception {

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        final byte[] encryptedKey = generateEncryptedKey(cert, symetricKey);
        final byte[] encryptedKeyLength = ByteBuffer
                .allocate(KEY_LENGTH_SIZE)
                .putInt(encryptedKey.length)
                .array();

        data.write(publicKeyHash, 0, publicKeyHash.length);
        data.write(encryptedKeyLength, 0, encryptedKeyLength.length);
        data.write(encryptedKey, 0, encryptedKey.length);

        return data.toByteArray();
    }

    private byte[] generateEncryptedKey(final Certificate cert, final byte[] symetricKey) throws Exception {
        final String algorithm = cert.getPublicKey().getAlgorithm();
        final Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.ENCRYPT_MODE, cert);
        final byte[] cipherText = encryptCipher.doFinal(symetricKey);
        return cipherText;
    }

    private byte[] parsePublicKeyHash(final InputStream input) throws IOException {
        final byte[] publicKeyHash = new byte[PUBLIC_KEY_HASH_SIZE];
        input.read(publicKeyHash, 0, publicKeyHash.length);
        return publicKeyHash;
    }

}
