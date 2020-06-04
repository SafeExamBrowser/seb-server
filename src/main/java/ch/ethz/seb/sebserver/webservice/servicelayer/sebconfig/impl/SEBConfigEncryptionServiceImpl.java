/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService;

@Lazy
@Service
@WebServiceProfile
public final class SEBConfigEncryptionServiceImpl implements SEBConfigEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(SEBConfigEncryptionServiceImpl.class);

    private final Map<Strategy, SEBConfigCryptor> encryptors;

    public SEBConfigEncryptionServiceImpl(
            final Collection<SEBConfigCryptor> encryptors) {

        this.encryptors = encryptors
                .stream()
                .flatMap(e -> e.strategies()
                        .stream()
                        .map(s -> new ImmutablePair<>(s, e)))
                .collect(Collectors.toMap(p -> p.left, p -> p.right));
    }

    @Override
    public void streamEncrypted(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        final Strategy strategy = context.getStrategy();
        PipedOutputStream pout = null;
        PipedInputStream pin = null;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            if (log.isDebugEnabled()) {
                log.debug("Password encryption with strategy: {}", strategy);
            }

            output.write(strategy.header);

            getEncryptor(strategy)
                    .getOrThrow()
                    .encrypt(pout, input, context);

            IOUtils.copyLarge(pin, output);

        } catch (final IOException e) {
            log.error("Error while stream encrypted data: ", e);
        } finally {
            try {
                if (pin != null) {
                    pin.close();
                }
                if (pout != null) {
                    pout.flush();
                    pout.close();
                }
                output.flush();
                output.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
        }
    }

    @Override
    public Future<Exception> streamDecrypted(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context) {

        PipedOutputStream pout = null;
        PipedInputStream pin = null;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            Strategy strategy = null;
            final byte[] header = new byte[Constants.SEB_FILE_HEADER_SIZE];
            final int read = input.read(header);
            if (read != Constants.SEB_FILE_HEADER_SIZE) {
                throw new IllegalArgumentException("Failed to read seb file header.");
            }

            for (final Strategy s : Strategy.values()) {
                if (Arrays.equals(s.header, header)) {
                    strategy = s;
                    break;
                }
            }

            InputStream newIn;
            if (strategy == null) {
                strategy = Strategy.PLAIN_TEXT;
                newIn = new SequenceInputStream(
                        new ByteArrayInputStream(header),
                        input);
            } else {
                newIn = input;
            }

            if ((strategy == Strategy.PASSWORD_PSWD || strategy == Strategy.PASSWORD_PWCC)
                    && StringUtils.isBlank(context.getPassword())) {
                return new AsyncResult<>(new APIMessage.APIMessageException(
                        APIMessage.ErrorMessage.MISSING_PASSWORD.of("Missing Password")));
            }

            // then decrypt stream
            getEncryptor(strategy)
                    .getOrThrow()
                    .decrypt(pout, newIn, context);

            IOUtils.copyLarge(pin, output);

            return new AsyncResult<>(null);
        } catch (final IOException e) {
            log.error("Error while stream decrypted data: ", e);
            return new AsyncResult<>(e);
        } catch (final Exception iae) {
            return new AsyncResult<>(iae);
        } finally {
            try {
                if (pin != null) {
                    pin.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pout != null) {
                    pout.flush();
                    pout.close();
                }
                output.flush();
                output.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }
        }
    }

    private Result<SEBConfigCryptor> getEncryptor(final Strategy strategy) {
        final SEBConfigCryptor encryptor = this.encryptors.get(strategy);
        if (encryptor == null) {
            return Result.ofError(new IllegalArgumentException("No Encryptor found for strategy : " + strategy));
        }

        return Result.of(encryptor);
    }

    static class EncryptionContext implements SEBConfigEncryptionContext {

        public final Strategy strategy;
        public final CharSequence password;
        public final Function<CharSequence, Certificate> certificateStore;

        private EncryptionContext(
                final Strategy strategy,
                final CharSequence password,
                final Function<CharSequence, Certificate> certificateStore) {

            this.strategy = strategy;
            this.password = password;
            this.certificateStore = certificateStore;
        }

        @Override
        public Strategy getStrategy() {
            return this.strategy;
        }

        @Override
        public CharSequence getPassword() {
            return this.password;
        }

        @Override
        public Certificate getCertificate(final CharSequence key) {
            if (this.certificateStore == null) {
                throw new UnsupportedOperationException();
            }
            return this.certificateStore.apply(key);
        }

        static SEBConfigEncryptionContext contextOf(final Strategy strategy, final CharSequence password) {
            checkPasswordBased(strategy);
            return new EncryptionContext(strategy, password, null);
        }

        static SEBConfigEncryptionContext contextOf(
                final Strategy strategy,
                final Function<CharSequence, Certificate> certificateStore) {

            checkCertificateBased(strategy);
            return new EncryptionContext(strategy, null, certificateStore);
        }

        static void checkPasswordBased(final Strategy strategy) {
            if (strategy == null || strategy.type != Type.PASSWORD) {
                throw new IllegalArgumentException("Strategy mismatch for password based encryption: " + strategy);
            }
        }

        static void checkCertificateBased(final Strategy strategy) {
            if (strategy == null || strategy.type != Type.CERTIFICATE) {
                throw new IllegalArgumentException("Strategy mismatch for certificate based encryption: " + strategy);
            }
        }

        public static SEBConfigEncryptionContext contextOfPlainText() {
            return new EncryptionContext(Strategy.PLAIN_TEXT, null, null);
        }

        public static SEBConfigEncryptionContext contextOf(final CharSequence password) {
            return new EncryptionContext(null, password, null);
        }

    }

}
