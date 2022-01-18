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
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.CertificateService;

public abstract class AbstractCertificateCryptor {

    private static final Logger log = LoggerFactory.getLogger(AbstractCertificateCryptor.class);

    protected static final int PUBLIC_KEY_HASH_SIZE = 20;
    protected static final int ENCRYPTION_KEY_BITS = 256;
    protected static final int KEY_LENGTH_SIZE = 4;

    protected final CertificateService certificateService;

    public AbstractCertificateCryptor(final CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    protected Certificate getCertificateByPublicKeyHash(final Long institutionId, final byte[] publicKeyHash) {
        try {

            final Certificates certs = this.certificateService
                    .getCertificates(institutionId)
                    .getOrThrow();

            @SuppressWarnings("unchecked")
            final Enumeration<String> engineAliases = certs.keyStore.engineAliases();
            while (engineAliases.hasMoreElements()) {
                final Certificate certificate = certs.keyStore.engineGetCertificate(engineAliases.nextElement());
                final byte[] otherPublicKeyHash = generatePublicKeyHash(certificate);
                if (Arrays.equals(otherPublicKeyHash, publicKeyHash)) {
                    return certificate;
                }
            }

            return null;

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get certificate by public key hash: ", e);
            return null;
        }
    }

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

    protected byte[] encryptWithCert(
            final Certificate cert,
            final byte[] data,
            final int length) throws Exception {

        final String algorithm = cert.getPublicKey().getAlgorithm();
        final Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.ENCRYPT_MODE, cert.getPublicKey());
        return encryptCipher.doFinal(data, 0, length);
    }

    protected byte[] decryptWithCert(final Certificate cert, final byte[] encryptedData) throws Exception {
        return decryptWithCert(cert, encryptedData, encryptedData.length);
    }

    protected byte[] decryptWithCert(
            final Certificate cert,
            final byte[] encryptedData, final int length) throws Exception {

        final String algorithm = cert.getPublicKey().getAlgorithm();
        final Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.DECRYPT_MODE, cert.getPublicKey());
        return encryptCipher.doFinal(encryptedData, 0, length);
    }

    protected byte[] parsePublicKeyHash(final InputStream input) throws IOException {
        final byte[] publicKeyHash = new byte[PUBLIC_KEY_HASH_SIZE];
        input.read(publicKeyHash, 0, publicKeyHash.length);
        return publicKeyHash;
    }

}
