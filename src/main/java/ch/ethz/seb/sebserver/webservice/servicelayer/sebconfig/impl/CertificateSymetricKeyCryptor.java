/*
 * Copyright (c) 2021 ETH Zürich, IT Services
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
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Set;

import javax.crypto.KeyGenerator;

import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.CertificateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

@Lazy
@Component
@WebServiceProfile
/** Uses symetric key for encryption of the data and a certificate asymetric key to encrypt
 * and decrypt the symetric key. All is put together as described here:
 * https://www.safeexambrowser.org/developer/seb-file-format.html
 *
 * <pre>
 * | “phsk” | public key hash | key length   | encrypted key  |   ... encrypted data ... |
 * |  0 - 3 |       4 - 23    |    24 - 27   |   28 - (28+kl) | (29+kl) - n              |
 * </pre>
 */
public class CertificateSymetricKeyCryptor extends AbstractCertificateCryptor implements SEBConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(CertificateSymetricKeyCryptor.class);

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PUBLIC_KEY_HASH_SYMMETRIC_KEY);

    private final PasswordEncryptor passwordEncryptor;
    private final PasswordDecryptor passwordDecryptor;

    public CertificateSymetricKeyCryptor(
            final PasswordEncryptor passwordEncryptor,
            final PasswordDecryptor passwordDecryptor,
            final CertificateService certificateService,
            final Cryptor cryptor) {

        super(certificateService, cryptor);
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
            log.debug("*** Start streaming asynchronous certificate symmetric-key encryption");
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
        } finally {
            IOUtils.closeQuietly(input);
            try {
                output.flush();
                output.close();
            } catch (final Exception e) {
                log.error("Failed to close output: ", e);
            }
        }
    }

    @Override
    public void decrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous certificate symmetric-key decryption");
        }

        try {

            final byte[] publicKeyHash = parsePublicKeyHash(input);
            final SEBConfigEncryptionContext sebConfigEncryptionContext = getCertificateByPublicKeyHash(
                    context,
                    publicKeyHash);
            final Certificate certificate = sebConfigEncryptionContext.getCertificate();

            if (certificate == null) {
                throw new RuntimeException("No matching certificate found to decrypt the configuration");
            }

            final byte[] encryptedKey = getEncryptedKey(input);
            final byte[] symetricKey = decryptWithCert(sebConfigEncryptionContext, encryptedKey);
            final CharSequence symetricKeyBase64 = Base64.getEncoder().encodeToString(symetricKey);

            this.passwordDecryptor.decrypt(output, input, symetricKeyBase64);

        } catch (final Exception e) {
            log.error("Error while trying to stream and decrypt data: ", e);
        } finally {
            try {
                output.flush();
                output.close();
            } catch (final Exception e) {
                log.error("Failed to close output: ", e);
            }
        }
    }

    private byte[] getEncryptedKey(final InputStream input) throws IOException {

        // first get the length of the encrypted key
        final byte[] keyLength = new byte[KEY_LENGTH_SIZE];
        input.read(keyLength, 0, keyLength.length);
        final int keyLengthInt = ByteBuffer
                .wrap(keyLength)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();

        // then get the encrypted symmetric key
        final byte[] encryptedKey = new byte[keyLengthInt];
        input.read(encryptedKey, 0, encryptedKey.length);
        return encryptedKey;
    }

    private byte[] generateSymetricKey() throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(Constants.AES);
        keyGenerator.init(ENCRYPTION_KEY_BITS);
        final byte[] encoded = keyGenerator.generateKey().getEncoded();
        return encoded;
    }

    @SuppressWarnings("resource")
    private byte[] generateParameter(
            final Certificate cert,
            final byte[] publicKeyHash,
            final byte[] symetricKey) throws Exception {

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        final byte[] encryptedKey = encryptWithCert(cert, symetricKey);
        final byte[] encryptedKeyLength = ByteBuffer
                .allocate(KEY_LENGTH_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(encryptedKey.length)
                .array();

        data.write(publicKeyHash, 0, publicKeyHash.length);
        data.write(encryptedKeyLength, 0, encryptedKeyLength.length);
        data.write(encryptedKey, 0, encryptedKey.length);
        final byte[] byteArray = data.toByteArray();

        return byteArray;
    }

    @Override
    protected Strategy getStrategy() {
        return Strategy.PUBLIC_KEY_HASH_SYMMETRIC_KEY;
    }

}
