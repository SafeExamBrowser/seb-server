/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import org.apache.commons.io.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptorInputStream;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@Lazy
@Component
@WebServiceProfile
public class PasswordDecryptor {

    private static final Logger log = LoggerFactory.getLogger(PasswordDecryptor.class);

    private final JNCryptor cryptor;

    public PasswordDecryptor(final JNCryptor cryptor) {
        this.cryptor = cryptor;
    }

    void decrypt(
            final OutputStream output,
            final InputStream input,
            final CharSequence password) {

        try {
            final byte[] version = new byte[Constants.JN_CRYPTOR_VERSION_HEADER_SIZE];
            final int read = input.read(version);
            if (read != Constants.JN_CRYPTOR_VERSION_HEADER_SIZE) {
                throw new IllegalArgumentException("Failed to verify RNCrypt version from input stream file header.");
            }

            final SequenceInputStream sequenceInputStream = new SequenceInputStream(
                    new ByteArrayInputStream(version),
                    input);

            if (version[0] == 3) {

                if (log.isDebugEnabled()) {
                    log.debug("*** Start streaming asynchronous password decryption");
                }

                AES256JNCryptorInputStream encryptInput = null;
                try {

                    encryptInput = new AES256JNCryptorInputStream(
                            sequenceInputStream,
                            Utils.toCharArray(password));

                    IOUtils.copyLarge(encryptInput, output);

                } catch (final IOException e) {
                    log.error("Error while trying to read/write form/to streams: ", e);
                } finally {
                    IOUtils.closeQuietly(encryptInput);
                }
            } else {
                // AES256JNCryptorInputStream supports only decryption of AES256 version 3 encrypted data
                // Workaround: stop streaming and use AES256JNCryptor which supports both, version 2 and 3
                log.info("Trying to decrypt with AES256JNCryptor by load all data into memory...");

                try {

                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(sequenceInputStream, out);
                    final byte[] ciphertext = out.toByteArray();

                    final byte[] decryptData = this.cryptor.decryptData(ciphertext, Utils.toCharArray(password));
                    final ByteArrayInputStream decryptedIn = new ByteArrayInputStream(decryptData);
                    IOUtils.copyLarge(decryptedIn, output);

                } catch (final IOException | CryptorException e) {
                    log.error("Error while trying to none-streaming decrypt: ", e);
                }
            }
        } catch (final IOException e) {
            log.error("Unexpected error while decryption: ", e);
        } finally {
            try {
                output.flush();
                output.close();
            } catch (final IOException e) {
                log.error("Failed to close streams");
            }

            if (log.isDebugEnabled()) {
                log.debug("*** Finish streaming asynchronous decryption");
            }
        }
    }

}
