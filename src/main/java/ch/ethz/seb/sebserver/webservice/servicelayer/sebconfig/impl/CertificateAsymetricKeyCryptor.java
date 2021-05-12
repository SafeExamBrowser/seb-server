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
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

@Lazy
@Component
@WebServiceProfile
public class CertificateAsymetricKeyCryptor extends AbstractCertificateCryptor implements SEBConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(CertificateAsymetricKeyCryptor.class);

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PUBLIC_KEY_HASH);

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

            final byte[] buffer = new byte[128];

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

        // TODO Auto-generated method stub
        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous certificate asymmetric-key decryption");
        }
    }

}
