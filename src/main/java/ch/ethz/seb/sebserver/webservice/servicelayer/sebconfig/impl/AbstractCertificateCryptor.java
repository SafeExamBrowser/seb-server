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
import java.security.cert.Certificate;

import javax.crypto.Cipher;

import org.apache.commons.codec.digest.DigestUtils;

public abstract class AbstractCertificateCryptor {

    protected static final int PUBLIC_KEY_HASH_SIZE = 20;
    protected static final int ENCRYPTION_KEY_BITS = 256;
    protected static final int KEY_LENGTH_SIZE = 4;

    protected byte[] generatePublicKeyHash(final Certificate cert) {

        try {
            final org.bouncycastle.asn1.x509.Certificate bcCert =
                    org.bouncycastle.asn1.x509.Certificate.getInstance(cert.getEncoded());
            final byte[] bytes = bcCert.getSubjectPublicKeyInfo().getPublicKeyData().getBytes();
            return DigestUtils.sha1(bytes);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to generate public key hash:" + e.getMessage(), e);
        }
    }

    protected byte[] encryptWithCert(final Certificate cert, final byte[] data) throws Exception {
        return encryptWithCert(cert, data, data.length);
    }

    protected byte[] encryptWithCert(final Certificate cert, final byte[] data, final int length) throws Exception {
        final String algorithm = cert.getPublicKey().getAlgorithm();
        final Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.ENCRYPT_MODE, cert);
        return encryptCipher.doFinal(data, 0, length);
    }

    protected byte[] decryptWithCert(final Certificate cert, final byte[] encryptedData) throws Exception {
        final String algorithm = cert.getPublicKey().getAlgorithm();
        final Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.DECRYPT_MODE, cert);
        return encryptCipher.doFinal(encryptedData);
    }

    protected byte[] parsePublicKeyHash(final InputStream input) throws IOException {
        final byte[] publicKeyHash = new byte[PUBLIC_KEY_HASH_SIZE];
        input.read(publicKeyHash, 0, publicKeyHash.length);
        return publicKeyHash;
    }

}
