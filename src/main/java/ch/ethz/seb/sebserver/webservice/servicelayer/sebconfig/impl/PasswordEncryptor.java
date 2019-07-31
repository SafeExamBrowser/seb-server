/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptorInputStream;
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream;
import org.cryptonode.jncryptor.CryptorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;

@Lazy
@Component
@WebServiceProfile
public class PasswordEncryptor implements SebConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncryptor.class);

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PASSWORD_PSWD,
            Strategy.PASSWORD_PWCC);

    @Override
    public Set<Strategy> strategies() {
        return STRATEGIES;
    }

    @Override
    public void encrypt(
            final OutputStream output,
            final InputStream input,
            final SebConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous encryption");
        }

        AES256JNCryptorOutputStream encryptOutput = null;
        try {

            encryptOutput = new AES256JNCryptorOutputStream(
                    output,
                    Utils.toCharArray(context.getPassword()),
                    10000);

            IOUtils.copyLarge(input, encryptOutput);

            input.close();
            encryptOutput.flush();

        } catch (final CryptorException e) {
            log.error("Error while trying to stream and encrypt data: ", e);
        } catch (final IOException e) {
            log.error("Error while trying to read/write form/to streams: ", e);
        } finally {
            try {
                if (encryptOutput != null)
                    encryptOutput.close();
            } catch (final IOException e) {
                log.error("Failed to close AES256JNCryptorOutputStream: ", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("*** Finish streaming asynchronous encryption");
            }
        }
    }

    @Override
    public void decrypt(
            final OutputStream output,
            final InputStream input,
            final SebConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous decryption");
        }

        AES256JNCryptorInputStream encryptInput = null;
        try {

            encryptInput = new AES256JNCryptorInputStream(
                    input,
                    Utils.toCharArray(context.getPassword()));

            IOUtils.copyLarge(encryptInput, output);

            encryptInput.close();
            output.flush();
            output.close();

        } catch (final IOException e) {
            log.error("Error while trying to read/write form/to streams: ", e);
        } finally {
            try {
                if (encryptInput != null)
                    encryptInput.close();
            } catch (final IOException e) {
                log.error("Failed to close AES256JNCryptorOutputStream: ", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("*** Finish streaming asynchronous decryption");
            }
        }
    }

}
