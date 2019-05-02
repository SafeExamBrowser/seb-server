/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService;

@Lazy
@Service
@WebServiceProfile
public final class SebConfigEncryptionServiceImpl implements SebConfigEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(SebConfigEncryptionServiceImpl.class);

    public static final int HEADER_SIZE = 4;

    private final Map<Strategy, SebConfigCryptor> encryptors;

    public SebConfigEncryptionServiceImpl(final Collection<SebConfigCryptor> encryptors) {
        this.encryptors = encryptors
                .stream()
                .flatMap(e -> e.strategies()
                        .stream()
                        .map(s -> new ImmutablePair<>(s, e)))
                .collect(Collectors.toMap(p -> p.left, p -> p.right));

    }

    @Override
    public Result<ByteBuffer> plainText(final CharSequence plainTextConfig) {

        if (log.isDebugEnabled()) {
            log.debug("No encryption, use plain text with header");
        }

        return Result.tryCatch(() -> {
            return addHeader(
                    Utils.toByteBuffer(plainTextConfig),
                    Strategy.PLAIN_TEXT);
        });
    }

    @Override
    public Result<ByteBuffer> encryptWithPassword(
            final CharSequence plainTextConfig,
            final Strategy strategy,
            final CharSequence password) {

        if (log.isDebugEnabled()) {
            log.debug("Password encryption with strategy: {}", strategy);
        }

        return getEncryptor(strategy)
                .flatMap(encryptor -> encryptor.encrypt(
                        plainTextConfig,
                        EncryptionContext.contextOf(strategy, password)))
                .map(bb -> addHeader(bb, strategy));
    }

    @Override
    public Result<ByteBuffer> encryptWithCertificate(
            final CharSequence plainTextConfig,
            final Strategy strategy,
            final Certificate certificate) {

        if (log.isDebugEnabled()) {
            log.debug("Certificate encryption with strategy: {}", strategy);
        }

        return getEncryptor(strategy)
                .flatMap(encryptor -> encryptor.encrypt(
                        plainTextConfig,
                        EncryptionContext.contextOf(strategy, certificate)))
                .map(bb -> addHeader(bb, strategy));
    }

    @Override
    public Result<ByteBuffer> decrypt(
            final ByteBuffer cipher,
            final Supplier<CharSequence> passwordSupplier,
            final Function<CharSequence, Certificate> certificateStore) {

        return verifyStrategy(cipher)
                .flatMap(strategy -> decrypt(strategy, cipher, passwordSupplier, certificateStore));
    }

    private Result<ByteBuffer> decrypt(
            final Strategy strategy,
            final ByteBuffer cipher,
            final Supplier<CharSequence> passwordSupplier,
            final Function<CharSequence, Certificate> certificateStore) {

        if (log.isDebugEnabled()) {
            log.debug("Decryption with strategy: {}", strategy);
        }

        if (strategy == Strategy.PLAIN_TEXT) {
            return Result.of(removeHeader(cipher, strategy));
        }

        return getEncryptor(strategy)
                .flatMap(encryptor -> encryptor.decrypt(
                        removeHeader(cipher, strategy),
                        (strategy.type == Type.PASSWORD)
                                ? EncryptionContext.contextOf(strategy, passwordSupplier.get())
                                : EncryptionContext.contextOf(strategy, certificateStore)));
    }

    private ByteBuffer addHeader(final ByteBuffer input, final Strategy strategy) {
        final ByteBuffer _input = (input == null) ? ByteBuffer.allocate(0) : input;

        _input.rewind();
        final ByteBuffer buffer = ByteBuffer.allocate(
                SebConfigEncryptionServiceImpl.HEADER_SIZE +
                        _input.limit());

        buffer.put(strategy.header);
        buffer.put(_input);
        return buffer.asReadOnlyBuffer();
    }

    private ByteBuffer removeHeader(final ByteBuffer input, final Strategy strategy) {
        input.rewind();
        final byte[] header = new byte[SebConfigEncryptionServiceImpl.HEADER_SIZE];
        input.get(header);

        if (Arrays.equals(strategy.header, header)) {
            final byte[] b = new byte[input.remaining()];
            input.get(b);
            return ByteBuffer.wrap(b).asReadOnlyBuffer();
        } else {
            input.clear();
            return input.asReadOnlyBuffer();
        }
    }

    private Result<Strategy> verifyStrategy(final ByteBuffer cipher) {
        cipher.rewind();
        final byte[] header = new byte[HEADER_SIZE];
        cipher.get(header);
        //final String headerString = Utils.toString(header);
        for (final Strategy s : Strategy.values()) {
            if (Arrays.equals(s.header, header)) {
                return Result.of(s);
            }
        }

        log.error("Failed to verify encryption strategy. Fallback to plain text strategy");
        return Result.of(Strategy.PLAIN_TEXT);
    }

    private Result<SebConfigCryptor> getEncryptor(final Strategy strategy) {
        final SebConfigCryptor encryptor = this.encryptors.get(strategy);
        if (encryptor == null) {
            return Result.ofError(new IllegalArgumentException("No Encryptor found for strategy : " + strategy));
        }

        return Result.of(encryptor);
    }

    protected static class EncryptionContext implements SebConfigEncryptionContext {

        public final Strategy strategy;
        public final CharSequence password;
        public final Certificate certificate;

        private EncryptionContext(
                final Strategy strategy,
                final CharSequence password,
                final Certificate certificate,
                final Function<CharSequence, Certificate> certificateStore) {

            this.strategy = strategy;
            this.password = password;
            this.certificate = certificate;
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
        public Certificate getCertificate() {
            return this.certificate;
        }

        static SebConfigEncryptionContext contextOf(final Strategy strategy, final CharSequence password) {
            checkPasswordbased(strategy);
            return new EncryptionContext(strategy, password, null, null);
        }

        static SebConfigEncryptionContext contextOf(final Strategy strategy, final Certificate certificate) {
            checkCertificateBased(strategy);
            return new EncryptionContext(strategy, null, certificate, null);
        }

        static SebConfigEncryptionContext contextOf(
                final Strategy strategy,
                final Function<CharSequence, Certificate> certificateStore) {

            checkCertificateBased(strategy);
            return new EncryptionContext(strategy, null, null, certificateStore);
        }

        static void checkPasswordbased(final Strategy strategy) {
            if (strategy == null || strategy.type != Type.PASSWORD) {
                throw new IllegalArgumentException("Strategy missmatch for password based encryption: " + strategy);
            }
        }

        static void checkCertificateBased(final Strategy strategy) {
            if (strategy == null || strategy.type != Type.CERTIFICATE) {
                throw new IllegalArgumentException("Strategy missmatch for certificate based encryption: " + strategy);
            }
        }

    }

}
