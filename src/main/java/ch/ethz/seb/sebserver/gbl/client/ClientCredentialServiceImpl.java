/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import java.nio.CharBuffer;
import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Lazy
@Service
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialServiceImpl.class);

    private final Environment environment;
    private final Cryptor cryptor;

    protected ClientCredentialServiceImpl(
            final Environment environment,
            final Cryptor cryptor) {

        this.environment = environment;
        this.cryptor = cryptor;
    }

    @Override
    public Result<ClientCredentials> generatedClientCredentials() {
        return Result.tryCatch(() -> {
            try {

                return encryptClientCredentials(
                        generateClientId(),
                        generateClientSecret());

            } catch (final Exception e) {
                log.error("Error while trying to generate client credentials: ", e);
                throw new RuntimeException("cause: ", e);
            }
        });
    }

    @Override
    public ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext,
            final CharSequence accessTokenPlaintext) {

        final CharSequence secret = this.environment
                .getProperty(Cryptor.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return new ClientCredentials(
                clientIdPlaintext,
                (StringUtils.isNoneBlank(secretPlaintext))
                        ? Cryptor.encrypt(secretPlaintext, secret).toString()
                        : null,
                (StringUtils.isNoneBlank(accessTokenPlaintext))
                        ? Cryptor.encrypt(accessTokenPlaintext, secret).toString()
                        : null);
    }

    @Override
    public CharSequence getPlainClientSecret(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasSecret()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getProperty(Cryptor.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return Cryptor.decrypt(credentials.secret, secret);
    }

    @Override
    public CharSequence getPlainAccessToken(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasAccessToken()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getProperty(Cryptor.SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return Cryptor.decrypt(credentials.accessToken, secret);
    }

    @Override
    public CharSequence encrypt(final CharSequence text) {
        return this.cryptor.encrypt(text);
    }

    @Override
    public CharSequence decrypt(final CharSequence text) {
        return this.cryptor.decrypt(text);
    }

    private final static char[] possibleCharacters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^*()-_=+[{]}?"
                    .toCharArray();

    public static CharSequence generateClientId() {
        return RandomStringUtils.random(
                16, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public static CharSequence generateClientSecret() {
        // TODO find a better way to generate a random char array instead of using RandomStringUtils.random which uses a String
        return RandomStringUtils.random(
                64, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public static void clearChars(final CharSequence sequence) {
        if (sequence == null) {
            return;
        }

        if (sequence instanceof CharBuffer) {
            ((CharBuffer) sequence).clear();
            return;
        }

        throw new IllegalArgumentException(
                "Cannot clear chars on CharSequence of type: " + sequence.getClass().getName());
    }

}
