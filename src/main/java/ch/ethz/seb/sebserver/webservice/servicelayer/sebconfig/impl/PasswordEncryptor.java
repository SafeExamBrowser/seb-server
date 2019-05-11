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
import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
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

    private final JNCryptor jnCryptor;

    protected PasswordEncryptor(final JNCryptor jnCryptor) {
        this.jnCryptor = jnCryptor;
    }

    @Override
    public Set<Strategy> strategies() {
        return STRATEGIES;
    }

    @Override
    public Result<ByteBuffer> encrypt(final CharSequence plainTextConfig, final SebConfigEncryptionContext context) {
        return Result.tryCatch(() -> {
            return ByteBuffer.wrap(this.jnCryptor.encryptData(
                    Utils.toByteArray(plainTextConfig),
                    Utils.toCharArray(context.getPassword())));
        });
    }

    @Override
    public Result<ByteBuffer> decrypt(final ByteBuffer cipher, final SebConfigEncryptionContext context) {
        return Result.tryCatch(() -> {
            return ByteBuffer.wrap(this.jnCryptor.decryptData(
                    Utils.toByteArray(cipher),
                    Utils.toCharArray(context.getPassword())));
        });
    }

    @Override
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public void encrypt(
            final OutputStream output,
            final InputStream input,
            final SebConfigEncryptionContext context) {

        if (log.isDebugEnabled()) {
            log.debug("*** Start streaming asynchronous encryption of SEB exam configuration data");
        }

        AES256JNCryptorOutputStream encryptOutput = null;
        try {

            encryptOutput = new AES256JNCryptorOutputStream(
                    output,
                    Utils.toCharArray(context.getPassword()));

            IOUtils.copyLarge(input, encryptOutput);

            encryptOutput.close();
            encryptOutput.flush();
            encryptOutput.close();
            output.flush();

        } catch (final CryptorException e) {
            log.error("Error while trying to stream and encrypt seb exam configuration data: ", e);
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
                log.debug("*** Finish streaming asynchronous encryption of SEB exam configuration data");
            }
        }
    }

    @Override
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public void decrypt(
            final OutputStream plainTextOutput,
            final InputStream cipherInputStream,
            final SebConfigEncryptionContext context) {

        // TODO Auto-generated method stub

    }

}
