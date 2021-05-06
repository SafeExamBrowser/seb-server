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

import org.apache.commons.io.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream;
import org.cryptonode.jncryptor.CryptorException;
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
public class PasswordEncryptor {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncryptor.class);

    public void encrypt(
            final OutputStream output,
            final InputStream input,
            final CharSequence password) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous password encryption");
        }

        OutputStream encryptOutput = null;
        try {

            if (password.length() == 0) {
                encryptOutput = new AES256JNCryptorOutputStreamEmptyPwdSupport(
                        output,
                        Utils.toCharArray(password),
                        Constants.JN_CRYPTOR_ITERATIONS);
            } else {
                encryptOutput = new AES256JNCryptorOutputStream(
                        output,
                        Utils.toCharArray(password),
                        Constants.JN_CRYPTOR_ITERATIONS);
            }

            IOUtils.copyLarge(input, encryptOutput);

        } catch (final CryptorException e) {
            log.error("Error while trying to stream and encrypt data: ", e);
        } catch (final IOException e) {
            log.error("Error while trying to read/write form/to streams: ", e);
        } finally {
            try {
                IOUtils.closeQuietly(input);
                if (encryptOutput != null) {
                    encryptOutput.flush();
                    encryptOutput.close();
                }
            } catch (final IOException e) {
                log.error("Failed to close AES256JNCryptorOutputStream: ", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("*** Finish streaming asynchronous encryption");
            }
        }
    }

}
