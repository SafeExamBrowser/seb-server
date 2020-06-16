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
public class NoneEncryptor implements SEBConfigCryptor {

    private static final Logger log = LoggerFactory.getLogger(NoneEncryptor.class);

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PLAIN_TEXT);

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
            log.debug("No encryption, write plain input data");
        }

        try {

            IOUtils.copyLarge(input, output);

        } catch (final IOException e) {
            log.error("Error while streaming plain data to output: ", e);
        } finally {
            try {
                input.close();
                output.flush();
                output.close();
            } catch (final IOException e) {
                log.error("Failed to close InputStream and OutputStream");
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished with no encryption. Close input stream");
            }
        }
    }

    @Override
    public void decrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("No decryption, read plain input data");
        }

        try {

            IOUtils.copyLarge(input, output);

            input.close();
            output.flush();
            output.close();

        } catch (final IOException e) {
            log.error("Error while streaming plain data to output: ", e);
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
                log.error("Failed to close InputStream");
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished with no encryption. Close input stream");
            }
        }

    }

}
