/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.CertificateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

@Lazy
@Component
@WebServiceProfile
public class CertificateAsymetricKeyCryptor extends AbstractCertificateCryptor implements SEBConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(CertificateAsymetricKeyCryptor.class);

    private static final int BUFFER_LENGTH = 128;
    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PUBLIC_KEY_HASH);

    public CertificateAsymetricKeyCryptor(
            final CertificateService certificateService,
            final Cryptor cryptor) {

        super(certificateService, cryptor);
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
            log.debug("*** Start streaming asynchronous certificate asymmetric-key encryption");
        }

        try {

            final Certificate certificate = context.getCertificate();
            final byte[] publicKeyHash = generatePublicKeyHash(certificate);

            output.write(publicKeyHash, 0, publicKeyHash.length);

            final byte[] buffer = new byte[BUFFER_LENGTH];

            int readBytes = input.read(buffer, 0, buffer.length);
            while (readBytes > 0) {
                final byte[] encryptedBlock = encryptWithCert(certificate, buffer, readBytes);
                output.write(encryptedBlock, 0, encryptedBlock.length);
                readBytes = input.read(buffer, 0, buffer.length);
            }

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

        try {
            final byte[] publicKeyHash = parsePublicKeyHash(input);
            final SEBConfigEncryptionContext sebConfigEncryptionContext = getCertificateByPublicKeyHash(
                    context,
                    publicKeyHash);
            final Certificate certificate = sebConfigEncryptionContext.getCertificate();

            if (certificate == null) {
                throw new RuntimeException("No matching certificate found to decrypt the configuration");
            }

            final byte[] buffer = new byte[BUFFER_LENGTH];

            int readBytes = input.read(buffer, 0, buffer.length);
            while (readBytes > 0) {
                final byte[] encryptedBlock = decryptWithCert(sebConfigEncryptionContext, buffer, readBytes);
                output.write(encryptedBlock, 0, encryptedBlock.length);
                readBytes = input.read(buffer, 0, buffer.length);
            }

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

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous certificate asymmetric-key decryption");
        }
    }

}
