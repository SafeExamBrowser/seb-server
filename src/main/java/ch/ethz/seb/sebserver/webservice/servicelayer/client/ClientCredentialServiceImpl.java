/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Lazy
@Service
@WebServiceProfile
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialServiceImpl.class);

    static final String SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY = "sebserver.webservice.internalSecret";

    private final Environment environment;

    protected ClientCredentialServiceImpl(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public Result<ClientCredentials> generatedClientCredentials() {
        return Result.tryCatch(() -> {
            try {

                return encryptClientCredentials(
                        generateClientId(),
                        generateClientSecret());

            } catch (final UnsupportedEncodingException e) {
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
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return new ClientCredentials(
                clientIdPlaintext,
                (StringUtils.isNoneBlank(secretPlaintext))
                        ? encrypt(secretPlaintext, secret).toString()
                        : null,
                (StringUtils.isNoneBlank(accessTokenPlaintext))
                        ? encrypt(accessTokenPlaintext, secret).toString()
                        : null);
    }

    @Override
    public CharSequence getPlainClientSecret(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasSecret()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return this.decrypt(credentials.secret, secret);
    }

    @Override
    public CharSequence getPlainAccessToken(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasAccessToken()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return this.decrypt(credentials.accessToken, secret);
    }

    @Override
    public CharSequence encrypt(final CharSequence text) {

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return encrypt(text, secret);
    }

    @Override
    public CharSequence decrypt(final CharSequence text) {

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return decrypt(text, secret);
    }

    CharSequence encrypt(final CharSequence text, final CharSequence secret) {
        if (text == null) {
            throw new IllegalArgumentException("Text has null reference");
        }

        if (secret == null) {
            log.warn("No internal secret supplied: skip encryption");
            return text;
        }

        try {

            final CharSequence salt = KeyGenerators.string().generateKey();
            final CharSequence cipher = Encryptors
                    .delux(secret, salt)
                    .encrypt(text.toString());

            return new StringBuilder(cipher)
                    .append(salt);

        } catch (final Exception e) {
            log.error("Failed to encrypt text: ", e);
            throw e;
        }
    }

    CharSequence decrypt(final CharSequence cipher, final CharSequence secret) {
        if (cipher == null) {
            throw new IllegalArgumentException("Cipher has null reference");
        }

        if (secret == null) {
            log.warn("No internal secret supplied: skip decryption");
            return cipher;
        }

        try {

            final int length = cipher.length();
            final int cipherTextLength = length - 16;
            final CharSequence salt = cipher.subSequence(cipherTextLength, length);
            final CharSequence cipherText = cipher.subSequence(0, cipherTextLength);

            return Encryptors
                    .delux(secret, salt)
                    .decrypt(cipherText.toString());

        } catch (final Exception e) {
            log.error("Failed to decrypt text: ", e);
            throw e;
        }
    }

    private final static char[] possibleCharacters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^*()-_=+[{]}?"
                    .toCharArray();

    public final static CharSequence generateClientId() {
        return RandomStringUtils.random(
                16, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public final static CharSequence generateClientSecret() throws UnsupportedEncodingException {
        // TODO fine a better way to generate a random char array instead of using RandomStringUtils.random which uses a String
        return RandomStringUtils.random(
                64, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public final static void clearChars(final CharSequence sequence) {
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
